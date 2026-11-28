package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;

/** What a scenario does after its given steps. */
public sealed interface When {
    /** Submits {@code step}. */
    static When submit(Step step) {
        return new Submit(step);
    }

    /** Attaches the surface under test, having taken in the log up to the latest of the steps {@code from}. */
    static When attach(String... from) {
        return new Attach(List.of(from));
    }

    /** Reads {@code thing}: the reference reader answers with the statements that stand for it. */
    static When read(Ref thing) {
        return new Read(thing);
    }

    /** Submits a step to the kernel. */
    record Submit(Step step) implements When {
        public Submit {
            Objects.requireNonNull(step, "step");
        }
    }

    /** Attaches the surface to the log, having taken it in up to the latest of the given steps. */
    record Attach(List<String> from) implements When {
        public Attach {
            from = List.copyOf(from);
        }
    }

    /** Reads a thing now. */
    record Read(Ref thing) implements When {
        public Read {
            Objects.requireNonNull(thing, "thing");
        }
    }
}
