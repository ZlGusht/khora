package khora.query.neo4j;

import static khora.query.neo4j.GeneratedLog.NAME;
import static khora.query.neo4j.GeneratedLog.OWNER;
import static khora.query.neo4j.GeneratedLog.SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Value;
import khora.events.json.Json;
import khora.query.Answer;
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
import khora.query.Values;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The graph datastore gives the answers of {@link EventsDatastore}, the reference, on generated
 * logs with branches and retractions, each time it has caught up to a point of the log:
 * materializing changes speed, never results (ADR-0XY decision 10). Rows are compared in any
 * order unless the query orders them.
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
                Query.of(owners).with(Query.Semantics.MULTISET),
                Query.of(owned),
                Query.of(new Filter(sizes, new Condition.Or(List.of(
                        new Condition.Compare("size", Op.LT, new Literal(new Json.Int(0))),
                        new Condition.Compare("size", Op.GE, new Literal(new Json.Int(300))))))),
                Query.of(new Filter(owners, new Condition.Has("o", NAME))),
                Query.of(new Not(Scan.things("t"), owned)),
                Query.of(Aggregate.count(owners, List.of("o"), "n")).with(Query.Semantics.MULTISET),
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
        for (int upTo : List.of(1, 20, 60, 120)) {
            store.take(generated.log.read(store.position() + 1, (int) (upTo - store.position())), generated.log);
            for (var query : queries()) {
                var expected = reference.answer(query.at(store.at()));
                var actual = store.answer(query);
                var what = upTo + " " + query.toJson().canonical();
                if (query.order().isEmpty()) {
                    assertEquals(sorted(expected), sorted(actual), what);
                } else {
                    assertEquals(expected, actual, what);
                }
            }
        }
    }

    /** {@code a} with its rows sorted as values sort. */
    static Answer sorted(Answer a) {
        Comparator<List<Value>> c = (x, y) -> 0;
        for (int i = 0; i < a.columns().size(); i++) {
            int column = i;
            c = c.thenComparing(r -> r.get(column), Values.ORDER);
        }
        return new Answer(a.at(), a.columns(), a.rows().stream().sorted(c).toList());
    }
}
