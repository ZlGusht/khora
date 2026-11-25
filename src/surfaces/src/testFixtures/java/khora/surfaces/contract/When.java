package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;

/** What a scenario does after its given steps. */
public sealed interface When {
    /** Submits a step through the surface. */
    record Submit(Step step) implements When {
        public Submit {
            Objects.requireNonNull(step, "step");
        }
    }

    /** Attaches the surface to the log, having seen the given steps. */
    record Attach(List<String> from) implements When {
        public Attach {
            from = List.copyOf(from);
        }
    }

    /** Reads a thing at the frontier of the given steps. */
    record Read(Ref thing, List<String> at) implements When {
        public Read {
            Objects.requireNonNull(thing, "thing");
            at = List.copyOf(at);
        }
    }
}
