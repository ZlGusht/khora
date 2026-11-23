package khora.kernel.log;

import java.util.Objects;
import khora.kernel.event.Event;

/**
 * An event and its position in one log.
 *
 * @param position where it is in this log, from 1
 * @param event the event
 */
public record Logged(long position, Event event) {
    public Logged {
        if (position < 1) {
            throw new IllegalArgumentException("position is less than 1: " + position);
        }
        Objects.requireNonNull(event, "event");
    }
}
