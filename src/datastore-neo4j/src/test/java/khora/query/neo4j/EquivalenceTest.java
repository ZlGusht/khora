package khora.query.neo4j;

import static khora.query.neo4j.GeneratedLog.NAME;
import static khora.query.neo4j.GeneratedLog.OWNER;
import static khora.query.neo4j.GeneratedLog.SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import khora.events.Assertion.Literal;
import khora.events.Frontier;
import khora.events.FrontierCheck;
import khora.events.Order;
import khora.events.json.Json;
import khora.query.Condition;
import khora.query.Condition.Op;
import khora.query.EventsDatastore;
import khora.query.Operator.Aggregate;
import khora.query.Operator.Filter;
import khora.query.Operator.Function;
import khora.query.Operator.Not;
import khora.query.Operator.Scan;
import khora.query.Query;
import khora.query.Query.Sort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The graph datastore gives the answers of {@link EventsDatastore}, the reference, on generated
 * logs with branches and retractions, at everything, at single events, at joined branches, and at
 * the empty frontier: materializing changes speed, never results (ADR-0XY decision 10).
 */
class EquivalenceTest {

    private static List<Query> queries() {
        var names = Scan.statements("t", NAME, "name");
        var sizes = Scan.statements("t", SIZE, "size");
        var owners = Scan.statements("t", OWNER, "o");
        var owned = new Filter(Scan.things("t"), new Condition.Has("t", OWNER));
        return List.of(
                Query.of(Scan.things("t")),
                Query.of(names),
                Query.of(sizes).with(Query.Semantics.MULTISET),
                Query.of(owners),
                Query.of(owned),
                Query.of(new Filter(sizes, new Condition.Or(List.of(
                        new Condition.Compare("size", Op.LT, new Literal(new Json.Int(0))),
                        new Condition.Compare("size", Op.GE, new Literal(new Json.Int(30))))))),
                Query.of(new Filter(owners, new Condition.Has("o", NAME))),
                Query.of(new Not(Scan.things("t"), owned)),
                Query.of(Aggregate.count(owners, List.of("o"), "n")),
                Query.of(Aggregate.of(sizes, List.of("t"), Function.SUM, "size", "s")).with(Query.Semantics.MULTISET),
                Query.of(Aggregate.of(names, List.of(), Function.MAX, "name", "m")),
                Query.of(names).returning("name").orderedBy(Sort.descending("name")).page(3, 5));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void answersAsTheReferenceDoes(long seed) {
        var generated = new GeneratedLog(seed, 120);
        var reference = new EventsDatastore(generated.log);
        var store = Neo4jTestServer.empty();
        store.catchUp(generated.log);

        var random = new Random(seed);
        var frontiers = new ArrayList<Frontier>();
        frontiers.add(Frontier.empty());
        for (int i = 0; i < 6; i++) {
            frontiers.add(GeneratedLog.at(generated.events.get(random.nextInt(generated.events.size()))));
        }
        int concurrent = 0;
        for (int i = 0; i < 200 && concurrent < 4; i++) {
            var a = generated.events.get(random.nextInt(generated.events.size()));
            var b = generated.events.get(random.nextInt(generated.events.size()));
            if (FrontierCheck.compare(GeneratedLog.at(a), GeneratedLog.at(b), generated.log) == Order.CONCURRENT) {
                frontiers.add(GeneratedLog.at(a, b));
                frontiers.add(GeneratedLog.at(a));
                concurrent++;
            }
        }
        assertTrue(concurrent > 0, "the generated log has branches");

        for (var query : queries()) {
            assertEquals(reference.answer(query), store.answer(query), query.toJson().canonical());
            for (var at : frontiers) {
                var q = query.at(at);
                assertEquals(reference.answer(q), store.answer(q), q.toJson().canonical());
            }
        }
    }
}
