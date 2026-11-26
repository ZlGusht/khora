package khora.surfaces.contract;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import khora.events.Assertion;
import khora.events.Event;
import khora.events.Identity;

/** A tuple as a scenario writes it: parts are refs, resolved when the scenario runs. */
public record TupleSpec(Optional<Ref> identity, Optional<Ref> attribute, Optional<ValueSpec> value) {
    /** The value {@link Assertion#retraction} writes. */
    private static final ValueSpec RETRACTED_VALUE = new ValueSpec.Literal(
            ((Assertion.Literal) Assertion.retraction(Identity.local(0)).value().orElseThrow()).value());

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

    /**
     * Retracts the assertion {@code assertion}, as {@link Assertion#retraction} does: its attribute is
     * {@link khora.events.Basic#RETRACTED}, named by the name it is made from.
     */
    public static TupleSpec retraction(Ref assertion) {
        return of(assertion, Ref.named("khora/basic/retracted"), RETRACTED_VALUE);
    }

    /** The refs this tuple names, identity, attribute and value. */
    public Stream<Ref> refs() {
        return Stream.of(identity, attribute, value.flatMap(v -> v instanceof ValueSpec.RefValue r
                ? Optional.of(r.ref()) : Optional.<Ref>empty())).flatMap(Optional::stream);
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
