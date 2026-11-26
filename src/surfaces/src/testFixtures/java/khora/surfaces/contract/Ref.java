package khora.surfaces.contract;

/** A way to name an identity in a scenario, resolved by {@link Bindings}. */
public sealed interface Ref {
    /** The event of a step. */
    record Step(String name) implements Ref {
        public Step {
            Check.name(name);
        }
    }

    /** The {@code index}-th tuple of a step's event. */
    record Tuple(String step, int index) implements Ref {
        public Tuple {
            Check.name(step);
            Check.index(index);
        }
    }

    /** A well-known name. */
    record Named(String name) implements Ref {
        public Named {
            Check.name(name);
        }
    }

    /** The {@code index}-th tuple of the event it is written in. */
    record Local(int index) implements Ref {
        public Local {
            Check.index(index);
        }
    }

    /** The {@code index}-th tuple of the event a surface responds to. */
    record Received(int index) implements Ref {
        public Received {
            Check.index(index);
        }
    }

    static Ref step(String name) {
        return new Step(name);
    }

    static Ref tuple(String step, int index) {
        return new Tuple(step, index);
    }

    static Ref named(String name) {
        return new Named(name);
    }

    static Ref local(int index) {
        return new Local(index);
    }

    static Ref received(int index) {
        return new Received(index);
    }
}
