package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.json.Json;
import org.junit.jupiter.api.Test;

class AssertionTest {

    static final Identity P = Identity.named("p");
    static final Identity NAME = Identity.named("name");
    static final Identity ANNA = Identity.named("anna");

    @Test
    void entityIsTheEmptyTuple() {
        var action = Assertion.action(Assertion.entity());
        assertEquals(Basic.ASSERT, action.definition());
        assertEquals("{\"tuples\":[{}]}", action.args().canonical());
    }

    @Test
    void literalValueIsMarked() {
        var a = Assertion.of(P, NAME, new Assertion.Literal(new Json.Str("Kitchen")));
        assertEquals("{\"tuples\":[{\"attribute\":\"" + NAME.value() + "\",\"identity\":\"" + P.value()
                + "\",\"value\":{\"literal\":\"Kitchen\"}}]}", Assertion.action(a).args().canonical());
    }

    @Test
    void referenceValueIsMarked() {
        var a = Assertion.of(ANNA, Identity.named("owns"), new Assertion.Ref(P));
        assertEquals("{\"ref\":\"" + P.value() + "\"}",
                Assertion.action(a).args().canonical().replaceAll(".*\"value\":(\\{[^}]*\\}).*", "$1"));
    }

    @Test
    void severalTuplesRoundTripInOrder() {
        var tuples = List.of(
                Assertion.entity(),
                Assertion.of(P, NAME, new Assertion.Literal(new Json.Int(7))),
                Assertion.of(ANNA, NAME, new Assertion.Ref(P)),
                new Assertion(Optional.of(P), Optional.of(NAME), Optional.empty()));
        assertEquals(Optional.of(tuples), Assertion.in(Assertion.action(tuples)));
    }

    @Test
    void localIdentitiesResolveInTheirEvent() {
        var e = EventId.hashOf("e");
        assertEquals(Assertion.of(Identity.of(e, 0), NAME, new Assertion.Ref(Identity.of(e, 1))),
                Assertion.of(Identity.local(0), NAME, new Assertion.Ref(Identity.local(1))).in(e));
    }

    @Test
    void anAssertNeedsATuple() {
        assertThrows(IllegalArgumentException.class, () -> Assertion.action(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Assertion.in(new Action(Basic.ASSERT, Json.Obj.of(Map.of("tuples", new Json.Arr(List.of()))))));
    }

    @Test
    void otherActionsCarryNoAssertions() {
        var other = new Action(Identity.named("orders/place"), Json.Obj.of(Map.of()));
        assertEquals(Optional.empty(), Assertion.in(other));
    }

    @Test
    void malformedTuplesAreRefused() {
        for (var tuple : new Json.Obj[] {
                Json.Obj.of(Map.of("subject", new Json.Str(P.value()))),
                Json.Obj.of(Map.of("identity", new Json.Int(1))),
                Json.Obj.of(Map.of("identity", new Json.Str("not an identity"))),
                Json.Obj.of(Map.of("value", Json.Obj.of(Map.of(
                        "ref", new Json.Str(P.value()), "literal", new Json.Str("x"))))),
                Json.Obj.of(Map.of("value", Json.Obj.of(Map.of("literal", Json.Obj.of(Map.of())))))}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Action(Basic.ASSERT, Json.Obj.of(Map.of("tuples", new Json.Arr(List.of(tuple))))),
                    tuple.canonical());
        }
        assertThrows(IllegalArgumentException.class, () -> new Action(Basic.ASSERT, Json.Obj.of(Map.of())));
    }

    @Test
    void retractionIsATupleWithTheRetractedAttribute() {
        assertEquals(Assertion.of(P, Basic.RETRACTED, new Assertion.Literal(new Json.Str("true"))),
                Assertion.retraction(P));
    }

    @Test
    void anAssertActionCannotBeBuiltWithBadTuples() {
        assertThrows(IllegalArgumentException.class,
                () -> new Action(Basic.ASSERT, Json.Obj.of(Map.of("tuples", new Json.Arr(List.of())))));
        assertThrows(IllegalArgumentException.class,
                () -> new Action(Basic.ASSERT, Json.Obj.of(Map.of("x", new Json.Int(1)))));
    }
}
