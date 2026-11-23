package khora.kernel.surface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Action;
import khora.kernel.event.AppendResult;
import khora.kernel.event.Basic;
import khora.kernel.event.Event;
import khora.kernel.event.EventGraph;
import khora.kernel.event.EventId;
import khora.kernel.event.Frontier;
import khora.kernel.event.FrontierCheck;
import khora.kernel.event.Hlc;
import khora.kernel.event.Identity;
import khora.kernel.event.Statement;
import khora.kernel.event.Statements;

/**
 * The log, the clock and the authority: where actions are submitted, become events, and are
 * delivered ([[Surfaces receive and emit events by interest]]).
 *
 * <p>An action's causes are what the caller knew, a frontier, and the events it links to. A read
 * links the frontier it reads at as {@code at}, and answers with the statements that hold there.
 *
 * <p>Provisional: one authority, one in-memory log, synchronous delivery.
 */
public final class Kernel {
    private final Authority authority;
    private final Hlc clock;
    private final EventGraph log = new EventGraph();

    public Kernel(Authority authority, Hlc clock) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Submits {@code action} by {@code context}'s caller, who knew {@code knew}, with named
     * {@code links}. If the authority accepts it, it is appended as an event.
     */
    public synchronized Answer submit(Action action, Context context, Frontier knew, Map<String, Set<EventId>> links) {
        if (FrontierCheck.validate(knew, log) instanceof FrontierCheck.Invalid invalid) {
            return new Answer.Rejected("unknown event " + invalid.missing().value());
        }
        var read = Basic.readOf(action);
        var allLinks = new HashMap<>(links);
        if (read.isPresent() && !knew.heads().isEmpty()) {
            allLinks.put("at", knew.heads());
        }
        var causes = new TreeSet<EventId>(knew.heads());
        allLinks.values().forEach(causes::addAll);
        long after = Long.MIN_VALUE;
        for (var cause : causes) {
            var known = log.get(cause);
            if (known.isEmpty()) {
                return new Answer.Rejected("unknown event " + cause.value());
            }
            after = Math.max(after, known.get().recorded());
        }
        var event = Event.create(causes, clock.next(after), Optional.empty(),
                new AcceptedAction(context.by(authority.name()), action, allLinks));
        if (authority.decide(event, log) instanceof Authority.Reject reject) {
            return new Answer.Rejected(reject.reason());
        }
        if (log.append(event) instanceof AppendResult.Refused refused) {
            return new Answer.Rejected(refused.reason().name());
        }
        return new Answer.Accepted(event, read.map(thing -> Statements.about(thing, knew, log)).orElse(List.of()));
    }

    /** Every event so far. */
    public synchronized Frontier frontier() {
        return log.heads();
    }

    /** What holds about {@code thing} at {@code at}, without recording a read. */
    public synchronized List<Statement> statements(Identity thing, Frontier at) {
        return Statements.about(thing, at, log);
    }
}
