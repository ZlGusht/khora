package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;

/** What a scenario does after its given steps. */
public sealed interface When {
    /** Submits {@code step}. */
    static When submit(Step step) {
        return new Submit(step);
    }

    /** Attaches the surface under test, having seen the steps {@code from}. */
    static When attach(String... from) {
        return new Attach(List.of(from));
    }

    /** Reads {@code thing} at the frontier of the steps {@code at}. */
    static When read(Ref thing, String... at) {
        return new Read(thing, List.of(at));
    }

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
