package khora.kernel.event;

import java.util.Optional;

/**
 * Whether an event fits what it knew: every cause is known, it was recorded after each of them, and
 * every event it links to is in its causal past ([[An event's causes are what it knew]]). Checked
 * before an event is appended; the log itself only stores.
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
        LINK_OUTSIDE_PAST
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
        if (event.body() instanceof AcceptedAction accepted && !accepted.links().isEmpty()) {
            var past = FrontierCheck.closure(event.causes(), source);
            for (var ids : accepted.links().values()) {
                if (!past.containsAll(ids)) {
                    return Optional.of(Flaw.LINK_OUTSIDE_PAST);
                }
            }
        }
        return Optional.empty();
    }
}
