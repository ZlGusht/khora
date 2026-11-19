package khora.kernel.surface;

import java.util.Objects;

/**
 * Who a call is made for.
 *
 * <p>Provisional: an opaque id until principals are defined (ADR-0XZ).
 */
public record Principal(String id) {
    public Principal {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id is blank");
        }
    }
}
