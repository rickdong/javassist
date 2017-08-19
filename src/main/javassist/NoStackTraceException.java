package javassist;

/**
 *
 * @author rdong
 */
public class NoStackTraceException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoStackTraceException() {
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public NoStackTraceException(String string) {
        super(string);
    }

    public NoStackTraceException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public NoStackTraceException(Throwable throwable) {
        super(throwable);
    }

}
