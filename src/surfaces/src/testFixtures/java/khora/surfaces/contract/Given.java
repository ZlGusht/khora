package khora.surfaces.contract;

import java.util.List;

/** What a scenario does before its {@code When}: submit a step, or attach the surface under test. */
public sealed interface Given permits Step, Given.AttachSurface {

    /**
     * Attaches the surface under test here, having taken in the named earlier steps (none: from
     * nothing). It catches up on what the log holds; the steps after it, and a submitted {@code When},
     * are delivered to it live. A scenario attaches its surface at most once.
     */
    record AttachSurface(List<String> from) implements Given {
        public AttachSurface {
            from = List.copyOf(from);
        }
    }

    /** Attaches the surface under test, having taken in {@code from}. */
    static AttachSurface attach(String... from) {
        return new AttachSurface(List.of(from));
    }
}
