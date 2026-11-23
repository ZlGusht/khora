package khora.kernel.log;

/** A failure of a {@link LogStore} operation. */
public class LogStoreException extends RuntimeException {

    public LogStoreException(String message) {
        super(message);
    }

    public LogStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
