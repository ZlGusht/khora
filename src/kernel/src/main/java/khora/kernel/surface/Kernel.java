package khora.kernel.surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Action;
import khora.kernel.event.Basic;
import khora.kernel.event.Causality;
import khora.kernel.event.Event;
import khora.kernel.event.EventId;
import khora.kernel.event.Frontier;
import khora.kernel.event.FrontierCheck;
import khora.kernel.event.Hlc;
import khora.kernel.event.Identity;
import khora.kernel.event.Statement;
import khora.kernel.event.Statements;
import khora.kernel.log.EventLog;

/**
 * An event log, a clock and the authority: where actions are submitted, become events, and are
 * delivered ([[Surfaces receive and emit events by interest]]).
 *
 * <p>An action's causes are what the caller knew, a frontier, and the events it links to. A read
 * links the frontier it reads at as {@code at}, and answers with the statements that hold there.
 *
 * <p>Every accepted event is delivered to each attached surface whose interests it matches, after
 * its causes. What a surface emits in response is submitted as an action by that surface, knowing
 * what it had taken in, linked to its {@code source}.
 *
 * <p>Provisional: one authority, synchronous delivery, no access checks at delivery; the kernel
 * keeps the log's heads only while it runs, so it starts from an empty log.
 */
public final class Kernel {
    private final Authority authority;
    private final Hlc clock;
    private final EventLog log;
    private Frontier heads = Frontier.empty();
    private final List<Attachment> attached = new ArrayList<>();

    /** An attached surface and the frontier of what it has taken in. */
    private static final class Attachment {
        final Surface surface;
        Frontier taken;

        Attachment(Surface surface, Frontier taken) {
            this.surface = surface;
            this.taken = taken;
        }
    }

    public Kernel(Authority authority, EventLog log, Hlc clock) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.log = Objects.requireNonNull(log, "log");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Submits {@code action} by {@code context}'s caller, who knew {@code knew}, with named
     * {@code links}. If the authority accepts it, it is appended as an event.
     */
    public synchronized Answer submit(Action action, Context context, Frontier knew, Map<String, Set<EventId>> links) {
        return accept(action, context, knew, links, null);
    }

    /**
     * Attaches {@code surface}, which has taken in {@code from}: it is sent every matching event not in
     * {@code from}, in order, then each new one that matches.
     *
     * @throws IllegalArgumentException if the log lacks an event of {@code from}
     */
    public synchronized void attach(Surface surface, Frontier from) {
        var known = FrontierCheck.closure(from.heads(), log);
        var attachment = new Attachment(Objects.requireNonNull(surface, "surface"), from);
        attached.add(attachment);
        for (long position = 1; ; position += EventLog.MAX_APPEND) {
            var page = log.read(position, EventLog.MAX_APPEND);
            for (var logged : page) {
                if (!known.contains(logged.event().id()) && wants(surface, logged.event())) {
                    deliver(attachment, logged.event());
                }
            }
            if (page.size() < EventLog.MAX_APPEND) {
                return;
            }
        }
    }

    private Answer accept(Action action, Context context, Frontier knew, Map<String, Set<EventId>> links,
            Attachment origin) {
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
        var flaw = Causality.check(event, log);
        if (flaw.isPresent()) {
            return new Answer.Rejected(flaw.get().name());
        }
        log.append(List.of(event));
        var newHeads = new TreeSet<EventId>(heads.heads());
        newHeads.removeAll(event.causes());
        newHeads.add(event.id());
        heads = new Frontier(newHeads);
        var answer = new Answer.Accepted(event, read.map(thing -> Statements.about(thing, knew, log)).orElse(List.of()));
        for (var attachment : List.copyOf(attached)) {
            if (attachment != origin && wants(attachment.surface, event)) {
                deliver(attachment, event);
            }
        }
        return answer;
    }

    private void deliver(Attachment attachment, Event event) {
        attachment.taken = FrontierCheck.combine(attachment.taken, new Frontier(new TreeSet<>(Set.of(event.id()))), log);
        var by = new Context(attachment.surface.name(), Optional.empty(), "surface", 0);
        for (var action : attachment.surface.receive(event)) {
            accept(action, by, attachment.taken, Map.of("source", Set.of(event.id())), attachment);
        }
    }

    private static boolean wants(Surface surface, Event event) {
        return surface.interests().stream().anyMatch(interest -> interest.matches(event));
    }

    /** Every event so far. */
    public synchronized Frontier frontier() {
        return heads;
    }

    /** The event a tuple, or an event's own identity, belongs to. */
    public synchronized Optional<Event> event(Identity tuple) {
        return tuple.event().flatMap(log::get);
    }

    /** What holds about {@code thing} at {@code at}, without recording a read. */
    public synchronized List<Statement> statements(Identity thing, Frontier at) {
        return Statements.about(thing, at, log);
    }
}
