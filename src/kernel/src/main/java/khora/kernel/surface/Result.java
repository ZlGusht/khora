package khora.kernel.surface;

import khora.kernel.event.Frontier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import khora.kernel.event.EventId;

/**
 * What a read returns (ADR-0XY decision 1).
 *
 * <p>Provisional: {@code value} is a single optional string until results carry batches of data.
 *
 * @param action the action this read was
 * @param value what was read, if anything
 * @param completeness whether it is everything that was asked for
 * @param seen the frontier the surface saw
 * @param annotations what the result says about itself
 */
public record Result(EventId action, Optional<String> value, Completeness completeness, Frontier seen,
        List<Annotation> annotations) {
    public Result {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(completeness, "completeness");
        Objects.requireNonNull(seen, "seen");
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
    }
}
