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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.util.CollectionUtils;

/**
 * <code>ClassFile</code> represents a Java <code>.class</code> file, which
 * consists of a constant pool, methods, fields, and attributes.
 *
 * <p>For example,</p>
 * <blockquote><pre>
 * ClassFile cf = new ClassFile(false, "test.Foo", null);
 * cf.setInterfaces(new String[] { "java.lang.Cloneable" });
 *
 * FieldInfo f = new FieldInfo(cf.getConstPool(), "width", "I");
 * f.setAccessFlags(AccessFlag.PUBLIC);
 * cf.addField(f);
 *
 * cf.write(new DataOutputStream(new FileOutputStream("Foo.class")));
 * </pre></blockquote>
 * <p>This code generates a class file <code>Foo.class</code> for the following class:</p>
 * <blockquote><pre>
 * package test;
 * class Foo implements Cloneable {
 *     public int width;
 * }
 * </pre></blockquote>
 *
 * @see FieldInfo
 * @see MethodInfo
 * @see ClassFileWriter
 * @see javassist.CtClass#getClassFile()
 * @see javassist.ClassPool#makeClass(ClassFile)
 */
public final class ClassFile implements AttributeChangeListener {
    int major, minor; // version number
    ConstPool constPool;
    int thisClass;
    int accessFlags;
    int superClass;
    int[] interfaces;
    Map<String, FieldInfo> fields; // field name to fieldInfo
    List<FieldInfo> fields2;
    Map<String, List<MethodInfo>> methods; // method name -> set of methodInfo
    List<MethodInfo> methods2;
    Map<String, AttributeInfo> attributes;
    String thisclassname; // not JVM-internal name
    String[] cachedInterfaces;
    Set<String> cacheInterfaces2 = new HashSet<String>();
    String cachedSuperclass;

    /**
     * The major version number of class files
     * for JDK 1.1.
     */
    public static final int JAVA_1 = 45;

    /**
     * The major version number of class files
     * for JDK 1.2.
     */
    public static final int JAVA_2 = 46;

    /**
     * The major version number of class files
     * for JDK 1.3.
     */
    public static final int JAVA_3 = 47;

    /**
     * The major version number of class files
     * for JDK 1.4.
     */
    public static final int JAVA_4 = 48;

    /**
     * The major version number of class files
     * for JDK 1.5.
     */
    public static final int JAVA_5 = 49;

    /**
     * The major version number of class files
     * for JDK 1.6.
     */
    public static final int JAVA_6 = 50;

    /**
     * The major version number of class files
     * for JDK 1.7.
     */
    public static final int JAVA_7 = 51;

    /**
     * The major version number of class files
     * for JDK 1.8.
     */
    public static final int JAVA_8 = 52;

    /**
     * The major version number of class files
     * for JDK 1.9.
     */
    public static final int JAVA_9 = 53;

    /**
     * The major version number of class files created
     * from scratch.  The default value is 47 (JDK 1.3).
     * It is 49 (JDK 1.5)
     * if the JVM supports <code>java.lang.StringBuilder</code>.
     * It is 50 (JDK 1.6)
     * if the JVM supports <code>java.util.zip.DeflaterInputStream</code>.
     * It is 51 (JDK 1.7)
     * if the JVM supports <code>java.lang.invoke.CallSite</code>.
     * It is 52 (JDK 1.8)
     * if the JVM supports <code>java.util.function.Function</code>.
     * It is 53 (JDK 1.9)
     * if the JVM supports <code>java.lang.reflect.Module</code>.
     */
    public static final int MAJOR_VERSION;

    static {
        int ver = JAVA_3;
        try {
            Class.forName("java.lang.StringBuilder");
            ver = JAVA_5;
            Class.forName("java.util.zip.DeflaterInputStream");
            ver = JAVA_6;
            Class.forName("java.lang.invoke.CallSite", false, ClassLoader.getSystemClassLoader());
            ver = JAVA_7;
            Class.forName("java.util.function.Function");
            ver = JAVA_8;
            Class.forName("java.lang.Module");
            ver = JAVA_9;
        }
        catch (Throwable t) {}
        MAJOR_VERSION = ver;
    }

    /**
     * Constructs a class file from a byte stream.
     */
    public ClassFile(DataInputStream in) throws IOException {
        read(in);
    }

    /**
     * Constructs a class file including no members.
     * 
     * @param isInterface
     *            true if this is an interface. false if this is a class.
     * @param classname
     *            a fully-qualified class name
     * @param superclass
     *            a fully-qualified super class name or null.
     */
    public ClassFile(boolean isInterface, String classname, String superclass) {
        major = MAJOR_VERSION;
        minor = 0; // JDK 1.3 or later
        constPool = new ConstPool(classname);
        thisClass = constPool.getThisClassInfo();
        if (isInterface)
            accessFlags = AccessFlag.INTERFACE | AccessFlag.ABSTRACT;
        else
            accessFlags = AccessFlag.SUPER;

        initSuperclass(superclass);
        interfaces = null;
        fields = new LinkedHashMap<String, FieldInfo>();
        fields2 = new ArrayList<FieldInfo>();
        methods = new LinkedHashMap<String, List<MethodInfo>>();
        methods2 = new ArrayList<MethodInfo>();
        thisclassname = classname;

        attributes = new LinkedHashMap<String, AttributeInfo>();
        SourceFileAttribute sa = new SourceFileAttribute(constPool,
                getSourcefileName(thisclassname));
        attributes.put(sa.getName(), sa);
    }

    private void initSuperclass(String superclass) {
        if (superclass != null) {
            this.superClass = constPool.addClassInfo(superclass);
            cachedSuperclass = superclass;
        }
        else {
            this.superClass = constPool.addClassInfo("java.lang.Object");
            cachedSuperclass = "java.lang.Object";
        }
    }

    private static String getSourcefileName(String qname) {
        int index = qname.lastIndexOf('.');
        if (index >= 0)
            qname = qname.substring(index + 1);

        return qname + ".java";
    }

    /**
     * Eliminates dead constant pool items. If a method or a field is removed,
     * the constant pool items used by that method/field become dead items. This
     * method recreates a constant pool.
     */
    public void compact() {
        final ConstPool cp = compact0();
        loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                info.compact(cp);
            }
        });
        
        loopFields(new FieldInfoCallback(){
            @Override
            public void onInfo(FieldInfo info) {
                info.compact(cp);
            }
        });

        attributes = AttributeInfo.copyAll(attributes, cp);
        constPool = cp;
    }

    private ConstPool compact0() {
        ConstPool cp = new ConstPool(thisclassname);
        thisClass = cp.getThisClassInfo();
        String sc = getSuperclass();
        if (sc != null)
            superClass = cp.addClassInfo(getSuperclass());

        if (interfaces != null) {
            int n = interfaces.length;
            for (int i = 0; i < n; ++i)
                interfaces[i]
                    = cp.addClassInfo(constPool.getClassInfo(interfaces[i]));
        }

        return cp;
    }

    /**
     * Discards all attributes, associated with both the class file and the
     * members such as a code attribute and exceptions attribute. The unused
     * constant pool entries are also discarded (a new packed constant pool is
     * constructed).
     */
    public void prune() {
        final ConstPool cp = compact0();
        Map<String, AttributeInfo> newAttributes = new LinkedHashMap<String, AttributeInfo>();
        AttributeInfo invisibleAnnotations
            = getAttribute(AnnotationsAttribute.invisibleTag);
        if (invisibleAnnotations != null) {
            invisibleAnnotations = invisibleAnnotations.copy(cp, null);
            newAttributes.put(invisibleAnnotations.getName(), invisibleAnnotations);
        }

        AttributeInfo visibleAnnotations
            = getAttribute(AnnotationsAttribute.visibleTag);
        if (visibleAnnotations != null) {
            visibleAnnotations = visibleAnnotations.copy(cp, null);
            newAttributes.put(visibleAnnotations.getName(), visibleAnnotations);
        }

        AttributeInfo signature 
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.put(signature.getName(), signature);
        }
        
        loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                info.prune(cp);
            }
        });
        
        loopFields(new FieldInfoCallback(){
            @Override
            public void onInfo(FieldInfo info) {
                info.prune(cp);
            }
        });

        attributes = newAttributes;
        constPool = cp;
    }
    
    public static interface MethodInfoCallback {

        void onInfo(MethodInfo info);

    }

    public static interface FieldInfoCallback {
        void onInfo(FieldInfo info);
    }
    
    public void loopMethods(MethodInfoCallback cb) {
        List<MethodInfo> list = methods2;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            MethodInfo info = list.get(i);
            cb.onInfo(info);
        }
    }

    public void loopFields(FieldInfoCallback cb) {
        List<FieldInfo> list = fields2;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            FieldInfo info = list.get(i);
            cb.onInfo(info);
        }
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Returns true if this is an interface.
     */
    public boolean isInterface() {
        return (accessFlags & AccessFlag.INTERFACE) != 0;
    }

    /**
     * Returns true if this is a final class or interface.
     */
    public boolean isFinal() {
        return (accessFlags & AccessFlag.FINAL) != 0;
    }

    /**
     * Returns true if this is an abstract class or an interface.
     */
    public boolean isAbstract() {
        return (accessFlags & AccessFlag.ABSTRACT) != 0;
    }

    /**
     * Returns access flags.
     * 
     * @see javassist.bytecode.AccessFlag
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Changes access flags.
     * 
     * @see javassist.bytecode.AccessFlag
     */
    public void setAccessFlags(int acc) {
        if ((acc & AccessFlag.INTERFACE) == 0)
            acc |= AccessFlag.SUPER;

        accessFlags = acc;
    }

    /**
     * Returns access and property flags of this nested class.
     * This method returns -1 if the class is not a nested class. 
     *
     * <p>The returned value is obtained from <code>inner_class_access_flags</code>
     * of the entry representing this nested class itself
     * in <code>InnerClasses_attribute</code>. 
     */
    public int getInnerAccessFlags() {
        InnerClassesAttribute ica
            = (InnerClassesAttribute)getAttribute(InnerClassesAttribute.tag);
        if (ica == null)
            return -1;

        String name = getName();
        int n = ica.tableLength();
        for (int i = 0; i < n; ++i)
            if (name.equals(ica.innerClass(i)))
                return ica.accessFlags(i);

        return -1;
    }

    /**
     * Returns the class name.
     */
    public String getName() {
        return thisclassname;
    }

    /**
     * Sets the class name. This method substitutes the new name for all
     * occurrences of the old class name in the class file.
     */
    public void setName(String name) {
        renameClass(thisclassname, name);
    }

    /**
     * Returns the super class name.
     */
    public String getSuperclass() {
        if (cachedSuperclass == null)
            cachedSuperclass = constPool.getClassInfo(superClass);

        return cachedSuperclass;
    }

    /**
     * Returns the index of the constant pool entry representing the super
     * class.
     */
    public int getSuperclassId() {
        return superClass;
    }

    /**
     * Sets the super class.
     * 
     * <p>
     * The new super class should inherit from the old super class.
     * This method modifies constructors so that they call constructors declared
     * in the new super class.
     */
    public void setSuperclass(String superclass) throws CannotCompileException {
        if (superclass == null)
            superclass = "java.lang.Object";

        this.superClass = constPool.addClassInfo(superclass);
        final String finalSuperClass = superclass;
        try {
            loopMethods(new MethodInfoCallback() {
                @Override
                public void onInfo(MethodInfo info) {
                    try {
                        info.setSuperclass(finalSuperClass);
                    } catch (BadBytecode e) {
                        throw new RuntimeException(new CannotCompileException(e));
                    }
                }
            });
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CannotCompileException) {
                throw (CannotCompileException) cause;
            }
            throw ex;
        }
        cachedSuperclass = superclass;
    }

    /**
     * Replaces all occurrences of a class name in the class file.
     * 
     * <p>
     * If class X is substituted for class Y in the class file, X and Y must
     * have the same signature. If Y provides a method m(), X must provide it
     * even if X inherits m() from the super class. If this fact is not
     * guaranteed, the bytecode verifier may cause an error.
     * 
     * @param oldname
     *            the replaced class name
     * @param newname
     *            the substituted class name
     */
    public final void renameClass(String oldname, String newname) {
        if (oldname.equals(newname))
            return;

        if (oldname.equals(thisclassname))
            thisclassname = newname;

        oldname = Descriptor.toJvmName(oldname);
        newname = Descriptor.toJvmName(newname);
        constPool.renameClass(oldname, newname);

        AttributeInfo.renameClass(attributes, oldname, newname);
        
        final String fOldName = oldname;
        final String fNewName = newname;
        loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                String desc = info.getDescriptor();
                info.setDescriptor(Descriptor.rename(desc, fOldName, fNewName));
                AttributeInfo.renameClass(info.getAttributes(), fOldName, fNewName);
            }
        });
        
        loopFields(new FieldInfoCallback() {
            @Override
            public void onInfo(FieldInfo info) {
                String desc = info.getDescriptor();
                info.setDescriptor(Descriptor.rename(desc, fOldName, fNewName));
                AttributeInfo.renameClass(info.getAttributes(), fOldName, fNewName);
            }
        });
    }

    /**
     * Replaces all occurrences of several class names in the class file.
     * 
     * @param classnames
     *            specifies which class name is replaced with which new name.
     *            Class names must be described with the JVM-internal
     *            representation like <code>java/lang/Object</code>.
     * @see #renameClass(String,String)
     */
    public final void renameClass(final Map classnames) {
        String jvmNewThisName = (String)classnames.get(Descriptor
                .toJvmName(thisclassname));
        if (jvmNewThisName != null)
            thisclassname = Descriptor.toJavaName(jvmNewThisName);

        constPool.renameClass(classnames);

        AttributeInfo.renameClass(attributes, classnames);
        
        loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                String desc = info.getDescriptor();
                info.setDescriptor(Descriptor.rename(desc, classnames));
                AttributeInfo.renameClass(info.getAttributes(), classnames);
            }
        });
        
        loopFields(new FieldInfoCallback() {
            @Override
            public void onInfo(FieldInfo info) {
                String desc = info.getDescriptor();
                info.setDescriptor(Descriptor.rename(desc, classnames));
                AttributeInfo.renameClass(info.getAttributes(), classnames);
            }
        });
    }

    /**
     * Internal-use only.
     * <code>CtClass.getRefClasses()</code> calls this method. 
     */
    public final void getRefClasses(final Map classnames) {
        constPool.renameClass(classnames);

        AttributeInfo.getRefClasses(attributes, classnames);
        
        loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                String desc = info.getDescriptor();
                Descriptor.rename(desc, classnames);
                AttributeInfo.getRefClasses(info.getAttributes(), classnames);
            }
        });
        
        loopFields(new FieldInfoCallback() {
            @Override
            public void onInfo(FieldInfo info) {
                String desc = info.getDescriptor();
                Descriptor.rename(desc, classnames);
                AttributeInfo.getRefClasses(info.getAttributes(), classnames);
            }
        });
        
    }

    /**
     * Returns the names of the interfaces implemented by the class.
     * The returned array is read only.
     */
    public String[] getInterfaces() {
        if (cachedInterfaces != null)
            return cachedInterfaces;

        String[] rtn = null;
        if (interfaces == null)
            rtn = new String[0];
        else {
            int n = interfaces.length;
            String[] list = new String[n];
            for (int i = 0; i < n; ++i) {
                list[i] = constPool.getClassInfo(interfaces[i]);
                cacheInterfaces2.add(list[i]);
            }

            rtn = list;
        }

        cachedInterfaces = rtn;
        return rtn;
    }
    
    public boolean containsInterface(String name) {
        if (cacheInterfaces2 == null) {
            getInterfaces();
        }
        return cacheInterfaces2.contains(name);
    }
    

    /**
     * Sets the interfaces.
     * 
     * @param nameList
     *            the names of the interfaces.
     */
    public void setInterfaces(String[] nameList) {
        cachedInterfaces = null;
        cacheInterfaces2.clear();
        if (nameList != null) {
            int n = nameList.length;
            interfaces = new int[n];
            for (int i = 0; i < n; ++i)
                interfaces[i] = constPool.addClassInfo(nameList[i]);
        }
    }

    /**
     * Appends an interface to the interfaces implemented by the class.
     */
    public void addInterface(String name) {
        cachedInterfaces = null;
        cacheInterfaces2.clear();
        int info = constPool.addClassInfo(name);
        if (interfaces == null) {
            interfaces = new int[1];
            interfaces[0] = info;
        }
        else {
            int n = interfaces.length;
            int[] newarray = new int[n + 1];
            System.arraycopy(interfaces, 0, newarray, 0, n);
            newarray[n] = info;
            interfaces = newarray;
        }
    }

    /**
     * Appends a field to the class.
     *
     * @throws DuplicateMemberException         when the field is already included.
     */
    public void addField(FieldInfo finfo) throws DuplicateMemberException {
        testExistingField(finfo.getName(), finfo.getDescriptor());
        addField2(finfo);
    }
    
    /**
     * Just appends a field to the class.
     * It does not check field duplication.
     * Use this method only when minimizing performance overheads
     * is seriously required.
     *
     * @since 3.13
     */
    public final void addField2(FieldInfo finfo) {
        FieldInfo prev = (FieldInfo) fields.put(finfo.getName(), finfo);
        if (prev != null) {
            prev.removeChangeListener(this);
            fields2.remove(prev);
        }
        fields2.add(finfo);
        finfo.addChangeListener(this);
    }
    
    @Override
    public void onChange(Object src, String name, Object oldValue, Object newValue) {
        if (AttributeObservable.NAME.equals(name)) {
            if (src instanceof FieldInfo) {
                fields.remove(oldValue);
                fields.put((String)newValue, (FieldInfo) src);
            }
            else if(src instanceof MethodInfo){
                List<MethodInfo> ms = methods.get(oldValue);
                List<MethodInfo> ms1 = methods.get(newValue);
                if (ms1 == null) {
                    ms1 = new ArrayList<MethodInfo>();
                    methods.put((String) newValue, ms1);
                }
                ms.remove(src);
                
                Iterator<MethodInfo> it = ms.iterator();
                while (it.hasNext()) {
                    MethodInfo mi = it.next();
                    if (mi.getName().equals(newValue)) {
                        it.remove();
                        ms1.add(mi);
                    }
                }
                it = ms1.iterator();
                while (it.hasNext()) {
                    MethodInfo mi = it.next();
                    if (mi.getName().equals(oldValue)) {
                        it.remove();
                        ms.add(mi);
                    }
                }
                ms1.add((MethodInfo) src);
                if (ms.isEmpty()) {
                    methods.remove(oldValue);
                }
            }
        }
    }

    private void testExistingField(String name, String descriptor)
            throws DuplicateMemberException {
        if(fields.containsKey(name)){
            throw new DuplicateMemberException("duplicate field: " + name);
        }
    }

    public boolean removeMethod(MethodInfo mi){
        List<MethodInfo> ms = methods.get(mi.getName());
        if(ms == null || ms.isEmpty()){
            return false;
        }
        if(ms.remove(mi)){
            methods2.remove(mi);
            mi.removeChangeListener(this);
            if(ms.isEmpty()){
                methods.remove(mi.getName());
            }
            return true;
        }
        return false;
    }
    
    public boolean removeField(FieldInfo fi){
         if(fields.remove(fi.getName()) != null){
             fields2.remove(fi);
             return true;
         }
         return false;
    }
    
    public FieldInfo getField(String name){
        return fields.get(name);
    }
    
    public List<MethodInfo> getMethods(String name){
        List<MethodInfo> ret = methods.get(name);
        return ret == null ? Collections.<MethodInfo>emptyList() : Collections.unmodifiableList(ret);
    }
    

    /**
     * Returns the method with the specified name. If there are multiple methods
     * with that name, this method returns one of them.
     * 
     * @return null if no such method is found.
     */
    public MethodInfo getMethod(String name) {
        List<MethodInfo> ms = getMethods(name);
        return ms.isEmpty() ? null : ms.get(0);
    }

    /**
     * Returns a static initializer (class initializer), or null if it does not
     * exist.
     */
    public MethodInfo getStaticInitializer() {
        return getMethod(MethodInfo.nameClinit);
    }

    /**
     * Appends a method to the class.
     * If there is a bridge method with the same name and signature,
     * then the bridge method is removed before a new method is added.
     *
     * @throws DuplicateMemberException         when the method is already included.
     */
    public void addMethod(MethodInfo minfo) throws DuplicateMemberException {
        testExistingMethod(minfo);
        addMethod2(minfo);
    }

    /**
     * Just appends a method to the class.
     * It does not check method duplication or remove a bridge method.
     * Use this method only when minimizing performance overheads
     * is seriously required.
     *
     * @since 3.13
     */
    public final void addMethod2(MethodInfo minfo) {
        CollectionUtils.addToKeyedList(minfo.getName(), minfo, methods);
        methods2.add(minfo);
        minfo.addChangeListener(this);
    }
    
    private void testExistingMethod(MethodInfo newMinfo)
        throws DuplicateMemberException
    {
        String name = newMinfo.getName();
        String descriptor = newMinfo.getDescriptor();
        List<MethodInfo> ms = methods.get(name);
        if (ms == null || ms.isEmpty()) {
            return;
        }
        for(MethodInfo mi : ms.toArray(new MethodInfo[0])){
            if (isDuplicated(newMinfo, name, descriptor, mi)){
                throw new DuplicateMemberException("duplicate method: " + name
                        + " in " + this.getName());
            }
        }
    }

    private boolean isDuplicated(MethodInfo newMethod, String newName,
                                        String newDesc, MethodInfo minfo)
    {
        if (!minfo.getName().equals(newName))
            return false;

        String desc = minfo.getDescriptor();
        if (!Descriptor.eqParamTypes(desc, newDesc))
           return false;

        if (desc.equals(newDesc)) {
            if (notBridgeMethod(minfo))
                return true;
            else {
            	// if the bridge method with the same signature
            	// already exists, replace it.
                removeMethod(minfo);
                return false;
            }
        }
        else
        	return false;
           // return notBridgeMethod(minfo) && notBridgeMethod(newMethod);
    }

    /* For a bridge method, see Sec. 15.12.4.5 of JLS 3rd Ed.
     */
    private static boolean notBridgeMethod(MethodInfo minfo) {
        return (minfo.getAccessFlags() & AccessFlag.BRIDGE) == 0;
    }

    /**
     * Returns all the attributes.  The returned <code>List</code> object
     * is shared with this object.  If you add a new attribute to the list,
     * the attribute is also added to the classs file represented by this
     * object.  If you remove an attribute from the list, it is also removed
     * from the class file.
     * 
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public Map<String, AttributeInfo> getAttributes() {
        return attributes;
    }

    /**
     * Returns the attribute with the specified name.  If there are multiple
     * attributes with that name, this method returns either of them.   It
     * returns null if the specified attributed is not found.
     *
     * <p>An attribute name can be obtained by, for example,
     * {@link AnnotationsAttribute#visibleTag} or
     * {@link AnnotationsAttribute#invisibleTag}. 
     * </p>
     * 
     * @param name          attribute name
     * @see #getAttributes()
     */
    public AttributeInfo getAttribute(String name) {
        Map<String, AttributeInfo> list = attributes;
        return list.get(name);
    }

    /**
     * Removes an attribute with the specified name.
     *
     * @param name      attribute name.
     * @return          the removed attribute or null.
     * @since 3.21
     */
    public AttributeInfo removeAttribute(String name) {
        return AttributeInfo.remove(attributes, name);
    }

    /**
     * Appends an attribute. If there is already an attribute with the same
     * name, the new one substitutes for it.
     *
     * @see #getAttributes()
     */
    public void addAttribute(AttributeInfo info) {
        attributes.put(info.getName(), info);
    }

    /**
     * Returns the source file containing this class.
     * 
     * @return null if this information is not available.
     */
    public String getSourceFile() {
        SourceFileAttribute sf
            = (SourceFileAttribute)getAttribute(SourceFileAttribute.tag);
        if (sf == null)
            return null;
        else
            return sf.getFileName();
    }

    private void read(DataInputStream in) throws IOException {
        int i, n;
        int magic = in.readInt();
        if (magic != 0xCAFEBABE)
            throw new IOException("bad magic number: " + Integer.toHexString(magic));

        minor = in.readUnsignedShort();
        major = in.readUnsignedShort();
        constPool = new ConstPool(in);
        accessFlags = in.readUnsignedShort();
        thisClass = in.readUnsignedShort();
        constPool.setThisClassInfo(thisClass);
        superClass = in.readUnsignedShort();
        n = in.readUnsignedShort();
        if (n == 0)
            interfaces = null;
        else {
            interfaces = new int[n];
            for (i = 0; i < n; ++i)
                interfaces[i] = in.readUnsignedShort();
        }

        ConstPool cp = constPool;
        n = in.readUnsignedShort();
        fields = new LinkedHashMap<String, FieldInfo>();
        fields2 = new ArrayList<FieldInfo>();
        for (i = 0; i < n; ++i)
            addField2(new FieldInfo(cp, in));

        n = in.readUnsignedShort();
        methods = new LinkedHashMap<String, List<MethodInfo>>();
        methods2 = new ArrayList<MethodInfo>();
        for (i = 0; i < n; ++i)
            addMethod2(new MethodInfo(cp, in));

        attributes = new LinkedHashMap<String, AttributeInfo>();
        n = in.readUnsignedShort();
        for (i = 0; i < n; ++i)
            addAttribute(AttributeInfo.read(cp, in));

        thisclassname = constPool.getClassInfo(thisClass);
    }

    /**
     * Writes a class file represented by this object into an output stream.
     */
    public void write(final DataOutputStream out) throws IOException {
        int i, n;

        out.writeInt(0xCAFEBABE); // magic
        out.writeShort(minor); // minor version
        out.writeShort(major); // major version
        constPool.write(out); // constant pool
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);

        if (interfaces == null)
            n = 0;
        else
            n = interfaces.length;

        out.writeShort(n);
        for (i = 0; i < n; ++i)
            out.writeShort(interfaces[i]);

        n = fields2.size();
        out.writeShort(n);
        try {
            loopFields(new FieldInfoCallback() {
                @Override
                public void onInfo(FieldInfo info) {
                    try {
                        info.write(out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            
            n = methods2.size();
            out.writeShort(n);
            loopMethods(new MethodInfoCallback() {
                @Override
                public void onInfo(MethodInfo info) {
                    try {
                        info.write(out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw ex;
        }
        out.writeShort(attributes.size());
        AttributeInfo.writeAll(attributes, out);
    }

    /**
     * Get the Major version.
     * 
     * @return the major version
     */
    public int getMajorVersion() {
        return major;
    }

    /**
     * Set the major version.
     * 
     * @param major
     *            the major version
     */
    public void setMajorVersion(int major) {
        this.major = major;
    }

    /**
     * Get the minor version.
     * 
     * @return the minor version
     */
    public int getMinorVersion() {
        return minor;
    }

    /**
     * Set the minor version.
     * 
     * @param minor
     *            the minor version
     */
    public void setMinorVersion(int minor) {
        this.minor = minor;
    }

    /**
     * Sets the major and minor version to Java 5.
     *
     * If the major version is older than 49, Java 5
     * extensions such as annotations are ignored
     * by the JVM.
     */
    public void setVersionToJava5() {
        this.major = 49;
        this.minor = 0;
    }
}
