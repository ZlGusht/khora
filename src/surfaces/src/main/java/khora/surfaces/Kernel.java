package khora.surfaces;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.Assertion;
import khora.events.log.EventLog;
import khora.events.log.EventLogException;

/**
 * An event log, a clock and the gates: where actions are submitted, become events, and are
 * delivered ([[Surfaces receive and emit events by interest]]).
 *
 * <p>Exploration: events are facts by whoever emitted them, and the kernel does not judge them. It
 * links an event to its trigger, the event it was emitted in response to, and to nothing else. The
 * gates that guard an action decide first; an action no gate guards always becomes an event.
 *
 * <p>Events are delivered in log order. Each attached surface has taken in the log up to a
 * position, and receives each later event its interests match once, and never one it emitted. What
 * a surface emits in response is submitted as an action by that surface, triggered by the event it
 * received; a chain of emissions in response to emissions stops after {@value #MAX_GENERATIONS}
 * steps. A surface that throws, or whose emission fails, loses that event; delivery goes on, and no
 * failure in delivery reaches the submitter.
 *
 * <p>An append whose outcome is unknown is retried with the same event, which the log stores once;
 * if it still fails, {@link Unconfirmed} carries the event.
 *
 * <p>Provisional: synchronous delivery under one lock, and no access checks at delivery.
 */
public final class Kernel {
    /** How many emissions in response to emissions are delivered before a chain is cut off. */
    public static final int MAX_GENERATIONS = 64;

    /** How often an append whose outcome is unknown is tried with the same event. */
    public static final int APPEND_ATTEMPTS = 3;

    /** What an event no gate guarded went {@code via}. */
    public static final String VIA = "kernel";

    private static final System.Logger LOG = System.getLogger(Kernel.class.getName());

    private final EventLog log;
    private final Hlc clock;
    private final List<Gate> gates;
    private long position;
    private final List<Attachment> attached = new ArrayList<>();
    private final ArrayDeque<Delivery> queue = new ArrayDeque<>();
    private final List<Event> appended = new ArrayList<>();
    private boolean draining;

    /** An attached surface: the position it has taken in up to, and what it emitted. */
    private static final class Attachment {
        final Surface surface;
        long position;
        final Set<EventId> emitted = new HashSet<>();

        Attachment(Surface surface, long position) {
            this.surface = surface;
            this.position = position;
        }
    }

    /** An appended event waiting to be delivered, its position, and how many emissions led to it. */
    private record Delivery(Event event, long position, int generation) {}

    /** A kernel over {@code log}, which may already hold events; each gate takes in what it holds. */
    public Kernel(EventLog log, Hlc clock, List<Gate> gates) {
        this.log = Objects.requireNonNull(log, "log");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.gates = List.copyOf(gates);
        for (long from = 1; ; ) {
            var page = log.read(from, EventLog.MAX_APPEND);
            if (page.isEmpty()) {
                break;
            }
            for (var logged : page) {
                this.gates.forEach(g -> take(g, logged.event()));
            }
            position = page.getLast().position();
            from = position + 1;
        }
    }

    /** A kernel over {@code log} with {@code gates}. */
    public Kernel(EventLog log, Hlc clock, Gate... gates) {
        this(log, clock, List.of(gates));
    }

    /** Submits {@code action} by {@code context}'s caller, in response to nothing. */
    public synchronized Answer submit(Action action, Context context) {
        return submit(action, context, Optional.empty());
    }

    /**
     * Submits {@code action} by {@code context}'s caller, in response to {@code trigger}. If no gate
     * that guards it refuses, it is appended as an event and delivered.
     */
    public synchronized Answer submit(Action action, Context context, Optional<EventId> trigger) {
        int from = appended.size();
        var answer = accept(action, context, trigger, null, 0);
        if (!(answer instanceof Answer.Logged logged)) {
            return answer;
        }
        var responses = new ArrayList<Event>();
        for (var e : appended.subList(from, appended.size())) {
            if (e.trigger().equals(Optional.of(logged.event().id()))) {
                responses.add(e);
            }
        }
        if (!draining) {
            appended.clear();
        }
        return new Answer.Logged(logged.event(), responses);
    }

    /**
     * Attaches {@code surface}, which has taken in the log up to {@code position}: it is sent every
     * later event it wants, in log order, then each new one.
     */
    public synchronized void attach(Surface surface, long position) {
        var attachment = new Attachment(Objects.requireNonNull(surface, "surface"), position);
        attached.add(attachment);
        boolean outer = !draining;
        draining = true;
        try {
            while (true) {
                var page = log.read(attachment.position + 1, EventLog.MAX_APPEND);
                if (page.isEmpty()) {
                    break;
                }
                for (var logged : page) {
                    offer(attachment, logged.event(), logged.position(), 0);
                }
            }
        } finally {
            if (outer) {
                draining = false;
            }
        }
        if (outer) {
            drain();
            appended.clear();
        }
    }

    private Answer accept(Action action, Context context, Optional<EventId> trigger, Attachment origin,
            int generation) {
        long after = Long.MIN_VALUE;
        if (trigger.isPresent()) {
            var known = log.get(trigger.get());
            if (known.isEmpty()) {
                return new Answer.Refused("unknown trigger " + trigger.get().value());
            }
            after = known.get().recorded();
        }
        var via = new ArrayList<String>();
        for (var gate : gates) {
            if (!gate.guards(action)) {
                continue;
            }
            Gate.Verdict verdict;
            try {
                verdict = gate.decide(action, context.by(gate.name()));
            } catch (RuntimeException e) {
                verdict = new Gate.Refuse("gate " + gate.name() + " failed: " + e.getMessage());
            }
            if (verdict instanceof Gate.Refuse refuse) {
                return new Answer.Refused(refuse.reason());
            }
            via.add(gate.name());
        }
        Event event;
        try {
            event = Event.create(trigger, clock.next(after), Optional.empty(),
                    new Emitted(context.by(via.isEmpty() ? VIA : String.join(",", via)), action));
        } catch (IllegalArgumentException e) {
            return new Answer.Refused(e.getMessage());
        }
        long at = append(event);
        position = Math.max(position, at);
        gates.forEach(g -> take(g, event));
        appended.add(event);
        if (origin != null) {
            origin.emitted.add(event.id());
        }
        queue.add(new Delivery(event, at, generation));
        if (!draining) {
            drain();
        }
        return new Answer.Logged(event, List.of());
    }

    private static void take(Gate gate, Event event) {
        try {
            gate.take(event);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "gate " + gate.name() + " failed to take in event "
                    + event.id().value(), e);
        }
    }

    /**
     * Appends {@code event}, retrying the same event when the outcome is unknown: the log stores an
     * id once, so a retry never stores it twice.
     *
     * @return its position
     * @throws Unconfirmed if every attempt failed
     */
    private long append(Event event) {
        EventLogException last = null;
        for (int attempt = 0; attempt < APPEND_ATTEMPTS; attempt++) {
            try {
                return log.append(List.of(event)).getFirst();
            } catch (EventLogException e) {
                last = e;
            }
        }
        throw new Unconfirmed(event, last);
    }

    /**
     * An event the log did not confirm storing: it may be stored or not. Appending {@link #event()}
     * itself again is safe; submitting the action again makes a new event.
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

    /** Delivers queued events, in order, to every attached surface. */
    private void drain() {
        draining = true;
        try {
            Delivery next;
            while ((next = queue.poll()) != null) {
                for (var attachment : List.copyOf(attached)) {
                    offer(attachment, next.event(), next.position(), next.generation());
                }
            }
        } finally {
            draining = false;
        }
    }

    /**
     * Hands {@code event} to the attachment if it is past what the attachment has taken in, it wants
     * it, and did not emit it. Nothing escapes: a failure loses this event for this attachment only.
     */
    private void offer(Attachment attachment, Event event, long at, int generation) {
        try {
            deliver(attachment, event, at, generation);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "delivering event " + event.id().value() + " to surface "
                    + attachment.surface.name() + " failed; delivery goes on", e);
        }
    }

    private void deliver(Attachment attachment, Event event, long at, int generation) {
        if (at <= attachment.position) {
            return;
        }
        attachment.position = at;
        if (attachment.emitted.contains(event.id()) || !wants(attachment.surface, event, log)) {
            return;
        }
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
                if (accept(action, by, Optional.of(event.id()), attachment, generation + 1) instanceof Answer.Refused r) {
                    LOG.log(System.Logger.Level.WARNING, "an action surface " + attachment.surface.name()
                            + " emitted on event " + event.id().value() + " was refused: " + r.reason());
                }
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "an action surface " + attachment.surface.name()
                        + " emitted on event " + event.id().value() + " failed; delivery goes on", e);
            }
        }
    }

    private static boolean wants(Surface surface, Event event, EventLog log) {
        return surface.interests().stream().anyMatch(interest -> interest.matches(event, log));
    }

    /** The position of the last event in the log. */
    public synchronized long position() {
        return position;
    }

    /** The event a tuple belongs to. */
    public synchronized Optional<Event> event(Identity tuple) {
        return tuple.event().flatMap(log::get);
    }

    /** The statement {@code id} names, if its event is in the log and holds it. */
    public synchronized Optional<Statement> statement(Identity id) {
        if (id.index().isEmpty()) {
            return Optional.empty();
        }
        int index = id.index().getAsInt();
        return event(id).flatMap(e -> e.body() instanceof Emitted emitted
                ? Assertion.in(emitted.action()).filter(t -> index < t.size())
                        .map(t -> new Statement(id, t.get(index).in(e.id())))
                : Optional.empty());
    }
}
