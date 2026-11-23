package khora.kernel.surface;

import java.util.Objects;

/**
 * The point a read happens at, or a write was conditioned on (ADR-0XY, Terms).
 *
 * <p>Out with events: replaced by K1's {@code khora.kernel.event.Frontier}.
 *
 * <p>Provisional: a placeholder that only names a frontier, so a surface recognises only frontiers
 * it reported itself and compares them by equality. K1 (#18) replaces it with a set of events closed
 * under causes, within the khora log. Origins, layers and branches are still open
 * ([[Frontiers need more than one position per origin]]).
 */
public record Frontier(String value) {
    public Frontier {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }
}
