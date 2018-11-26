package javassist.bytecode;

import java.util.zip.*;

import javassist.bytecode.ClassFile.MethodInfoCallback;

import java.util.Enumeration;
import java.util.List;
import java.io.*;

@SuppressWarnings({"rawtypes","resource"})
public class CodeAnalyzerTest {
    public static void main(String[] args) throws Exception {
        ZipFile zfile = new ZipFile(args[0]);
        Enumeration e = zfile.entries();
        while (e.hasMoreElements()) {
            ZipEntry zip = (ZipEntry)e.nextElement();
            if (zip.getName().endsWith(".class"))
                test(zfile.getInputStream(zip));
        }
    }

    static void test(InputStream is) throws Exception {
        is = new BufferedInputStream(is);
        final ClassFile cf = new ClassFile(new DataInputStream(is));
        is.close();
        
        cf.loopMethods(new MethodInfoCallback() {
            @Override
            public void onInfo(MethodInfo info) {
                CodeAttribute ca = info.getCodeAttribute();
                if (ca != null) {
                    try {
                        int max = ca.getMaxStack();
                        int newMax = ca.computeMaxStack();
                        if (max != newMax)
                            System.out.println(max + " -> " + newMax +
                                               " for " + info.getName() + " (" +
                                               info.getDescriptor() + ") in " +
                                               cf.getName());
                    }
                    catch (BadBytecode e) {
                        System.out.println(e.getMessage() +
                                           " for " + info.getName() + " (" +
                                           info.getDescriptor() + ") in " +
                                           cf.getName());
                    }
                }
            }
        });
    }
}
