package khora.surfaces.contract;

import java.util.Objects;
import java.util.Optional;
import khora.events.Assertion;
import khora.events.Event;

/** A tuple as a scenario writes it: parts are refs, resolved when the scenario runs. */
public record TupleSpec(Optional<Ref> identity, Optional<Ref> attribute, Optional<ValueSpec> value) {
    public TupleSpec {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(attribute, "attribute");
        Objects.requireNonNull(value, "value");
    }

    /** The empty tuple, which instantiates an entity. */
    public static TupleSpec entity() {
        return new TupleSpec(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static TupleSpec of(Ref identity, Ref attribute, ValueSpec value) {
        return new TupleSpec(Optional.of(identity), Optional.of(attribute), Optional.of(value));
    }

    /** Retracts the assertion {@code assertion}. */
    public static TupleSpec retraction(Ref assertion) {
        return of(assertion, Ref.named("khora/basic/retracted"), ValueSpec.text("true"));
    }

    /** This tuple with its refs resolved. */
    public Assertion resolve(Bindings bindings, Optional<Event> received) {
        return new Assertion(identity.map(r -> bindings.identity(r, received)),
                attribute.map(r -> bindings.identity(r, received)),
                value.map(v -> switch (v) {
                    case ValueSpec.Literal l -> new Assertion.Literal(l.value());
                    case ValueSpec.RefValue r -> new Assertion.Ref(bindings.identity(r.ref(), received));
                }));
    }
}
