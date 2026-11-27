package khora.surfaces;

import java.util.Objects;
import khora.events.Action;
import khora.events.By;
import khora.events.Event;

/**
 * The surface a shape's writes go through first, where that shape needs prevention rather than
 * detection: a unique name, the last seat, a payment. It keeps its own view of the shape from every
 * event, and judges an action against that view before the action becomes an event.
 *
 * <p>Exploration: replaces the kernel-wide authority. A gate is chosen per shape and guards only
 * the actions {@link #guards} names; nothing judges the others. A refused action leaves no event; a
 * let-through one names the gate as what it went {@code via}.
 */
public interface Gate {

    /** Its name: what a let-through event went {@code via}. */
    String name();

    /** Whether {@code action} touches the shape this gate guards. */
    boolean guards(Action action);

    /** Whether {@code action} by {@code by} may become an event, given what this gate has taken in. */
    Verdict decide(Action action, By by);

    /** Takes in one event of the log: every event, in log order, as soon as it is appended. */
    void take(Event event);

    sealed interface Verdict permits Pass, Refuse {}

    record Pass() implements Verdict {}

    record Refuse(String reason) implements Verdict {
        public Refuse {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
