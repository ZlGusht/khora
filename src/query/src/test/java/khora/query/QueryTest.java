package khora.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;
import khora.query.Condition.Op;
import khora.query.Operator.Aggregate;
import khora.query.Operator.Filter;
import khora.query.Operator.Function;
import khora.query.Operator.Not;
import khora.query.Operator.Scan;
import khora.query.Query.Sort;
import org.junit.jupiter.api.Test;

class QueryTest {
    static final Identity NAME = Identity.named("name");
    static final Identity PROJECT = Identity.named("project");
    static final Identity THING = new Identity(EventId.hashOf("x").value() + "/0");

    /** A query that uses every operator, condition and envelope member. */
    static Query everything() {
        var tasks = Scan.statements("t", PROJECT, "p");
        var named = new Filter(tasks, new Condition.Or(List.of(
                new Condition.Has("t", NAME),
                new Condition.And(List.of(new Condition.Has("t", NAME, new Literal(new Json.Str("Tile"))),
                        new Condition.Compare("p", Op.NE, new Ref(THING)))))));
        var open = new Not(named, new Filter(Scan.things("t"), new Condition.Compare("t", Op.EQ, new Ref(THING))));
        var heads = new TreeSet<EventId>();
        heads.add(EventId.hashOf("e"));
        return Query.of(Aggregate.count(open, List.of("p"), "n"))
                .at(new Frontier(heads))
                .with(Query.Semantics.MULTISET)
                .returning("n", "p")
                .orderedBy(Sort.descending("n"), Sort.ascending("p"))
                .page(10, 5);
    }

    // Validation

    @Test
    void aFilterOnAColumnItsInputLacksIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Filter(Scan.things("t"), new Condition.Has("p", NAME)));
    }

    @Test
    void aNotWhoseMinusHasOtherColumnsIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Not(Scan.things("t"), Scan.statements("t", NAME, "name")));
    }

    @Test
    void aScanNamingAColumnTwiceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Scan.statements("t", NAME, "t"));
        assertThrows(IllegalArgumentException.class, () -> new Scan("t", Optional.of(NAME), Optional.empty()));
    }

    @Test
    void badColumnNamesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Scan.things("T"));
        assertThrows(IllegalArgumentException.class, () -> Scan.things(""));
        assertThrows(IllegalArgumentException.class, () -> Scan.things("a".repeat(65)));
        assertEquals(List.of("a".repeat(64)), Scan.things("a".repeat(64)).columns());
    }

    @Test
    void anAggregateIsChecked() {
        var tasks = Scan.statements("t", PROJECT, "p");
        assertThrows(IllegalArgumentException.class, () -> Aggregate.count(tasks, List.of("x"), "n"));
        assertThrows(IllegalArgumentException.class, () -> Aggregate.count(tasks, List.of("p", "p"), "n"));
        assertThrows(IllegalArgumentException.class, () -> Aggregate.count(tasks, List.of("p"), "p"));
        assertThrows(IllegalArgumentException.class,
                () -> new Aggregate(tasks, List.of(), Function.SUM, Optional.empty(), "s"));
        assertThrows(IllegalArgumentException.class,
                () -> new Aggregate(tasks, List.of(), Function.COUNT, Optional.of("t"), "s"));
        assertThrows(IllegalArgumentException.class, () -> Aggregate.of(tasks, List.of(), Function.MAX, "x", "m"));
        assertEquals(List.of("p", "n"), Aggregate.count(tasks, List.of("p"), "n").columns());
    }

    @Test
    void emptyAndOrAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Condition.And(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Condition.Or(List.of()));
    }

    @Test
    void theEnvelopeIsChecked() {
        var q = Query.of(Scan.statements("t", NAME, "name"));
        assertThrows(IllegalArgumentException.class, () -> q.returning("x"));
        assertThrows(IllegalArgumentException.class, () -> q.returning());
        assertThrows(IllegalArgumentException.class, () -> q.returning("t", "t"));
        assertThrows(IllegalArgumentException.class, () -> q.returning("t").orderedBy(Sort.ascending("name")));
        assertThrows(IllegalArgumentException.class, () -> q.orderedBy(Sort.ascending("t"), Sort.descending("t")));
        assertThrows(IllegalArgumentException.class, () -> q.page(10, 0));
        assertThrows(IllegalArgumentException.class, () -> q.orderedBy(Sort.ascending("t")).page(0, 0));
        assertThrows(IllegalArgumentException.class, () -> q.orderedBy(Sort.ascending("t")).page(1, -1));
    }

    // JSON

    @Test
    void aQueryRoundTripsThroughCanonicalJson() {
        var q = everything();
        assertEquals(q, Query.fromJson(Json.parse(q.toJson().canonical())));
        assertEquals(q.toJson().canonical(), Query.fromJson(q.toJson()).toJson().canonical());
    }

    @Test
    void theJsonIsReadable() {
        var q = Query.of(new Filter(Scan.things("t"), new Condition.Has("t", NAME)));
        assertEquals("{\"body\":{\"filter\":{\"input\":{\"scan\":{\"as\":\"t\"}},\"where\":{\"has\":{\"attribute\":\""
                + NAME.value() + "\",\"column\":\"t\"}}}},\"return\":[\"t\"],\"semantics\":\"set\"}",
                q.toJson().canonical());
    }

    @Test
    void defaultsAreFilledWhenReading() {
        var q = Query.fromJson(Json.parse("{\"body\":{\"scan\":{\"as\":\"t\"}},\"return\":[\"t\"]}"));
        assertEquals(Query.of(Scan.things("t")), q);
        var paged = Query.fromJson(Json.parse("{\"body\":{\"scan\":{\"as\":\"t\"}},\"return\":[\"t\"],"
                + "\"order\":[{\"column\":\"t\",\"direction\":\"asc\"}],\"page\":{\"limit\":3}}"));
        assertEquals(Optional.of(new Query.Page(3, 0)), paged.page());
    }

    @Test
    void malformedJsonIsRefused() {
        var scan = "{\"scan\":{\"as\":\"t\"}}";
        for (var text : List.of(
                "[]",
                "{\"return\":[\"t\"]}",
                "{\"body\":" + scan + "}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"extra\":1}",
                "{\"body\":{\"scan\":{\"as\":\"t\"},\"not\":{}},\"return\":[\"t\"]}",
                "{\"body\":{\"join\":{}},\"return\":[\"t\"]}",
                "{\"body\":{\"scan\":{\"as\":\"t\",\"colour\":\"red\"}},\"return\":[\"t\"]}",
                "{\"body\":{\"scan\":{\"as\":1}},\"return\":[\"t\"]}",
                "{\"body\":" + scan + ",\"return\":[\"x\"]}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"semantics\":\"bag\"}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"semantics\":\"SET\"}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"order\":[{\"column\":\"t\",\"direction\":\"up\"}]}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"page\":{\"limit\":3}}",
                "{\"body\":" + scan + ",\"return\":[\"t\"],\"at\":[\"e1\"]}",
                "{\"body\":{\"filter\":{\"input\":" + scan + ",\"where\":{\"not\":[]}}},\"return\":[\"t\"]}",
                "{\"body\":{\"filter\":{\"input\":" + scan + ",\"where\":{\"and\":[]}}},\"return\":[\"t\"]}",
                "{\"body\":{\"filter\":{\"input\":" + scan + ",\"where\":{\"compare\":{\"column\":\"t\",\"op\":\"like\","
                        + "\"value\":{\"literal\":\"a\"}}}}},\"return\":[\"t\"]}",
                "{\"body\":{\"filter\":{\"input\":" + scan + ",\"where\":{\"has\":{\"column\":\"t\",\"attribute\":\""
                        + NAME.value() + "\",\"value\":{\"literal\":\"a\",\"ref\":\"x\"}}}}},\"return\":[\"t\"]}",
                "{\"body\":{\"aggregate\":{\"input\":" + scan + ",\"group\":[],\"function\":\"avg\",\"of\":\"t\","
                        + "\"as\":\"n\"}},\"return\":[\"n\"]}")) {
            assertThrows(IllegalArgumentException.class, () -> Query.fromJson(Json.parse(text)), text);
        }
    }

    // Storing

    @Test
    void aStoredQueryIsAStringLiteralOfItsCanonicalJson() {
        var q = everything();
        var tuple = q.stored(THING);
        assertEquals(Assertion.of(THING, Query.ATTRIBUTE, new Literal(new Json.Str(q.toJson().canonical()))), tuple);
        assertEquals(q, Query.from(new Statement(new Identity(EventId.hashOf("s").value() + "/1"), tuple)));
    }

    @Test
    void readingAQueryFromAnotherStatementIsRefused() {
        var id = new Identity(EventId.hashOf("s").value() + "/1");
        assertThrows(IllegalArgumentException.class,
                () -> Query.from(new Statement(id, Assertion.of(THING, NAME, new Literal(new Json.Str("{}"))))));
        assertThrows(IllegalArgumentException.class,
                () -> Query.from(new Statement(id, Assertion.of(THING, Query.ATTRIBUTE, new Literal(new Json.Int(1))))));
        assertThrows(IllegalArgumentException.class,
                () -> Query.from(new Statement(id, Assertion.of(THING, Query.ATTRIBUTE, new Literal(new Json.Str("{}"))))));
    }
}
