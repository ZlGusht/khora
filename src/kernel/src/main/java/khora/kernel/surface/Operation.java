package khora.kernel.surface;

import java.util.Objects;

/**
 * What a write asks to change.
 *
 * <p>Provisional: sets {@code target} to {@code value} until operations are defined.
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
