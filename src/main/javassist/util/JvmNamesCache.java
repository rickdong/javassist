package javassist.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author rdong
 */
public final class JvmNamesCache {

    private static final ConcurrentMap<String, String> JAVA_TO_JVM_NAMES = new ConcurrentHashMap<String, String>();

    private static final ConcurrentMap<String, String> JVM_TO_JAVA_NAMES = new ConcurrentHashMap<String, String>();

    public static String javaToJvmName(String classname) {
        String name = JAVA_TO_JVM_NAMES.get(classname);
        if (name != null) {
            return name;
        }

        name = classname.replace('.', '/');
        String prev = JAVA_TO_JVM_NAMES.putIfAbsent(classname, name);
        if (prev != null) {
            name = prev;
        }
        return name;
    }

    public static String jvmToJavaName(String classname) {
        String name = JVM_TO_JAVA_NAMES.get(classname);
        if (name != null) {
            return name;
        }

        name = classname.replace('/', '.');
        String prev = JVM_TO_JAVA_NAMES.putIfAbsent(classname, name);
        if (prev != null) {
            name = prev;
        }
        return name;
    }
}
