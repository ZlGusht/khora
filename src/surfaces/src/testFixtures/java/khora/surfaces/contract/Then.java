package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** What a scenario expects. */
public sealed interface Then {
    /** The submission is accepted. */
    record Accepted() implements Then {}

    /** The submission is rejected, for a reason containing the text if given. */
    record Rejected(Optional<String> reasonContains) implements Then {
        public Rejected {
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
