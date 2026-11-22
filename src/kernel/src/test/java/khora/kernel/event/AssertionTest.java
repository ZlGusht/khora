package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import khora.kernel.json.Json;
import org.junit.jupiter.api.Test;

class AssertionTest {

    static final EventId P = EventId.hashOf("p");
    static final EventId NAME = EventId.hashOf("name");
    static final EventId ANNA = EventId.hashOf("anna");

    @Test
    void entityIsTheEmptyTuple() {
        var action = Assertion.entity().toAction();
        assertEquals(Basic.ASSERT, action.definition());
        assertEquals("{}", action.args().canonical());
    }

    @Test
    void literalValueIsMarked() {
        var a = Assertion.of(P, NAME, new Assertion.Literal(new Json.Str("Kitchen")));
        assertEquals("{\"attribute\":\"" + NAME.value() + "\",\"identity\":\"" + P.value()
                + "\",\"value\":{\"literal\":\"Kitchen\"}}", a.toAction().args().canonical());
    }

    @Test
    void referenceValueIsMarked() {
        var a = Assertion.of(ANNA, EventId.hashOf("owns"), new Assertion.Ref(P));
        assertEquals("{\"ref\":\"" + P.value() + "\"}", a.toAction().args().obj("value").canonical());
    }

    @Test
    void tuplesRoundTripThroughActions() {
        for (var a : new Assertion[] {
                Assertion.entity(),
                Assertion.of(P, NAME, new Assertion.Literal(new Json.Int(7))),
                Assertion.of(ANNA, NAME, new Assertion.Ref(P)),
                new Assertion(Optional.of(P), Optional.of(NAME), Optional.empty())}) {
            assertEquals(Optional.of(a), Assertion.from(a.toAction()));
        }
    }

    @Test
    void otherActionsAreNotAssertions() {
        var other = new Action(EventId.hashOf("orders/place"), Json.Obj.of(Map.of()));
        assertEquals(Optional.empty(), Assertion.from(other));
    }

    @Test
    void malformedAssertionsAreRefused() {
        for (var args : new Json.Obj[] {
                Json.Obj.of(Map.of("subject", new Json.Str(P.value()))),
                Json.Obj.of(Map.of("identity", new Json.Int(1))),
                Json.Obj.of(Map.of("value", Json.Obj.of(Map.of(
                        "ref", new Json.Str(P.value()), "literal", new Json.Str("x"))))),
                Json.Obj.of(Map.of("value", Json.Obj.of(Map.of("literal", Json.Obj.of(Map.of())))))}) {
            assertThrows(IllegalArgumentException.class, () -> Assertion.from(new Action(Basic.ASSERT, args)),
                    args.canonical());
        }
    }

    @Test
    void retractionIsATupleWithTheRetractedAttribute() {
        assertEquals(Assertion.of(P, Basic.RETRACTED, new Assertion.Literal(new Json.Str("true"))),
                Assertion.retraction(P));
    }
}
