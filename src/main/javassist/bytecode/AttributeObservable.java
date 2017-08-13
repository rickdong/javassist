package javassist.bytecode;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 * @author rdong
 */
public abstract class AttributeObservable {

    public static final String NAME = "NAME";

    public static final String ACC_FLAGS = "ACC_FLAGS";

    public static final String DESCRIPTOR = "DESCRIPTOR";

    private Set<AttributeChangeListener> changeListeners = Collections
            .newSetFromMap(new WeakHashMap<AttributeChangeListener, Boolean>());

    public void addChangeListener(AttributeChangeListener l) {
        changeListeners.add(l);
    }

    public void removeChangeListener(AttributeChangeListener l) {
        changeListeners.remove(l);
    }

    protected void fireChange(Object src, String name, Object oldValue, Object newValue) {
        for (AttributeChangeListener o : changeListeners) {
            o.onChange(src, name, oldValue, newValue);
        }
    }
}
