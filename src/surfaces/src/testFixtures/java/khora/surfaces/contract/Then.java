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

    /** The surface emits these actions, in order. */
    record Emits(List<ActionSpec> actions) implements Then {
        public Emits {
            actions = List.copyOf(actions);
        }
    }

    /** The read answers with these tuples. */
    record Statements(List<TupleSpec> tuples) implements Then {
        public Statements {
            tuples = List.copyOf(tuples);
        }
    }
}
