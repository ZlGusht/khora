package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.Identity;
import khora.events.json.Json;

/** An action as a scenario writes it. */
public sealed interface ActionSpec {
    /** An assert of {@code tuples}, in order. */
    static ActionSpec asserts(TupleSpec... tuples) {
        return new Assert(List.of(tuples));
    }

    /** A read of {@code thing}. */
    static ActionSpec read(Ref thing) {
        return new Read(thing);
    }

    /** The action this spec stands for, its refs resolved. */
    Action resolve(Bindings bindings, Optional<Event> received);

    /** The refs this spec names. */
    default Stream<Ref> refs() {
        return switch (this) {
            case Assert a -> a.tuples().stream().flatMap(TupleSpec::refs);
            case Read r -> Stream.of(r.thing());
            case Raw ignored -> Stream.empty();
        };
    }

    /** An assert of one or more tuples. */
    record Assert(List<TupleSpec> tuples) implements ActionSpec {
        public Assert {
            tuples = List.copyOf(tuples);
            if (tuples.isEmpty()) {
                throw new IllegalArgumentException("an assert carries at least one tuple");
            }
        }

        @Override
        public Action resolve(Bindings bindings, Optional<Event> received) {
            return Assertion.action(tuples.stream().map(t -> t.resolve(bindings, received)).toList());
        }
    }

    /** A read of one thing. */
    record Read(Ref thing) implements ActionSpec {
        public Read {
            Objects.requireNonNull(thing, "thing");
        }

        @Override
        public Action resolve(Bindings bindings, Optional<Event> received) {
            return Basic.read(bindings.identity(thing, received));
        }
    }

    /** An action of any definition, with its arguments as they are. */
    record Raw(Identity definition, Json.Obj args) implements ActionSpec {
        public Raw {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(args, "args");
        }

        @Override
        public Action resolve(Bindings bindings, Optional<Event> received) {
            return new Action(definition, args);
        }
    }
}
