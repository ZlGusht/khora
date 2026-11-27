package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** What a scenario expects. */
public sealed interface Then {
    /** The submission becomes an event. */
    static Then logged() {
        return new Logged();
    }

    /** A gate refuses the submission, for any reason. */
    static Then refused() {
        return new Refused(Optional.empty());
    }

    /** A gate refuses the submission, for a reason containing {@code text}. */
    static Then refused(String text) {
        return new Refused(Optional.of(text));
    }

    /** The reference reader answers the read with exactly these tuples, in log order. */
    static Then statements(TupleSpec... tuples) {
        return new Statements(List.of(tuples));
    }

    /** The surface answers delivery by delivery, as {@link Emits} says. */
    static Then emits(Emits.After... groups) {
        return new Emits(groups);
    }

    /** The submission becomes an event. */
    record Logged() implements Then {}

    /** A gate refuses the submission, for a reason containing the text if given. */
    record Refused(Optional<String> reasonContains) implements Then {
        public Refused {
            Objects.requireNonNull(reasonContains, "reasonContains");
        }
    }

    /**
     * What the surface under test returns, delivery by delivery: for each group, in delivery order,
     * the event of its step and the actions the surface returned for it. A delivery for which the
     * surface returned nothing need not be listed; listed with no actions, it must have returned
     * nothing. The actions are resolved without a received event, so they name the step's tuples
     * ({@link Ref#tuple}), never {@link Ref.Received}.
     */
    record Emits(List<After> groups) implements Then {
        public Emits {
            groups = List.copyOf(groups);
        }

        public Emits(After... groups) {
            this(List.of(groups));
        }

        /** The actions the surface returns for the event of step {@code step}, in order. */
        public record After(String step, List<ActionSpec> actions) {
            public After {
                Check.name(step);
                actions = List.copyOf(actions);
            }
        }

        /** The surface answers the event of {@code step} with {@code actions}. */
        public static After after(String step, ActionSpec... actions) {
            return new After(step, List.of(actions));
        }
    }

    /** The read answers with these tuples. */
    record Statements(List<TupleSpec> tuples) implements Then {
        public Statements {
            tuples = List.copyOf(tuples);
        }
    }
}
