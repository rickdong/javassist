package javassist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author rdong
 */
public class NoStackTraceException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final Map<Class<?>, AtomicLong> COUNT = new ConcurrentHashMap<>();

    private void updateCount() {
        Class<? extends NoStackTraceException> cls = getClass();
        AtomicLong c = COUNT.get(cls);
        if (c == null) {
            AtomicLong prev = COUNT.putIfAbsent(cls, c = new AtomicLong());
            if (prev != null) {
                c = prev;
            }
        }
        c.incrementAndGet();
    }

    public NoStackTraceException() {
        updateCount();
    }

    public static Map<Class<?>, AtomicLong> getCount() {
        return COUNT;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public NoStackTraceException(String string) {
        super(string);
        updateCount();
    }

    public NoStackTraceException(String string, Throwable throwable) {
        super(string, throwable);
        updateCount();
    }

    public NoStackTraceException(Throwable throwable) {
        super(throwable);
        updateCount();
    }

}
