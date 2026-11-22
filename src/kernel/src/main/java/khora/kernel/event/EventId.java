package khora.kernel.event;

import java.util.Objects;

/**
 * The id of an event in the log.
 *
 * <p>Provisional: a placeholder. K1 (#18) replaces it with salted hashes of an event's canonical JSON.
 */
public record EventId(String value) {
    public EventId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }
}
