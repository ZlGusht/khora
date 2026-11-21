package khora.kernel.event;

import java.util.Objects;

/**
 * The id of an event in the log.
 *
 * <p>Provisional: a placeholder until K1 (#18) defines event ids as content hashes over their causes.
 */
public record EventId(String value) {
    public EventId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }
}
