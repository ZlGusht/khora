package khora.kernel.surface;

import java.util.Objects;

/**
 * The grant a call is made under. It decides on whose behalf the call acts.
 *
 * <p>Provisional: an opaque id until grants are defined (ADR-0XZ).
 */
public record Grant(String id) {
    public Grant {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id is blank");
        }
    }
}
