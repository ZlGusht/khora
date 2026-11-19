package khora.kernel.surface;

import java.util.Objects;
import java.util.Optional;
import khora.kernel.event.EventId;

/**
 * The context every read and write carries (ADR-0XY decision 1).
 *
 * <p>Provisional: {@code cause} is optional, because a person's first call has no causing event.
 *
 * @param principal who the call is made for
 * @param grant the grant it is made under
 * @param budget what it may spend
 * @param cause the event that caused it, if any
 */
public record Context(Principal principal, Grant grant, Budget budget, Optional<EventId> cause) {
    public Context {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(cause, "cause");
    }
}
