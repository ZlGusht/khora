package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Action;
import khora.kernel.event.Assertion;
import khora.kernel.event.Basic;
import khora.kernel.event.By;
import khora.kernel.event.Event;
import khora.kernel.event.Identity;
import khora.kernel.json.Json;
import org.junit.jupiter.api.Test;

class InterestTest {

    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final Identity P = Identity.named("p");
    static final Identity NAME = Identity.named("name");
    static final Identity OWNS = Identity.named("owns");

    static Event event(Action action) {
        return Event.create(List.of(), 1000, Optional.empty(), new AcceptedAction(BY, action));
    }

    @Test
    void anyInterestMatchesEveryAcceptedAction() {
        assertTrue(Interest.any().matches(event(Assertion.action(Assertion.entity()))));
    }

    @Test
    void matchesByDefinition() {
        var e = event(Basic.read(P));
        assertTrue(new Interest(Set.of(Basic.READ), Set.of()).matches(e));
        assertFalse(new Interest(Set.of(Basic.ASSERT), Set.of()).matches(e));
    }

    @Test
    void matchesWhatTuplesAreAbout() {
        var named = event(Assertion.action(Assertion.of(P, NAME, new Assertion.Literal(new Json.Str("Kitchen")))));
        assertTrue(Interest.about(P).matches(named));
        assertFalse(Interest.about(OWNS).matches(named));
        var owner = Identity.named("anna");
        var owns = event(Assertion.action(Assertion.of(owner, OWNS, new Assertion.Ref(P))));
        assertTrue(Interest.about(P).matches(owns));
    }

    @Test
    void aNewThingIsAboutItself() {
        var created = event(Assertion.action(Assertion.entity()));
        assertTrue(Interest.about(Identity.of(created.id(), 0)).matches(created));
    }

    @Test
    void matchesWhatAReadRead() {
        assertTrue(Interest.about(P).matches(event(Basic.read(P))));
        assertFalse(Interest.about(NAME).matches(event(Basic.read(P))));
    }

    @Test
    void readsNameExactlyOneThing() {
        org.junit.jupiter.api.Assertions.assertEquals(Optional.of(P), Basic.readOf(Basic.read(P)));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Action(Basic.READ, Json.Obj.of(java.util.Map.of())));
    }
}
