package khora.kernel.surface;

import java.util.Objects;
import java.util.Optional;

/**
 * What a write assumes: the frontier it read and the value it depends on (ADR-0XY decision 7).
 *
 * <p>Out with events: {@code expected} goes, as values are derived from events; what the write read
 * stays, as the read it came from ([[An event's causes are what it knew]]).
 *
 * @param read the frontier the writer read at
 * @param expected the value the writer saw; empty means the target had no value
 */
public record Condition(Frontier read, Optional<String> expected) {
    public Condition {
        Objects.requireNonNull(read, "read");
        Objects.requireNonNull(expected, "expected");
    }
}
