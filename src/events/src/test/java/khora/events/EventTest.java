package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventTest {

    static final String SALT = "00".repeat(16);
    static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    static final EventId A = EventId.hashOf("a");
    static final EventId B = EventId.hashOf("b");

    static Event event(Optional<EventId> trigger, String salt) {
        return Event.create(trigger, 1000, Optional.empty(), new Emitted(BY, Assertion.action(Assertion.entity())), salt);
    }

    @Test
    void eventRoundTrips() {
        var e = Event.create(Optional.of(A), Hlc.pack(1000, 0), Optional.of(900L),
                new Emitted(new By("anna", Optional.of("ivan"), "g3", "projects"), Assertion.action(Assertion.entity())), SALT);
        var back = EventJson.read(EventJson.write(e));
        assertEquals(e, back);
        assertEquals(e.id(), back.id());
    }

    @Test
    void idIsSha256OfCanonicalJsonWithoutId() {
        var e = event(Optional.of(A), SALT);
        assertEquals(EventId.hashOf(EventJson.toJson(e, false).canonical()), e.id());
        assertTrue(e.id().value().matches("sha256:[0-9a-f]{64}"));
        assertFalse(EventJson.toJson(e, false).get("id").isPresent());
    }

    @Test
    void tamperedEventIsRefused() {
        var text = EventJson.write(event(Optional.empty(), SALT));
        var tampered = text.replace("\"recorded\":1000", "\"recorded\":1001");
        assertNotEquals(text, tampered);
        assertThrows(IllegalArgumentException.class, () -> EventJson.read(tampered));
    }

    @Test
    void sameContentGetsDifferentIdsWithDifferentSalts() {
        var one = Event.create(Optional.empty(), 1000, Optional.empty(), new Emitted(BY, Assertion.action(Assertion.entity())));
        var two = Event.create(Optional.empty(), 1000, Optional.empty(), new Emitted(BY, Assertion.action(Assertion.entity())));
        assertNotEquals(one.id(), two.id());
        assertTrue(one.salt().matches("[0-9a-f]{32}"));
    }

    @Test
    void anEventWithoutATriggerOmitsIt() {
        var e = event(Optional.empty(), SALT);
        assertEquals(e, EventJson.read(EventJson.write(e)));
        assertFalse(EventJson.write(e).contains("trigger"));
    }

    @Test
    void absentOptionalsAreOmitted() {
        var text = EventJson.write(event(Optional.empty(), SALT));
        assertFalse(text.contains("occurred"));
        assertFalse(text.contains("on_behalf_of"));
    }

    @Test
    void writtenJsonHasThePinnedShape() {
        var e = event(Optional.of(A), SALT);
        var expected = "{\"body\":{\"emitted\":{\"action\":{\"args\":{\"tuples\":[{}]},"
                + "\"definition\":\"" + Basic.ASSERT.value() + "\"},"
                + "\"by\":{\"grant\":\"g3\",\"principal\":\"anna\",\"via\":\"projects\"}}},"
                + "\"id\":\"" + e.id().value() + "\","
                + "\"recorded\":1000,\"salt\":\"" + SALT + "\",\"trigger\":\"" + A.value() + "\"}";
        assertEquals(expected, EventJson.write(e));
    }

    @Test
    void malformedSaltIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> event(Optional.empty(), "xyz"));
    }

    @Test
    void unknownMembersAreRefusedAtEveryLevel() {
        var text = EventJson.write(Event.create(Optional.of(A), 1000, Optional.empty(), new Emitted(BY,
                Assertion.action(Assertion.entity())), SALT));
        for (var tampered : List.of(
                text.replaceFirst("^\\{", "{\"evil\":\"x\","),
                text.replace("{\"emitted\":{", "{\"emitted\":{\"evil\":\"x\","),
                text.replace("{\"action\":{", "{\"action\":{\"evil\":\"x\","),
                text.replace("\"by\":{", "\"by\":{\"evil\":\"x\","))) {
            org.junit.jupiter.api.Assertions.assertNotEquals(text, tampered);
            assertThrows(IllegalArgumentException.class, () -> EventJson.read(tampered), tampered);
        }
    }

    @Test
    void theTriggerIsCoveredByTheId() {
        assertNotEquals(event(Optional.of(A), SALT).id(), event(Optional.of(B), SALT).id());
        assertNotEquals(event(Optional.of(A), SALT).id(), event(Optional.empty(), SALT).id());
    }

    @Test
    void timesOutOfRangeAreRefused() {
        var body = new Emitted(BY, Assertion.action(Assertion.entity()));
        assertThrows(IllegalArgumentException.class, () -> Event.create(Optional.empty(), -1, Optional.empty(), body, SALT));
        assertThrows(IllegalArgumentException.class,
                () -> Event.create(Optional.empty(), Hlc.pack(1000, 0), Optional.of(-1L), body, SALT));
        assertThrows(IllegalArgumentException.class,
                () -> Event.create(Optional.empty(), Hlc.pack(1000, 0), Optional.of(1001L), body, SALT));
        Event.create(Optional.empty(), Hlc.pack(1000, 0), Optional.of(1000L), body, SALT);
    }

    @Test
    void onlyTheCanonicalTextIsRead() {
        var text = EventJson.write(event(Optional.of(A), SALT));
        for (var other : List.of(
                " " + text,
                text.replace("\"trigger\":\"" + A.value() + "\"", "\"trigger\" :\"" + A.value() + "\""),
                text.replace("\"principal\":\"anna\"", "\"principal\":\"\\u0061nna\""))) {
            assertNotEquals(text, other);
            assertThrows(IllegalArgumentException.class, () -> EventJson.read(other), other);
        }
        assertEquals(text, EventJson.write(EventJson.read(text)));
    }

}
