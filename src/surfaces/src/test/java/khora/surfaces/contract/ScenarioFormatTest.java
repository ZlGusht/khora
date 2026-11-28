package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.EventId;
import khora.events.Identity;
import khora.events.json.Json;
import khora.surfaces.Context;
import org.junit.jupiter.api.Test;

class ScenarioFormatTest {
    private static final EventId CREATE = EventId.hashOf("create");

    private static Bindings bound() {
        var bindings = new Bindings();
        bindings.bind("create", CREATE);
        return bindings;
    }

    @Test
    void refsResolveAgainstBoundSteps() {
        var bindings = bound();
        assertEquals(Identity.of(CREATE, 0), bindings.identity(Ref.tuple("create", 0), Optional.empty()));
        assertEquals(Identity.named("name"), bindings.identity(Ref.named("name"), Optional.empty()));
        assertEquals(Identity.local(1), bindings.identity(Ref.local(1), Optional.empty()));
    }

    @Test
    void anUnknownStepIsNamed() {
        var e = assertThrows(IllegalArgumentException.class, () -> bound().event("missing"));
        assertTrue(e.getMessage().contains("missing"));
    }

    @Test
    void assertSpecResolvesToTheAssertAction() {
        var spec = new ActionSpec.Assert(List.of(TupleSpec.entity(),
                TupleSpec.of(Ref.local(0), Ref.named("name"), ValueSpec.text("Kitchen"))));
        assertEquals(Assertion.action(Assertion.entity(), Assertion.of(Identity.local(0), Identity.named("name"),
                new Assertion.Literal(new Json.Str("Kitchen")))), spec.resolve(bound(), Optional.empty()));
    }

    @Test
    void readSpecResolvesToBasicRead() {
        assertEquals(Basic.read(Identity.of(CREATE, 0)),
                new ActionSpec.Read(Ref.tuple("create", 0)).resolve(bound(), Optional.empty()));
    }

    @Test
    void receivedRefsNeedAReceivedEvent() {
        assertThrows(IllegalArgumentException.class,
                () -> bound().identity(Ref.received(0), Optional.empty()));
    }

    @Test
    void unnamedEventsRenderAsEmitted() {
        var bindings = bound();
        var first = EventId.hashOf("first");
        var second = EventId.hashOf("second");
        bindings.emitted(first);
        bindings.emitted(second);
        assertEquals("create", bindings.render(new Identity(CREATE.value())));
        assertEquals("create/0", bindings.render(Identity.of(CREATE, 0)));
        assertEquals("emitted#1/0", bindings.render(Identity.of(first, 0)));
        assertEquals("emitted#2", bindings.render(new Identity(second.value())));
    }

    @Test
    void namedIdentitiesRenderByTheirName() {
        var bindings = bound();
        var name = Identity.named("name");
        assertEquals(name.value(), bindings.render(name), "not yet resolved");
        assertEquals(name, bindings.identity(Ref.named("name"), Optional.empty()));
        assertEquals("name", bindings.render(name));
        assertEquals("(create/0, name, \"Kitchen\")", GateContract.symbolic(Assertion.of(
                Identity.of(CREATE, 0), name, new Assertion.Literal(new Json.Str("Kitchen"))), bindings));
    }

    @Test
    void aRetractionSpecResolvesToTheRetraction() {
        assertEquals(Assertion.retraction(Identity.of(CREATE, 1)),
                TupleSpec.retraction(Ref.tuple("create", 1)).resolve(bound(), Optional.empty()));
    }

    @Test
    void theBuilderMakesTheRecords() {
        var anna = new Context("anna", Optional.empty(), "g", 1);
        var action = new ActionSpec.Assert(List.of(TupleSpec.entity()));
        var built = Scenario.named("s")
                .given(Step.named("create").by(anna).after("a").does(action))
                .when(new When.Attach(List.of("create")))
                .then(new Then.Logged());
        var step = new Step("create", anna, Optional.of("a"), action);
        assertEquals(new Scenario("s", List.of(step), new When.Attach(List.of("create")), new Then.Logged()),
                built);
    }
}
