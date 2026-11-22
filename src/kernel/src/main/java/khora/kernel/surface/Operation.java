package khora.kernel.surface;

import java.util.Objects;

/**
 * What a write asks to change.
 *
 * <p>Provisional: sets {@code target} to {@code value}. Under [[Events are the basic unit of khora]] a
 * write is a surface's own event, translated down into basic events; this changes after K1 (#18).
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
