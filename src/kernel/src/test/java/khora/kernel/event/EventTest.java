package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class EventTest {

    static final String SALT = "00".repeat(16);
    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final EventId A = EventId.hashOf("a");
    static final EventId B = EventId.hashOf("b");

    static Event event(List<EventId> causes, String salt) {
        return Event.create(causes, 1000, Optional.empty(), new AcceptedAction(BY, Assertion.action(Assertion.entity())), salt);
    }

    @Test
    void eventRoundTrips() {
        var e = Event.create(List.of(A, B), 1000, Optional.of(900L),
                new AcceptedAction(new By("anna", Optional.of("ivan"), "g3", "projects"), Assertion.action(Assertion.entity())), SALT);
        var back = EventJson.read(EventJson.write(e));
        assertEquals(e, back);
        assertEquals(e.id(), back.id());
    }

    @Test
    void idIsSha256OfCanonicalJsonWithoutId() {
        var e = event(List.of(A), SALT);
        assertEquals(EventId.hashOf(EventJson.toJson(e, false).canonical()), e.id());
        assertTrue(e.id().value().matches("sha256:[0-9a-f]{64}"));
        assertFalse(EventJson.toJson(e, false).get("id").isPresent());
    }

    @Test
    void tamperedEventIsRefused() {
        var text = EventJson.write(event(List.of(), SALT));
        var tampered = text.replace("\"recorded\":1000", "\"recorded\":1001");
        assertNotEquals(text, tampered);
        assertThrows(IllegalArgumentException.class, () -> EventJson.read(tampered));
    }

    @Test
    void sameContentGetsDifferentIdsWithDifferentSalts() {
        var one = Event.create(List.of(), 1000, Optional.empty(), new AcceptedAction(BY, Assertion.action(Assertion.entity())));
        var two = Event.create(List.of(), 1000, Optional.empty(), new AcceptedAction(BY, Assertion.action(Assertion.entity())));
        assertNotEquals(one.id(), two.id());
        assertTrue(one.salt().matches("[0-9a-f]{32}"));
    }

    @Test
    void causesOrderDoesNotMatter() {
        assertEquals(event(List.of(B, A), SALT).id(), event(List.of(A, B), SALT).id());
        assertEquals(event(List.of(A, B), SALT).id(), event(List.of(A, B, A), SALT).id());
    }

    @Test
    void firstEventHasNoCauses() {
        var e = event(List.of(), SALT);
        assertEquals(e, EventJson.read(EventJson.write(e)));
        assertTrue(EventJson.write(e).contains("\"causes\":[]"));
    }

    @Test
    void absentOptionalsAreOmitted() {
        var text = EventJson.write(event(List.of(), SALT));
        assertFalse(text.contains("occurred"));
        assertFalse(text.contains("on_behalf_of"));
    }

    @Test
    void writtenJsonHasThePinnedShape() {
        var e = event(List.of(A), SALT);
        var expected = "{\"body\":{\"accepted_action\":{\"action\":{\"args\":{\"tuples\":[{}]},"
                + "\"definition\":\"" + Basic.ASSERT.value() + "\"},"
                + "\"by\":{\"grant\":\"g3\",\"principal\":\"anna\",\"via\":\"projects\"}}},"
                + "\"causes\":[\"" + A.value() + "\"],\"id\":\"" + e.id().value() + "\","
                + "\"recorded\":1000,\"salt\":\"" + SALT + "\"}";
        assertEquals(expected, EventJson.write(e));
    }

    @Test
    void malformedSaltIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> event(List.of(), "xyz"));
    }

    @Test
    void causesIgnoreTheCallersComparator() {
        var e = event(List.of(A, B), SALT);
        var reversed = new TreeSet<EventId>(Comparator.reverseOrder());
        reversed.addAll(List.of(A, B));
        var same = new Event(e.id(), reversed, e.recorded(), e.occurred(), e.salt(), e.body());
        assertEquals(e, same);
        assertEquals(null, same.causes().comparator());
    }

    @Test
    void unknownMembersAreRefusedAtEveryLevel() {
        var text = EventJson.write(Event.create(List.of(A), 1000, Optional.empty(), new AcceptedAction(BY,
                Assertion.action(Assertion.entity())), SALT));
        for (var tampered : List.of(
                text.replaceFirst("^\\{", "{\"evil\":\"x\","),
                text.replace("{\"accepted_action\":{", "{\"accepted_action\":{\"evil\":\"x\","),
                text.replace("{\"action\":{", "{\"action\":{\"evil\":\"x\","),
                text.replace("\"by\":{", "\"by\":{\"evil\":\"x\","))) {
            org.junit.jupiter.api.Assertions.assertNotEquals(text, tampered);
            assertThrows(IllegalArgumentException.class, () -> EventJson.read(tampered), tampered);
        }
    }
}
