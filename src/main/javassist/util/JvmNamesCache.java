package javassist.util;

/**
 *
 * @author rdong
 */
public final class JvmNamesCache {

    public static String javaToJvmName(String classname) {
        return classname.replace('.', '/');
    }

    public static String jvmToJavaName(String classname) {
        return classname.replace('/', '.');
    }
}
