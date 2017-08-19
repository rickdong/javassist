/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javassist.bytecode.AttributeChangeListener;
import javassist.bytecode.AttributeObservable;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.util.CollectionUtils;

/**
 * An instance of <code>CtMember</code> represents a field, a constructor,
 * or a method.
 */
public abstract class CtMember {
    protected CtClass declaringClass;

    /* Make a circular link of CtMembers declared in the
     * same class so that they are garbage-collected together
     * at the same time.
     */
    static class Cache extends CtMember implements AttributeChangeListener {
        protected void extendToString(StringBuilder buffer) {}
        public boolean hasAnnotation(String clz) { return false; }
        public Object getAnnotation(Class clz)
            throws ClassNotFoundException { return null; }
        public Object[] getAnnotations()
            throws ClassNotFoundException { return null; }
        public byte[] getAttribute(String name) { return null; }
        public Object[] getAvailableAnnotations() { return null; }
        public int getModifiers() { return 0; }
        public String getName() { return null; }
        public String getSignature() { return null; }
        public void setAttribute(String name, byte[] data) {}
        public void setModifiers(int mod) {}
        public String getGenericSignature() { return null; }
        public void setGenericSignature(String sig) {}

        private final List<CtConstructor> cons = new ArrayList<CtConstructor>();
        private final List<CtConstructor> staticInits = new ArrayList<CtConstructor>();
        private final Map<String, List<CtMethod>> methods = new HashMap<String, List<CtMethod>>();
        private final List<CtMethod> methods2 = new ArrayList<CtMethod>();
        private final Map<String, CtField> fields = new HashMap<String, CtField>();
        private final Map<MethodInfo, CtBehavior> methodInfoToBehavior = new IdentityHashMap<MethodInfo, CtBehavior>(1000);

        Cache(CtClassType decl) {
            super(decl);
        }

        @Override
        public void onChange(Object src, String name, Object oldValue, Object newValue) {
            if (AttributeObservable.NAME.equals(name)) {
                if (src instanceof FieldInfo) {
                    CtField field = fields.remove(oldValue);
                    fields.put((String) newValue, field);
                } else if (src instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) src;
                    List<CtMethod> mths = methods.remove(oldValue + " " + mi.getDescriptor());
                    if (mths != null) {
                        Iterator<CtMethod> it = mths.iterator();
                        while (it.hasNext()) {
                            CtMethod mth = it.next();
                            if (mth.getMethodInfo2().equals(src)) {
                                it.remove();
                                CollectionUtils.addToKeyedList(newValue + " " + mi.getDescriptor(), mth, methods);
                                break;
                            }
                        }
                    }
                    if (MethodInfo.nameClinit.equals(oldValue)) {
                        CtBehavior cb = methodInfoToBehavior.get(mi);
                        if (cb instanceof CtConstructor) {
                            staticInits.remove(cb);
                        }
                    }
                    else if (MethodInfo.nameClinit.equals(newValue)) {
                        CtBehavior cb = methodInfoToBehavior.get(mi);
                        if (cb instanceof CtConstructor) {
                            staticInits.add((CtConstructor) cb);
                        }
                    }
                }
            } else if (AttributeObservable.DESCRIPTOR.equals(name)) {
                if (src instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) src;
                    List<CtMethod> mths = methods.remove(mi.getName() + " " + oldValue);
                    if (mths != null) {
                        Iterator<CtMethod> it = mths.iterator();
                        while (it.hasNext()) {
                            CtMethod mth = it.next();
                            if (mth.getMethodInfo2().equals(src)) {
                                it.remove();
                                CollectionUtils.addToKeyedList(mi.getName() + " " + newValue, mth, methods);
                                break;
                            }
                        }
                    }
                }
            }
        }

        
        void addMethod(CtMethod method) {
            method.getMethodInfo2().addChangeListener(this);
            CollectionUtils.addToKeyedList(method.getName() + " " + method.getSignature(), method, methods);
            methodInfoToBehavior.put(method.getMethodInfo2(), method);
            methods2.add(method);
        }

        CtMethod[] getMethods() {
            return methods2.toArray(new CtMethod[0]);
        }
        
        CtMethod getMethod(String name, String desc) {
            return getMethodByNameAndDesc(name + " " + desc);
        }

        CtMethod getMethodByNameAndDesc(String nameAndDesc) {
            List<CtMethod> mths = methods.get(nameAndDesc);
            return mths == null || mths.isEmpty() ? null : mths.get(0);
        }
        
        /* Both constructors and a class initializer.
         */
        void addConstructor(CtConstructor cons) {
            this.cons.add(cons);
            methodInfoToBehavior.put(cons.getMethodInfo2(), cons);
            if (cons.isClassInitializer()) {
                staticInits.add(cons);
            }
        }
        
        CtConstructor[] getConstructors(){
            return cons.toArray(new CtConstructor[0]);
        }
        
        CtConstructor[] getPublicConstructors(){
            List<CtConstructor> ret = new ArrayList<CtConstructor>();
            for(CtConstructor c : cons){
                if(isPubCons(c)){
                    ret.add(c);
                }
            }
            return ret.toArray(new CtConstructor[0]);
        }
        
        CtConstructor[] getDeclaredConstructors() {
            List<CtConstructor> ret = new ArrayList<CtConstructor>();
            for(CtConstructor c : cons){
                if(c.isConstructor()){
                    ret.add(c);
                }
            }
            return ret.toArray(new CtConstructor[0]);
        }
        
        CtConstructor getConstructors(String desc) {
            for(CtConstructor c : cons){
                if (c.getMethodInfo2().getDescriptor().equals(desc) && c.isConstructor()) {
                    return c;
                }
            }
            return null;
        }
        
        CtConstructor getClassInitializer() {
            if (staticInits.isEmpty()) {
                return null;
            }
            return staticInits.get(0);
        }

        private static boolean isPubCons(CtConstructor cons) {
            return !Modifier.isPrivate(cons.getModifiers())
                    && cons.isConstructor();
        }

        void addField(CtField field) {
            fields.put(field.getName(), field);
            field.getFieldInfo2().addChangeListener(this);
        }
        
        CtField[] getFields(){
            return fields.values().toArray(new CtField[0]);
        }
        
        CtField getFieldByName(String name){
            return fields.get(name);
        }
        
        CtField getFieldByNameAndDescc(String name, String desc) {
            CtField field = getFieldByName(name);
            if (field != null && desc.equals(field.getSignature())) {
                return field;
            }
            for (CtField f : fields.values()) {
                if (f.getName().equals(name) && (desc == null || desc.equals(f.getSignature()))) {
                    return f;
                }
            }
            return null;
        }

        void remove(CtMember mem) {
            if(mem instanceof CtMethod){
                CtMethod mth = (CtMethod) mem;
                Iterator<Entry<String, List<CtMethod>>> it = methods.entrySet().iterator();
                while(it.hasNext()){
                    List<CtMethod> val  = it.next().getValue();
                    if(val.remove(mth)) {
                        mth.getMethodInfo2().removeChangeListener(this);
                        methodInfoToBehavior.remove(mth.getMethodInfo2());
                        methods2.remove(mth);
                        if(val.isEmpty()){
                            it.remove();
                        }
                        break;
                    }
                }
            }
            else if(mem instanceof CtField){
                CtField f = fields.remove(mem.getName());
                if (f != null) {
                    f.getFieldInfo2().removeChangeListener(this);
                }
            }
            else if(mem instanceof CtConstructor){
                CtConstructor con = (CtConstructor) mem;
                methodInfoToBehavior.remove(con.getMethodInfo2());
                cons.remove(con);
            }
        }
        
        CtBehavior where(MethodInfo mi) {
            CtBehavior b = methodInfoToBehavior.get(mi);
            if (b != null) {
                return b;
            }
            /*
             * getDeclaredBehaviors() returns a list of methods/constructors. Although the list is
             * cached in a CtClass object, it might be recreated for some reason. Thus, the member
             * name and the signature must be also checked.
             */
            for (MethodInfo i : methodInfoToBehavior.keySet()) {
                if (mi.getName().equals(i.getName()) && mi.getDescriptor().equals(i.getDescriptor())) {
                    return methodInfoToBehavior.get(i);
                }
            }
            return null;
        }
    }
    
    protected CtMember(CtClass clazz) {
        declaringClass = clazz;
    }

    /**
     * This method is invoked when setName() or replaceClassName()
     * in CtClass is called.
     *
     * @see CtMethod#nameReplaced()
     */
    void nameReplaced() {}

    public String toString() {
        StringBuilder buffer = new StringBuilder(getClass().getName());
        buffer.append("@");
        buffer.append(Integer.toHexString(hashCode()));
        buffer.append("[");
        buffer.append(Modifier.toString(getModifiers()));
        extendToString(buffer);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Invoked by {@link #toString()} to add to the buffer and provide the
     * complete value.  Subclasses should invoke this method, adding a
     * space before each token.  The modifiers for the member are
     * provided first; subclasses should provide additional data such
     * as return type, field or method name, etc.
     */
    protected abstract void extendToString(StringBuilder buffer);

    /**
     * Returns the class that declares this member.
     */
    public CtClass getDeclaringClass() { return declaringClass; }

    /**
     * Returns true if this member is accessible from the given class.
     */
    public boolean visibleFrom(CtClass clazz) {
        int mod = getModifiers();
        if (Modifier.isPublic(mod))
            return true;
        else if (Modifier.isPrivate(mod))
            return clazz == declaringClass;
        else {  // package or protected
            String declName = declaringClass.getPackageName();
            String fromName = clazz.getPackageName();
            boolean visible;
            if (declName == null)
                visible = fromName == null;
            else
                visible = declName.equals(fromName);

            if (!visible && Modifier.isProtected(mod))
                return clazz.subclassOf(declaringClass);

            return visible;
        }
    }

    /**
     * Obtains the modifiers of the member.
     *
     * @return          modifiers encoded with
     *                  <code>javassist.Modifier</code>.
     * @see Modifier
     */
    public abstract int getModifiers();

    /**
     * Sets the encoded modifiers of the member.
     *
     * @see Modifier
     */
    public abstract void setModifiers(int mod);

    /**
     * Returns true if the class has the specified annotation type.
     *
     * @param clz the annotation type.
     * @return <code>true</code> if the annotation is found, otherwise <code>false</code>.
     * @since 3.11
     */
    public boolean hasAnnotation(Class clz) {
        return hasAnnotation(clz.getName());
    }

    /**
     * Returns true if the class has the specified annotation type.
     *
     * @param annotationTypeName the name of annotation type.
     * @return <code>true</code> if the annotation is found, otherwise <code>false</code>.
     * @since 3.21
     */
    public abstract boolean hasAnnotation(String annotationTypeName);

    /**
     * Returns the annotation if the class has the specified annotation type.
     * For example, if an annotation <code>@Author</code> is associated
     * with this member, an <code>Author</code> object is returned.
     * The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @param annotationType    the annotation type.
     * @return the annotation if found, otherwise <code>null</code>.
     * @since 3.11
     */
    public abstract Object getAnnotation(Class annotationType) throws ClassNotFoundException;

    /**
     * Returns the annotations associated with this member.
     * For example, if an annotation <code>@Author</code> is associated
     * with this member, the returned array contains an <code>Author</code>
     * object.  The member values can be obtained by calling methods on
     * the <code>Author</code> object.
     *
     * @return an array of annotation-type objects.
     * @see CtClass#getAnnotations()
     */
    public abstract Object[] getAnnotations() throws ClassNotFoundException;

    /**
     * Returns the annotations associated with this member.
     * This method is equivalent to <code>getAnnotations()</code>
     * except that, if any annotations are not on the classpath,
     * they are not included in the returned array.
     *
     * @return an array of annotation-type objects.
     * @see #getAnnotations()
     * @see CtClass#getAvailableAnnotations()
     * @since 3.3
     */
    public abstract Object[] getAvailableAnnotations();

    /**
     * Obtains the name of the member.
     *
     * <p>As for constructor names, see <code>getName()</code>
     * in <code>CtConstructor</code>.
     *
     * @see CtConstructor#getName()
     */
    public abstract String getName();

    /**
     * Returns the character string representing the signature of the member.
     * If two members have the same signature (parameter types etc.),
     * <code>getSignature()</code> returns the same string.
     */
    public abstract String getSignature();

    /**
     * Returns the generic signature of the member.
     *
     * @see javassist.bytecode.SignatureAttribute#toFieldSignature(String)
     * @see javassist.bytecode.SignatureAttribute#toMethodSignature(String)
     * @see CtClass#getGenericSignature()
     * @since 3.17
     */
    public abstract String getGenericSignature();

    /**
     * Sets the generic signature of the member.
     *
     * @param sig   a new generic signature.
     * @see javassist.bytecode.SignatureAttribute.ObjectType#encode()
     * @see javassist.bytecode.SignatureAttribute.MethodSignature#encode()
     * @see CtClass#setGenericSignature(String)
     * @since 3.17
     */
    public abstract void setGenericSignature(String sig);

    /**
     * Obtains a user-defined attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name              attribute name
     */
    public abstract byte[] getAttribute(String name);

    /**
     * Adds a user-defined attribute. The attribute is saved in the class file.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public abstract void setAttribute(String name, byte[] data);
}
