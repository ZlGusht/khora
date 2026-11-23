package khora.kernel.surface;

import java.util.Objects;

/**
 * What a write asks to change.
 *
 * <p>Out with events: goes. A write is a surface's own event, translated down into basic events
 * ([[Events are the basic unit of khora]]).
 *
 * @param target what to change
 * @param value its new value
 */
public record Operation(String target, String value) {
    public Operation {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(value, "value");
    }
}
