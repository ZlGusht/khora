package khora.kernel.event;

import java.util.Objects;

/**
 * The id of an event in the log.
 *
 * <p>Out with events: the placeholder constructor goes. K1 (#18) makes ids salted hashes of an
 * event's canonical JSON.
 */
public record EventId(String value) {
    public EventId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }
}
