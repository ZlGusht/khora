package khora.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Assertion.Value;
import khora.events.Frontier;
import khora.events.json.Json;

/**
 * Answers a query over a {@link Graph}, operator by operator, as the operators' Javadoc says.
 * A datastore that has no better plan uses it.
 *
 * <p>Provisional: evaluates in memory, one operator after another, without indexes of its own.
 */
public final class Evaluator {
    private Evaluator() {}

    /** The answer to {@code query} over {@code graph}, which holds what is true at {@code at}. */
    public static Answer answer(Query query, Frontier at, Graph graph) {
        var set = query.semantics() == Query.Semantics.SET;
        var rows = new ArrayList<List<Value>>();
        for (var row : evaluate(query.body(), graph, set)) {
            var values = new ArrayList<Value>();
            query.returns().forEach(c -> values.add(row.get(c)));
            rows.add(values);
        }
        List<List<Value>> result = set ? new ArrayList<>(new LinkedHashSet<>(rows)) : rows;
        if (!query.order().isEmpty()) {
            result.sort(comparator(query));
        }
        if (query.page().isPresent()) {
            var page = query.page().get();
            result = result.stream().skip(page.offset()).limit(page.limit()).toList();
        }
        return new Answer(at, query.returns(), result);
    }

    private static Comparator<List<Value>> comparator(Query query) {
        Comparator<List<Value>> c = (a, b) -> 0;
        for (var sort : query.order()) {
            int i = query.returns().indexOf(sort.column());
            Comparator<List<Value>> by = Comparator.comparing(row -> row.get(i), Values.ORDER);
            c = c.thenComparing(sort.descending() ? by.reversed() : by);
        }
        return c;
    }

    private static List<Map<String, Value>> evaluate(Operator op, Graph graph, boolean set) {
        var rows = switch (op) {
            case Operator.Scan s -> scan(s, graph);
            case Operator.Filter f -> evaluate(f.input(), graph, set).stream()
                    .filter(row -> holds(f.where(), row, graph)).toList();
            case Operator.Not n -> not(n, graph, set);
            case Operator.Aggregate a -> aggregate(a, evaluate(a.input(), graph, set));
        };
        return set ? List.copyOf(new LinkedHashSet<>(rows)) : rows;
    }

    private static List<Map<String, Value>> scan(Operator.Scan s, Graph graph) {
        if (s.attribute().isEmpty()) {
            return graph.things().stream().map(t -> Map.<String, Value>of(s.as(), new Ref(t))).toList();
        }
        var value = s.value().orElseThrow();
        return graph.with(s.attribute().get()).stream()
                .map(f -> Map.<String, Value>of(s.as(), new Ref(f.thing()), value, f.value())).toList();
    }

    private static boolean holds(Condition c, Map<String, Value> row, Graph graph) {
        return switch (c) {
            case Condition.Has h -> row.get(h.column()) instanceof Ref(var thing)
                    && h.value().map(v -> graph.values(thing, h.attribute()).contains(v))
                            .orElseGet(() -> !graph.values(thing, h.attribute()).isEmpty());
            case Condition.Compare k -> Values.compare(row.get(k.column()), k.op(), k.value());
            case Condition.And a -> a.all().stream().allMatch(x -> holds(x, row, graph));
            case Condition.Or o -> o.any().stream().anyMatch(x -> holds(x, row, graph));
        };
    }

    private static List<Map<String, Value>> not(Operator.Not n, Graph graph, boolean set) {
        var columns = n.minus().columns();
        var excluded = new HashSet<List<Value>>();
        evaluate(n.minus(), graph, set).forEach(row -> excluded.add(project(row, columns)));
        return evaluate(n.input(), graph, set).stream()
                .filter(row -> !excluded.contains(project(row, columns))).toList();
    }

    private static List<Value> project(Map<String, Value> row, List<String> columns) {
        var values = new ArrayList<Value>(columns.size());
        columns.forEach(c -> values.add(row.get(c)));
        return values;
    }

    private static List<Map<String, Value>> aggregate(Operator.Aggregate a, List<Map<String, Value>> rows) {
        var groups = new LinkedHashMap<List<Value>, List<Map<String, Value>>>();
        for (var row : rows) {
            groups.computeIfAbsent(project(row, a.group()), k -> new ArrayList<>()).add(row);
        }
        if (groups.isEmpty() && a.group().isEmpty()) {
            groups.put(List.of(), List.of());
        }
        var result = new ArrayList<Map<String, Value>>();
        for (var group : groups.entrySet()) {
            var values = a.of().map(c -> group.getValue().stream().map(r -> r.get(c)).toList()).orElse(List.of());
            compute(a.function(), group.getValue().size(), values).ifPresent(v -> {
                var row = new HashMap<String, Value>();
                for (int i = 0; i < a.group().size(); i++) {
                    row.put(a.group().get(i), group.getKey().get(i));
                }
                row.put(a.as(), v);
                result.add(Map.copyOf(row));
            });
        }
        return result;
    }

    private static Optional<Value> compute(Operator.Function function, int count, Collection<Value> values) {
        return switch (function) {
            case COUNT -> Optional.of(integer(count));
            case SUM -> Optional.of(integer(values.stream().map(Values::integer).flatMap(Optional::stream)
                    .reduce(0L, Math::addExact)));
            case MIN -> values.stream().min(Values.ORDER);
            case MAX -> values.stream().max(Values.ORDER);
        };
    }

    private static Value integer(long n) {
        return new Literal(new Json.Int(n));
    }
}
