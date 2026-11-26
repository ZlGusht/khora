package khora.events;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Whether an event fits what it knew: every cause is known, it was recorded after each of them, and
 * every event it links to, and every statement its tuples name, is in its causal past
 * ([[An event's causes are what it knew]]). Checked before an event is appended; the log itself only
 * stores.
 */
public final class Causality {
    private Causality() {}

    /** What is wrong with an event's place among the others. */
    public enum Flaw {
        /** A cause is not known. */
        MISSING_CAUSE,
        /** It is not recorded after every cause. */
        NOT_AFTER_CAUSES,
        /** A named link points outside its causal past. */
        LINK_OUTSIDE_PAST,
        /** A tuple names a statement of an event outside its causal past. */
        STATEMENT_OUTSIDE_PAST,
        /** A tuple names a statement its event does not hold. */
        UNKNOWN_STATEMENT
    }

    /** The flaw in {@code event} given the events of {@code source}, or empty if it has none. */
    public static Optional<Flaw> check(Event event, EventSource source) {
        for (var causeId : event.causes()) {
            var cause = source.get(causeId);
            if (cause.isEmpty()) {
                return Optional.of(Flaw.MISSING_CAUSE);
            }
            if (event.recorded() <= cause.get().recorded()) {
                return Optional.of(Flaw.NOT_AFTER_CAUSES);
            }
        }
        if (!(event.body() instanceof AcceptedAction accepted)) {
            return Optional.empty();
        }
        var tuples = Assertion.in(accepted.action()).orElse(List.of());
        Set<EventId> past = null;
        if (!accepted.links().isEmpty()) {
            past = FrontierCheck.closure(event.causes(), source);
            for (var ids : accepted.links().values()) {
                if (!past.containsAll(ids)) {
                    return Optional.of(Flaw.LINK_OUTSIDE_PAST);
                }
            }
        }
        for (var tuple : tuples) {
            for (var id : tuple.identities()) {
                // Local identities name this event's own tuples; Assertion checks their range.
                if (id.isLocal() || id.event().isEmpty()) {
                    continue;
                }
                var holder = id.event().get();
                if (past == null) {
                    past = FrontierCheck.closure(event.causes(), source);
                }
                if (!past.contains(holder)) {
                    return Optional.of(Flaw.STATEMENT_OUTSIDE_PAST);
                }
                var held = source.get(holder)
                        .filter(e -> e.body() instanceof AcceptedAction)
                        .flatMap(e -> Assertion.in(((AcceptedAction) e.body()).action()))
                        .map(List::size)
                        .orElse(0);
                if (id.index().orElseThrow() >= held) {
                    return Optional.of(Flaw.UNKNOWN_STATEMENT);
                }
            }
        }
        return Optional.empty();
    }
}
