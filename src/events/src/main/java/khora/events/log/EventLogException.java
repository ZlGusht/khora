package khora.events.log;

import java.io.Serial;

/** A log could not do what was asked. See {@link EventLog#append} for what it means for appends. */
public class EventLogException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EventLogException(String message) {
        super(message);
    }

    public EventLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
