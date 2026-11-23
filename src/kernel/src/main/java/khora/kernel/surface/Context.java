package khora.kernel.surface;

import java.util.Objects;
import java.util.Optional;
import khora.kernel.event.EventId;

/**
 * The context every read and write carries (ADR-0XY decision 1).
 *
 * <p>Provisional: {@code trigger} is optional, because a person's own call has none. ADR-0XY calls it
 * {@code cause}; it is renamed because an event's causes are what it knew, and a trigger is a named
 * link of its own ([[An event's causes are what it knew]]).
 *
 * @param principal who the call is made for
 * @param grant the grant it is made under
 * @param budget what it may spend
 * @param trigger the event that triggered it, if any
 */
public record Context(Principal principal, Grant grant, Budget budget, Optional<EventId> trigger) {
    public Context {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(trigger, "trigger");
    }
}
