package khora.kernel.surface;

import khora.kernel.event.Frontier;
import java.util.Objects;
import java.util.Optional;

/**
 * The time a read happens at (ADR-0XY decision 1: the request states the time it reads at).
 *
 * <p>Provisional: only a frontier. Which time it is measured in and which meaning past data is read
 * with come with K8's {@code Query} (ADR-0XX decision 2).
 *
 * @param at the frontier to read at; empty means the latest frontier the surface has reached
 */
public record ReadTime(Optional<Frontier> at) {
    public ReadTime {
        Objects.requireNonNull(at, "at");
    }

    /** A read at the latest frontier the surface has reached. */
    public static ReadTime latest() {
        return new ReadTime(Optional.empty());
    }
}
