package khora.kernel.surface;

import java.util.Objects;

/**
 * The point a read happens at, or a write was conditioned on (ADR-0XY, Terms).
 *
 * <p>Provisional: a placeholder that only names a frontier. What a frontier is, and how frontiers
 * compare, combine and stay valid across origins, layers and branches, is decided in K1 (#18).
 * Until then a surface only recognises frontiers it reported itself.
 */
public record Frontier(String value) {
    public Frontier {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
    }
}
