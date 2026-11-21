package khora.kernel.surface;

import java.util.Objects;

/**
 * What a read asks for, and the time it reads at.
 *
 * <p>Provisional: {@code target} is a plain name until K8's {@code Query} replaces it.
 *
 * @param target what to read
 * @param time the time to read at
 */
public record ReadRequest(String target, ReadTime time) {
    public ReadRequest {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(time, "time");
    }
}
