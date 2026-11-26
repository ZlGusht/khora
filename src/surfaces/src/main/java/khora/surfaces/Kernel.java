package khora.surfaces;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Basic;
import khora.events.Causality;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.FrontierCheck;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.Statements;
import khora.events.log.EventLog;
import khora.events.log.EventLogException;

/**
 * An event log, a clock and the authority: where actions are submitted, become events, and are
 * delivered ([[Surfaces receive and emit events by interest]]).
 *
 * <p>An action's causes are what the caller knew, a frontier, and the events it links to. A read
 * links the frontier it reads at as {@code at}, and answers with the statements that hold there.
 *
 * <p>Accepted events are delivered in the order they were appended, which is after their causes:
 * each attached surface receives each event its interests match once, and never one it emitted.
 * What a surface emits in response is submitted as an action by that surface, knowing what it had
 * taken in, linked to its {@code source}; a chain of emissions in response to emissions stops after
 * {@value #MAX_GENERATIONS} steps. A surface's emissions are causally ordered: each knows the ones
 * before it. A surface that throws, or whose emission fails, loses that event; delivery goes on, and
 * no failure in delivery reaches the submitter.
 *
 * <p>An append whose outcome is unknown is retried with the same event, which the log stores once;
 * if it still fails, {@link Unconfirmed} carries the event.
 *
 * <p>Provisional: one authority, synchronous delivery under one lock, no access checks at
 * delivery, and an emitted action the authority rejects is dropped without a trace.
 */
public final class Kernel {
    /** How many emissions in response to emissions are delivered before a chain is cut off. */
    public static final int MAX_GENERATIONS = 64;

    /** How often an append whose outcome is unknown is tried with the same event. */
    public static final int APPEND_ATTEMPTS = 3;

    private static final System.Logger LOG = System.getLogger(Kernel.class.getName());

    private final Authority authority;
    private final EventLog log;
    private final Hlc clock;
    private Frontier heads;
    private final List<Attachment> attached = new ArrayList<>();
    private final ArrayDeque<Delivery> queue = new ArrayDeque<>();
    private boolean draining;

    /** An attached surface: what it has taken in, and what it emitted. */
    private static final class Attachment {
        final Surface surface;
        Frontier taken;
        final Set<EventId> received = new HashSet<>();
        final Set<EventId> emitted = new HashSet<>();
        boolean catchingUp = true;

        Attachment(Surface surface, Frontier taken) {
            this.surface = surface;
            this.taken = taken;
        }
    }

    /** An accepted event waiting to be delivered, and how many emissions led to it. */
    private record Delivery(Event event, int generation) {}

    /** A kernel over {@code log}, which may already hold events. */
    public Kernel(Authority authority, EventLog log, Hlc clock) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.log = Objects.requireNonNull(log, "log");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.heads = headsOf(log);
    }

    /**
     * Submits {@code action} by {@code context}'s caller, who knew {@code knew}, with named
     * {@code links}. If the authority accepts it, it is appended as an event and delivered.
     */
    public synchronized Answer submit(Action action, Context context, Frontier knew, Map<String, Set<EventId>> links) {
        return accept(action, context, knew, links, null, 0);
    }

    /**
     * Attaches {@code surface}, which has taken in {@code from}: it is sent every matching event not in
     * {@code from}, in log order, then each new one that matches.
     *
     * @throws IllegalArgumentException if the log lacks an event of {@code from}
     */
    public synchronized void attach(Surface surface, Frontier from) {
        var known = FrontierCheck.closure(from.heads(), log);
        var attachment = new Attachment(Objects.requireNonNull(surface, "surface"), from);
        attached.add(attachment);
        boolean outer = !draining;
        draining = true;
        try {
            long position = 1;
            while (true) {
                var page = log.read(position, EventLog.MAX_APPEND);
                for (var logged : page) {
                    if (!known.contains(logged.event().id())) {
                        offer(attachment, logged.event(), 0);
                    }
                }
                if (page.isEmpty()) {
                    break;
                }
                position = page.getLast().position() + 1;
            }
        } finally {
            attachment.catchingUp = false;
            if (outer) {
                draining = false;
            }
        }
        if (outer) {
            drain();
        }
    }

    private Answer accept(Action action, Context context, Frontier knew, Map<String, Set<EventId>> links,
            Attachment origin, int generation) {
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
        Event event;
        try {
            event = Event.create(causes, clock.next(after), Optional.empty(),
                    new AcceptedAction(context.by(authority.name()), action, allLinks));
        } catch (IllegalArgumentException e) {
            return new Answer.Rejected(e.getMessage());
        }
        if (authority.decide(event, log) instanceof Authority.Reject reject) {
            return new Answer.Rejected(reject.reason());
        }
        var flaw = Causality.check(event, log);
        if (flaw.isPresent()) {
            return new Answer.Rejected(flaw.get().name());
        }
        // Answered before appending: what holds at knew does not depend on the new event, and
        // nothing may fail between the append and the return.
        var answer = new Answer.Accepted(event, read.map(thing -> Statements.about(thing, knew, log)).orElse(List.of()));
        append(event);
        var newHeads = new TreeSet<EventId>(heads.heads());
        newHeads.removeAll(event.causes());
        newHeads.add(event.id());
        heads = new Frontier(newHeads);
        if (origin != null) {
            origin.emitted.add(event.id());
            origin.taken = FrontierCheck.combine(origin.taken, new Frontier(new TreeSet<>(Set.of(event.id()))), log);
        }
        queue.add(new Delivery(event, generation));
        if (!draining) {
            drain();
        }
        return answer;
    }

    /**
     * Appends {@code event}, retrying the same event when the outcome is unknown: the log stores an
     * id once, so a retry never stores it twice.
     *
     * @throws Unconfirmed if every attempt failed
     */
    private void append(Event event) {
        EventLogException last = null;
        for (int attempt = 0; attempt < APPEND_ATTEMPTS; attempt++) {
            try {
                log.append(List.of(event));
                return;
            } catch (EventLogException e) {
                last = e;
            }
        }
        throw new Unconfirmed(event, last);
    }

    /**
     * An accepted event the log did not confirm storing: it may be stored or not. Appending
     * {@link #event()} itself again is safe; submitting the action again makes a new event.
     */
    public static final class Unconfirmed extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private final transient Event event;

        Unconfirmed(Event event, EventLogException cause) {
            super("event " + event.id().value() + " may not be stored", cause);
            this.event = event;
        }

        /** The event that may not be stored. */
        public Event event() {
            return event;
        }
    }

    /** Delivers queued events, in order, to every attached surface that is not catching up. */
    private void drain() {
        draining = true;
        try {
            Delivery next;
            while ((next = queue.poll()) != null) {
                for (var attachment : List.copyOf(attached)) {
                    if (!attachment.catchingUp) {
                        offer(attachment, next.event(), next.generation());
                    }
                }
            }
        } finally {
            draining = false;
        }
    }

    /**
     * Hands {@code event} to the attachment if it wants it and has neither received nor emitted it.
     * Nothing escapes: a failure loses this event for this attachment only, so it never reaches the
     * submitter of an event already appended.
     */
    private void offer(Attachment attachment, Event event, int generation) {
        try {
            deliver(attachment, event, generation);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "delivering event " + event.id().value() + " to surface "
                    + attachment.surface.name() + " failed; delivery goes on", e);
        }
    }

    private void deliver(Attachment attachment, Event event, int generation) {
        if (attachment.received.contains(event.id()) || attachment.emitted.contains(event.id())
                || !wants(attachment.surface, event, log)) {
            return;
        }
        attachment.received.add(event.id());
        attachment.taken = FrontierCheck.combine(attachment.taken, new Frontier(new TreeSet<>(Set.of(event.id()))), log);
        List<Action> emitted;
        try {
            emitted = attachment.surface.receive(event);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "surface " + attachment.surface.name() + " failed on event "
                    + event.id().value() + "; delivery goes on", e);
            return;
        }
        if (generation + 1 > MAX_GENERATIONS) {
            if (!emitted.isEmpty()) {
                LOG.log(System.Logger.Level.WARNING, "surface " + attachment.surface.name() + " emitted in response to a chain of "
                        + MAX_GENERATIONS + " emissions; dropped");
            }
            return;
        }
        var by = new Context(attachment.surface.name(), Optional.empty(), "surface", 0);
        for (var action : emitted) {
            try {
                accept(action, by, attachment.taken, Map.of("source", Set.of(event.id())), attachment, generation + 1);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "an action surface " + attachment.surface.name()
                        + " emitted on event " + event.id().value() + " failed; delivery goes on", e);
            }
        }
    }

    private static boolean wants(Surface surface, Event event, EventLog log) {
        return surface.interests().stream().anyMatch(interest -> interest.matches(event, log));
    }

    private static Frontier headsOf(EventLog log) {
        var heads = new TreeSet<EventId>();
        var caused = new HashSet<EventId>();
        long position = 1;
        while (true) {
            var page = log.read(position, EventLog.MAX_APPEND);
            for (var logged : page) {
                var event = logged.event();
                caused.addAll(event.causes());
                heads.removeAll(event.causes());
                if (!caused.contains(event.id())) {
                    heads.add(event.id());
                }
            }
            if (page.isEmpty()) {
                return new Frontier(heads);
            }
            position = page.getLast().position() + 1;
        }
    }

    /** Every event so far, by its newest events. */
    public synchronized Frontier frontier() {
        return heads;
    }

    /** The event a tuple belongs to. */
    public synchronized Optional<Event> event(Identity tuple) {
        return tuple.event().flatMap(log::get);
    }

    /** What holds about {@code thing} at {@code at}, without recording a read. */
    public synchronized List<Statement> statements(Identity thing, Frontier at) {
        return Statements.about(thing, at, log);
    }
}
