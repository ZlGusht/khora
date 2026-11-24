package khora.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import khora.events.AcceptedAction;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Assertion.Value;
import khora.events.By;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.Statements;
import khora.events.json.Json;
import khora.events.log.EventLog;
import khora.events.log.InMemoryEventLog;
import khora.query.Condition.Op;
import khora.query.Operator.Aggregate;
import khora.query.Operator.Filter;
import khora.query.Operator.Function;
import khora.query.Operator.Not;
import khora.query.Operator.Scan;
import khora.query.Query.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tests every {@link Datastore} passes: each operator, the envelope, frontiers, and stored
 * queries. A datastore's test extends this and says how to build the datastore over a log that
 * already holds the events.
 *
 * <p>The content: two projects, Kitchen (budget 300) and Studio (budget 120), and three tasks, two
 * in Kitchen and one in Studio, Paint done. A third project, Garage, was created and then its creation
 * retracted; its name still stands.
 */
public abstract class DatastoreContract {
    protected static final By BY = new By("anna", Optional.empty(), "g3", "projects");
    protected static final Identity NAME = Identity.named("name");
    protected static final Identity BUDGET = Identity.named("budget");
    protected static final Identity PROJECT = Identity.named("project");
    protected static final Identity DONE = Identity.named("done");

    /** A datastore over {@code log}, which already holds the events it should answer from. */
    protected abstract Datastore over(EventLog log);

    protected EventLog log;
    protected Event first;
    protected Event second;
    protected Identity kitchen;
    protected Identity studio;
    protected Identity garage;
    protected Identity tile;
    protected Identity paint;
    protected Identity desk;

    @BeforeEach
    void content() {
        log = new InMemoryEventLog();
        first = event(1000, List.of(),
                Assertion.entity(), named(0, "Kitchen"), Assertion.of(Identity.local(0), BUDGET, integer(300)),
                Assertion.entity(), named(3, "Studio"), Assertion.of(Identity.local(3), BUDGET, integer(120)),
                Assertion.entity(), named(6, "Garage"));
        kitchen = Identity.of(first.id(), 0);
        studio = Identity.of(first.id(), 3);
        garage = Identity.of(first.id(), 6);
        second = event(1001, List.of(first.id()),
                Assertion.retraction(garage),
                Assertion.entity(), named(1, "Tile"), Assertion.of(Identity.local(1), PROJECT, new Ref(kitchen)),
                Assertion.entity(), named(4, "Paint"), Assertion.of(Identity.local(4), PROJECT, new Ref(kitchen)),
                Assertion.of(Identity.local(4), DONE, text("yes")),
                Assertion.entity(), named(8, "Desk"), Assertion.of(Identity.local(8), PROJECT, new Ref(studio)));
        tile = Identity.of(second.id(), 1);
        paint = Identity.of(second.id(), 4);
        desk = Identity.of(second.id(), 8);
        log.append(List.of(first));
        log.append(List.of(second));
    }

    protected static Event event(long recorded, List<EventId> causes, Assertion... tuples) {
        return Event.create(causes, recorded, Optional.empty(), new AcceptedAction(BY, Assertion.action(tuples)));
    }

    protected static Assertion named(int local, String name) {
        return Assertion.of(Identity.local(local), NAME, text(name));
    }

    protected static Value text(String s) {
        return new Literal(new Json.Str(s));
    }

    protected static Value integer(long n) {
        return new Literal(new Json.Int(n));
    }

    protected static Value ref(Identity id) {
        return new Ref(id);
    }

    protected static Frontier at(Event... events) {
        var ids = new TreeSet<EventId>();
        Arrays.stream(events).forEach(e -> ids.add(e.id()));
        return new Frontier(ids);
    }

    private List<List<Value>> rows(Query query) {
        return over(log).answer(query).rows();
    }

    /** {@code rows}, sorted as values sort, so tests do not depend on a datastore's order. */
    private static List<List<Value>> sorted(List<List<Value>> rows) {
        Comparator<List<Value>> c = (a, b) -> 0;
        for (int i = 0; i < (rows.isEmpty() ? 0 : rows.get(0).size()); i++) {
            int column = i;
            c = c.thenComparing(r -> r.get(column), Values.ORDER);
        }
        return rows.stream().sorted(c).toList();
    }

    private List<List<Value>> unordered(Query query) {
        return sorted(rows(query));
    }

    private static List<Value> row(Value... values) {
        return List.of(values);
    }

    @SafeVarargs
    private static List<List<Value>> expect(List<Value>... rows) {
        var list = new ArrayList<List<Value>>();
        for (var r : rows) {
            list.add(r);
        }
        return sorted(list);
    }

    private static Scan names() {
        return Scan.statements("t", NAME, "name");
    }

    private static Scan tasks() {
        return Scan.statements("t", PROJECT, "p");
    }

    // Scan

    @Test
    void scanGivesEveryThingThatHolds() {
        assertEquals(expect(row(ref(kitchen)), row(ref(studio)), row(ref(tile)), row(ref(paint)), row(ref(desk))),
                unordered(Query.of(Scan.things("t"))));
    }

    @Test
    void scanOfAnAttributeGivesEachStatementsThingAndValue() {
        assertEquals(expect(row(ref(tile), ref(kitchen)), row(ref(paint), ref(kitchen)), row(ref(desk), ref(studio))),
                unordered(Query.of(tasks())));
    }

    @Test
    void retractedStatementsDoNotShow() {
        var paintName = Identity.of(second.id(), 5);
        log.append(List.of(event(1002, List.of(second.id()), Assertion.retraction(paintName))));
        assertEquals(expect(row(ref(kitchen), text("Kitchen")), row(ref(studio), text("Studio")),
                row(ref(garage), text("Garage")), row(ref(tile), text("Tile")), row(ref(desk), text("Desk"))),
                unordered(Query.of(names())));
    }

    @Test
    void statementsAboutARetractedThingStillShow() {
        assertEquals(expect(row(ref(garage))), unordered(Query.of(
                new Filter(names(), new Condition.Compare("name", Op.EQ, text("Garage")))).returning("t")));
    }

    @Test
    void scanOfAnUnusedAttributeIsEmpty() {
        assertEquals(List.of(), rows(Query.of(Scan.statements("t", Identity.named("colour"), "c"))));
    }

    // Filter

    @Test
    void hasKeepsThingsWithTheAttribute() {
        assertEquals(expect(row(ref(kitchen)), row(ref(studio))),
                unordered(Query.of(new Filter(Scan.things("t"), new Condition.Has("t", BUDGET)))));
    }

    @Test
    void hasWithAValueKeepsThingsWithThatValue() {
        assertEquals(expect(row(ref(tile)), row(ref(paint))), unordered(Query.of(
                new Filter(Scan.things("t"), new Condition.Has("t", PROJECT, ref(kitchen))))));
    }

    @Test
    void hasOnALiteralColumnHoldsForNothing() {
        assertEquals(List.of(), rows(Query.of(new Filter(names(), new Condition.Has("name", NAME)))));
    }

    @Test
    void compareOrdersIntegersAndStrings() {
        var budgets = Scan.statements("t", BUDGET, "b");
        assertEquals(expect(row(ref(studio), integer(120))), unordered(Query.of(
                new Filter(budgets, new Condition.Compare("b", Op.LT, integer(300))))));
        assertEquals(expect(row(ref(kitchen), integer(300)), row(ref(studio), integer(120))), unordered(Query.of(
                new Filter(budgets, new Condition.Compare("b", Op.GE, integer(120))))));
        assertEquals(expect(row(ref(paint), text("Paint")), row(ref(studio), text("Studio")),
                row(ref(tile), text("Tile"))), unordered(Query.of(
                        new Filter(names(), new Condition.Compare("name", Op.GT, text("Kitchen"))))));
    }

    @Test
    void compareAcrossKindsOfValueIsFalseExceptNe() {
        var budgets = Scan.statements("t", BUDGET, "b");
        assertEquals(List.of(), rows(Query.of(new Filter(budgets, new Condition.Compare("b", Op.LT, text("z"))))));
        assertEquals(List.of(), rows(Query.of(new Filter(budgets, new Condition.Compare("b", Op.EQ, text("300"))))));
        assertEquals(2, rows(Query.of(new Filter(budgets, new Condition.Compare("b", Op.NE, text("300"))))).size());
        assertEquals(List.of(), rows(Query.of(new Filter(tasks(), new Condition.Compare("p", Op.LT, ref(studio))))));
    }

    @Test
    void andAndOrCombineConditions() {
        var kitchenTask = new Condition.Compare("p", Op.EQ, ref(kitchen));
        var done = new Condition.Has("t", DONE);
        assertEquals(expect(row(ref(paint), ref(kitchen))),
                unordered(Query.of(new Filter(tasks(), new Condition.And(List.of(kitchenTask, done))))));
        assertEquals(expect(row(ref(tile), ref(kitchen)), row(ref(paint), ref(kitchen))),
                unordered(Query.of(new Filter(tasks(), new Condition.Or(List.of(kitchenTask, done))))));
    }

    // Not

    @Test
    void notRemovesRowsThatMatchOnTheMinusColumns() {
        var done = new Filter(Scan.things("t"), new Condition.Has("t", DONE));
        assertEquals(expect(row(ref(tile), ref(kitchen)), row(ref(desk), ref(studio))),
                unordered(Query.of(new Not(tasks(), done))));
    }

    @Test
    void notOfNothingKeepsEverything() {
        var none = new Filter(Scan.things("t"), new Condition.Has("t", Identity.named("colour")));
        assertEquals(3, rows(Query.of(new Not(tasks(), none))).size());
    }

    // Aggregate

    @Test
    void countByGroup() {
        assertEquals(expect(row(ref(kitchen), integer(2)), row(ref(studio), integer(1))),
                unordered(Query.of(Aggregate.count(tasks(), List.of("p"), "n"))));
    }

    @Test
    void countOfNothingIsZero() {
        var none = Scan.statements("t", Identity.named("colour"), "c");
        assertEquals(List.of(row(integer(0))), rows(Query.of(Aggregate.count(none, List.of(), "n"))));
        assertEquals(List.of(), rows(Query.of(Aggregate.count(none, List.of("t"), "n"))));
        assertEquals(List.of(row(integer(0))),
                rows(Query.of(Aggregate.of(none, List.of(), Function.SUM, "c", "s"))));
        assertEquals(List.of(), rows(Query.of(Aggregate.of(none, List.of(), Function.MAX, "c", "m"))));
    }

    @Test
    void sumMinAndMax() {
        var budgets = Scan.statements("t", BUDGET, "b");
        assertEquals(List.of(row(integer(420))), rows(Query.of(Aggregate.of(budgets, List.of(), Function.SUM, "b", "s"))));
        assertEquals(List.of(row(integer(120))), rows(Query.of(Aggregate.of(budgets, List.of(), Function.MIN, "b", "m"))));
        assertEquals(List.of(row(integer(300))), rows(Query.of(Aggregate.of(budgets, List.of(), Function.MAX, "b", "m"))));
        assertEquals(List.of(row(text("Tile"))), rows(Query.of(Aggregate.of(names(), List.of(), Function.MAX, "name", "m"))));
    }

    @Test
    void sumSkipsWhatIsNotAnInteger() {
        assertEquals(List.of(row(integer(0))), rows(Query.of(Aggregate.of(names(), List.of(), Function.SUM, "name", "s"))));
    }

    @Test
    void sumBeyondALongIsRefused() {
        var big = event(1002, List.of(second.id()), Assertion.of(tile, BUDGET, integer(Long.MAX_VALUE)));
        log.append(List.of(big));
        var sum = Query.of(Aggregate.of(Scan.statements("t", BUDGET, "b"), List.of(), Function.SUM, "b", "s"));
        assertThrows(ArithmeticException.class, () -> over(log).answer(sum));
    }

    // Envelope

    @Test
    void returnProjectsAndSetSemanticsDropsRepeats() {
        assertEquals(expect(row(ref(kitchen)), row(ref(studio))), unordered(Query.of(tasks()).returning("p")));
        assertEquals(expect(row(ref(kitchen)), row(ref(kitchen)), row(ref(studio))),
                unordered(Query.of(tasks()).returning("p").with(Query.Semantics.MULTISET)));
    }

    @Test
    void setSemanticsAppliesBeforeAnAggregateCounts() {
        var again = event(1002, List.of(second.id()), Assertion.of(tile, PROJECT, ref(kitchen)));
        log.append(List.of(again));
        var count = Query.of(Aggregate.count(tasks(), List.of("p"), "n"));
        assertEquals(expect(row(ref(kitchen), integer(2)), row(ref(studio), integer(1))), unordered(count));
        assertEquals(expect(row(ref(kitchen), integer(3)), row(ref(studio), integer(1))),
                unordered(count.with(Query.Semantics.MULTISET)));
    }

    @Test
    void orderAndPage() {
        var byName = Query.of(names()).returning("name").orderedBy(Sort.descending("name"));
        assertEquals(List.of(row(text("Tile")), row(text("Studio")), row(text("Paint")), row(text("Kitchen")),
                row(text("Garage")), row(text("Desk"))), rows(byName));
        assertEquals(List.of(row(text("Paint")), row(text("Kitchen"))), rows(byName.page(2, 2)));
        assertEquals(List.of(), rows(byName.page(2, 10)));
    }

    @Test
    void orderByTwoColumns() {
        var q = Query.of(Aggregate.count(tasks(), List.of("p"), "n")).returning("n", "p")
                .orderedBy(Sort.descending("n"), Sort.ascending("p"));
        assertEquals(List.of(row(integer(2), ref(kitchen)), row(integer(1), ref(studio))), rows(q));
    }

    // Frontiers

    @Test
    void anAnswerSaysItsFrontier() {
        var answer = over(log).answer(Query.of(Scan.things("t")));
        assertEquals(at(second), answer.at());
        assertEquals(List.of("t"), answer.columns());
        assertEquals(at(first), over(log).answer(Query.of(Scan.things("t")).at(at(first))).at());
    }

    @Test
    void anEarlierFrontierSeesOldData() {
        assertEquals(expect(row(ref(kitchen)), row(ref(studio)), row(ref(garage))),
                unordered(Query.of(Scan.things("t")).at(at(first))));
        assertEquals(List.of(), rows(Query.of(tasks()).at(Frontier.empty())));
    }

    @Test
    void aFrontierWithAnUnknownEventIsRefused() {
        var unknown = event(5000, List.of(), Assertion.entity());
        assertThrows(IllegalArgumentException.class,
                () -> over(log).answer(Query.of(Scan.things("t")).at(at(unknown))));
    }

    @Test
    void anEmptyLogAnswersNothing() {
        log = new InMemoryEventLog();
        var answer = over(log).answer(Query.of(Scan.things("t")));
        assertEquals(Frontier.empty(), answer.at());
        assertEquals(List.of(), answer.rows());
    }

    // Stored queries

    @Test
    void aStoredQueryIsReadBackAndAnswered() {
        var open = Query.of(new Not(Scan.things("t"), new Filter(Scan.things("t"), new Condition.Has("t", DONE))))
                .orderedBy(Sort.ascending("t"));
        var saved = event(1002, List.of(second.id()),
                Assertion.entity(), open.stored(Identity.local(0)));
        log.append(List.of(saved));
        var statement = Statements.about(Identity.of(saved.id(), 0), at(saved), log).stream()
                .filter(s -> s.assertion().attribute().filter(Query.ATTRIBUTE::equals).isPresent())
                .findFirst().orElseThrow();
        var read = Query.from(statement);
        assertEquals(open, read);
        var things = over(log).answer(read.at(at(second))).rows();
        assertEquals(expect(row(ref(kitchen)), row(ref(studio)), row(ref(tile)), row(ref(desk))), sorted(things));
    }
}
