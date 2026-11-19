package khora.kernel.surface;

import java.util.Objects;
import java.util.Optional;

/**
 * The time a read happens at (ADR-0XY decision 1: the request states the time it reads at).
 *
 * <p>Provisional: until K8's {@code Query} carries its own time.
 *
 * @param at the frontier to read at; empty means the latest frontier the surface has reached
 * @param by which time the frontier is measured in
 * @param meaning which definitions past data is read with
 */
public record ReadTime(Optional<Frontier> at, TimeAxis by, Meaning meaning) {
    public ReadTime {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(meaning, "meaning");
    }

    /** A read at the latest frontier the surface has reached, by recorded time, with the meaning of then. */
    public static ReadTime latest() {
        return new ReadTime(Optional.empty(), TimeAxis.RECORDED, Meaning.THEN);
    }
}
