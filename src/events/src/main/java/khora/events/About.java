package khora.events;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What an event is about. An assert is about each of its tuples and the identity each one is about;
 * a statement about a statement, such as a retraction, is also about what that statement was about.
 * A read is about the thing it reads. A value that refers to something is a mention, not what the
 * statement is about.
 *
 * <p>Provisional: only accepted actions are about anything.
 */
public final class About {
    private About() {}

    /** The things {@code event} is about, in the order its action names them, given the events of {@code source}. */
    public static Set<Identity> of(Event event, EventSource source) {
        var things = new LinkedHashSet<Identity>();
        if (!(event.body() instanceof AcceptedAction accepted)) {
            return things;
        }
        Basic.readOf(accepted.action()).ifPresent(things::add);
        var tuples = Assertion.in(accepted.action()).orElse(List.of());
        for (int i = 0; i < tuples.size(); i++) {
            things.add(Identity.of(event.id(), i));
            var subject = tuples.get(i).in(event.id()).identity();
            while (subject.isPresent() && things.add(subject.get())) {
                subject = subjectOf(subject.get(), event, tuples, source);
            }
        }
        return things;
    }

    /** What the statement {@code id} is about, if {@code id} is a statement {@code source} or this event holds. */
    private static java.util.Optional<Identity> subjectOf(Identity id, Event event, List<Assertion> own, EventSource source) {
        var holder = id.event();
        if (holder.isEmpty()) {
            return java.util.Optional.empty();
        }
        int index = id.index().orElseThrow();
        if (holder.get().equals(event.id())) {
            return index < own.size() ? own.get(index).in(event.id()).identity() : java.util.Optional.empty();
        }
        return source.get(holder.get())
                .filter(e -> e.body() instanceof AcceptedAction)
                .flatMap(e -> Assertion.in(((AcceptedAction) e.body()).action())
                        .filter(tuples -> index < tuples.size())
                        .flatMap(tuples -> tuples.get(index).in(e.id()).identity()));
    }
}
