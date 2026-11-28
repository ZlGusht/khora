package khora.query.neo4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.Emitted;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Event;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import org.junit.jupiter.api.Test;

/**
 * What the datastore keeps: entities with their attributes, and relationships between them. No
 * events and no statements.
 */
class GraphShapeTest {
    private static final Identity NAME = Identity.named("name");
    private static final Identity BUDGET = Identity.named("budget");
    private static final Identity PROJECT = Identity.named("project");

    @Test
    void anEntityIsANodeWithItsAttributesAndAReferenceIsARelationship() {
        var log = new InMemoryEventLog();
        var first = event(List.of(), Assertion.entity(), Assertion.of(Identity.local(0), NAME, text("Kitchen")),
                Assertion.of(Identity.local(0), BUDGET, new Literal(new Json.Int(300))));
        var kitchen = Identity.of(first.id(), 0);
        var second = event(List.of(first),
                Assertion.entity(), Assertion.of(Identity.local(0), NAME, text("Tile")),
                Assertion.of(Identity.local(0), PROJECT, new Ref(kitchen)));
        var tile = Identity.of(second.id(), 0);
        log.append(List.of(first, second));
        var store = Neo4jTestServer.empty();
        store.catchUp(log);

        var snapshot = store.snapshot();
        assertEquals(sorted(List.of(
                entity(kitchen, Map.of(NAME, "{\"literal\":\"Kitchen\"}", BUDGET, "{\"literal\":300}")),
                entity(tile, Map.of(NAME, "{\"literal\":\"Tile\"}")),
                "{\"attribute\":\"" + PROJECT.value() + "\",\"statement\":\"" + Identity.of(second.id(), 2).value()
                        + "\",\"target\":\"" + kitchen.value() + "\",\"thing\":\"" + tile.value() + "\",\"type\":\"relation\"}")),
                sorted(snapshot.rows()));
        assertEquals(Map.of("Entity", 2L, "Progress", 1L, "Thing", 2L, "relationships", 1L), store.shape());
    }

    @Test
    void aRetractionDeletesWhatItsTargetAsserted() {
        var log = new InMemoryEventLog();
        var first = event(List.of(), Assertion.entity(), Assertion.of(Identity.local(0), NAME, text("Kitchen")),
                Assertion.entity(), Assertion.of(Identity.local(2), PROJECT, new Ref(Identity.local(0))));
        var kitchen = Identity.of(first.id(), 0);
        var tile = Identity.of(first.id(), 2);
        var second = event(List.of(first), Assertion.retraction(Identity.of(first.id(), 1)),
                Assertion.retraction(Identity.of(first.id(), 3)), Assertion.retraction(tile));
        log.append(List.of(first, second));
        var store = Neo4jTestServer.empty();
        store.catchUp(log);

        assertEquals(sorted(List.of(entity(kitchen, Map.of()),
                "{\"attributes\":{},\"id\":\"" + tile.value() + "\",\"type\":\"thing\"}")),
                sorted(store.snapshot().rows()));
    }

    private static List<String> sorted(List<String> rows) {
        return rows.stream().sorted().toList();
    }

    private static String entity(Identity id, Map<Identity, String> attributes) {
        var members = new java.util.TreeMap<String, String>();
        attributes.forEach((k, v) -> members.put(k.value(), v));
        var body = new StringBuilder();
        members.forEach((k, v) -> body.append(body.isEmpty() ? "" : ",").append('"').append(k).append("\":[").append(v).append(']'));
        return "{\"attributes\":{" + body + "},\"id\":\"" + id.value() + "\",\"type\":\"entity\"}";
    }

    private static Event event(List<Event> earlier, Assertion... tuples) {
        return Event.create(Optional.empty(), 1000 + earlier.size(), Optional.empty(),
                new Emitted(GeneratedLog.BY, Assertion.action(tuples)));
    }

    private static Literal text(String s) {
        return new Literal(new Json.Str(s));
    }
}
