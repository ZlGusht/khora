package khora.kernel.surface;

import java.util.Objects;

/**
 * Whether a result holds everything that was asked for (ADR-0XY decisions 1 and 4).
 *
 * <p>Provisional.
 */
public sealed interface Completeness {

    /** Everything that was asked for. */
    record Complete() implements Completeness {}

    /** Only part of it, and why. */
    record Partial(String reason) implements Completeness {
        public Partial {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
