package javassist.bytecode;

/**
 *
 * @author rdong
 */
public interface AttributeChangeListener {
    void onChange(Object src, String name, Object oldValue, Object newValue);
}
