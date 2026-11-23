package khora.kernel.surface;

import java.util.Objects;

/**
 * Something a result says about itself, such as "as observed at 10:42" or "not reproducible"
 * (ADR-0XY decision 2).
 *
 * <p>Provisional: free text until annotations are given a structure.
 */
public record Annotation(String text) {
    public Annotation {
        Objects.requireNonNull(text, "text");
    }
}
