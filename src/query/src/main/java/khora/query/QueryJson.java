package khora.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.events.Assertion;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.json.Json;

/**
 * A query's JSON. Operators and conditions are objects with one member, named for what they are:
 * {@code {"scan": {...}}}, {@code {"has": {...}}}. Enumerations are lowercase strings. Values are
 * written as assertions write them. Every object refuses members it does not know.
 *
 * <pre>
 * {"body": {"filter": {"input": {"scan": {"as": "p"}},
 *                      "where": {"has": {"column": "p", "attribute": "sha256:..."}}}},
 *  "at": ["sha256:..."], "semantics": "set", "return": ["p"],
 *  "order": [{"column": "p", "direction": "asc"}], "page": {"limit": 10, "offset": 0}}
 * </pre>
 */
final class QueryJson {
    private QueryJson() {}

    static Json.Obj write(Query q) {
        var m = new HashMap<String, Json>();
        m.put("body", operator(q.body()));
        q.at().ifPresent(f -> m.put("at", new Json.Arr(f.heads().stream().<Json>map(h -> new Json.Str(h.value())).toList())));
        m.put("semantics", name(q.semantics()));
        m.put("return", strings(q.returns()));
        if (!q.order().isEmpty()) {
            m.put("order", new Json.Arr(q.order().stream().<Json>map(s -> Json.Obj.of(Map.of(
                    "column", new Json.Str(s.column()),
                    "direction", new Json.Str(s.descending() ? "desc" : "asc")))).toList()));
        }
        q.page().ifPresent(p -> m.put("page", Json.Obj.of(Map.of(
                "limit", new Json.Int(p.limit()), "offset", new Json.Int(p.offset())))));
        return Json.Obj.of(m);
    }

    static Query read(Json json) {
        var o = obj(json, "a query");
        members(o, Set.of("body", "return"), Set.of("at", "semantics", "order", "page"));
        var body = operator(o.get("body").orElseThrow());
        Optional<Frontier> at = o.get("at").map(a -> {
            var heads = new TreeSet<EventId>();
            for (var item : arr(a, "at").items()) {
                heads.add(new EventId(str(item, "a head")));
            }
            return new Frontier(heads);
        });
        var semantics = o.get("semantics").map(s -> named(Query.Semantics.class, s, "semantics"))
                .orElse(Query.Semantics.SET);
        var order = new ArrayList<Query.Sort>();
        o.get("order").ifPresent(a -> {
            for (var item : arr(a, "order").items()) {
                var s = obj(item, "a sort");
                members(s, Set.of("column", "direction"), Set.of());
                var direction = str(s.members().get("direction"), "direction");
                if (!direction.equals("asc") && !direction.equals("desc")) {
                    throw new IllegalArgumentException("direction is asc or desc: " + direction);
                }
                order.add(new Query.Sort(str(s.members().get("column"), "column"), direction.equals("desc")));
            }
        });
        Optional<Query.Page> page = o.get("page").map(p -> {
            var po = obj(p, "a page");
            members(po, Set.of("limit"), Set.of("offset"));
            return new Query.Page(po.integer("limit"), po.get("offset").isPresent() ? po.integer("offset") : 0);
        });
        return new Query(body, at, semantics, strings(o.members().get("return"), "return"), order, page);
    }

    private static Json.Obj operator(Operator op) {
        return switch (op) {
            case Operator.Scan s -> {
                var m = new HashMap<String, Json>();
                m.put("as", new Json.Str(s.as()));
                s.attribute().ifPresent(a -> m.put("attribute", new Json.Str(a.value())));
                s.value().ifPresent(v -> m.put("value", new Json.Str(v)));
                yield tagged("scan", Json.Obj.of(m));
            }
            case Operator.Filter f -> tagged("filter", Json.Obj.of(Map.of(
                    "input", operator(f.input()), "where", condition(f.where()))));
            case Operator.Not n -> tagged("not", Json.Obj.of(Map.of(
                    "input", operator(n.input()), "minus", operator(n.minus()))));
            case Operator.Aggregate a -> {
                var m = new HashMap<String, Json>();
                m.put("input", operator(a.input()));
                m.put("group", strings(a.group()));
                m.put("function", name(a.function()));
                a.of().ifPresent(c -> m.put("of", new Json.Str(c)));
                m.put("as", new Json.Str(a.as()));
                yield tagged("aggregate", Json.Obj.of(m));
            }
        };
    }

    private static Operator operator(Json json) {
        var tag = tag(json, "an operator");
        var o = tag.getValue();
        return switch (tag.getKey()) {
            case "scan" -> {
                members(o, Set.of("as"), Set.of("attribute", "value"));
                yield new Operator.Scan(o.str("as"), o.get("attribute").map(a -> new Identity(str(a, "attribute"))),
                        o.get("value").map(v -> str(v, "value")));
            }
            case "filter" -> {
                members(o, Set.of("input", "where"), Set.of());
                yield new Operator.Filter(operator(o.members().get("input")), condition(o.members().get("where")));
            }
            case "not" -> {
                members(o, Set.of("input", "minus"), Set.of());
                yield new Operator.Not(operator(o.members().get("input")), operator(o.members().get("minus")));
            }
            case "aggregate" -> {
                members(o, Set.of("input", "group", "function", "as"), Set.of("of"));
                yield new Operator.Aggregate(operator(o.members().get("input")), strings(o.members().get("group"), "group"),
                        named(Operator.Function.class, o.members().get("function"), "function"),
                        o.get("of").map(c -> str(c, "of")), o.str("as"));
            }
            default -> throw new IllegalArgumentException("unknown operator " + tag.getKey());
        };
    }

    private static Json.Obj condition(Condition c) {
        return switch (c) {
            case Condition.Has h -> {
                var m = new HashMap<String, Json>();
                m.put("column", new Json.Str(h.column()));
                m.put("attribute", new Json.Str(h.attribute().value()));
                h.value().ifPresent(v -> m.put("value", Assertion.valueJson(v)));
                yield tagged("has", Json.Obj.of(m));
            }
            case Condition.Compare k -> tagged("compare", Json.Obj.of(Map.of(
                    "column", new Json.Str(k.column()), "op", name(k.op()), "value", Assertion.valueJson(k.value()))));
            case Condition.And a -> tagged("and", new Json.Arr(a.all().stream().<Json>map(QueryJson::condition).toList()));
            case Condition.Or a -> tagged("or", new Json.Arr(a.any().stream().<Json>map(QueryJson::condition).toList()));
        };
    }

    private static Condition condition(Json json) {
        var o = obj(json, "a condition");
        if (o.members().size() != 1) {
            throw new IllegalArgumentException("a condition is an object with one member, its name");
        }
        var entry = o.members().entrySet().iterator().next();
        return switch (entry.getKey()) {
            case "has" -> {
                var h = obj(entry.getValue(), "has");
                members(h, Set.of("column", "attribute"), Set.of("value"));
                yield new Condition.Has(h.str("column"), new Identity(h.str("attribute")),
                        h.get("value").map(v -> Assertion.valueOf(obj(v, "a value"))));
            }
            case "compare" -> {
                var k = obj(entry.getValue(), "compare");
                members(k, Set.of("column", "op", "value"), Set.of());
                yield new Condition.Compare(k.str("column"), named(Condition.Op.class, k.members().get("op"), "op"),
                        Assertion.valueOf(k.obj("value")));
            }
            case "and" -> new Condition.And(conditions(entry.getValue(), "and"));
            case "or" -> new Condition.Or(conditions(entry.getValue(), "or"));
            default -> throw new IllegalArgumentException("unknown condition " + entry.getKey());
        };
    }

    private static List<Condition> conditions(Json json, String what) {
        return arr(json, what).items().stream().map(QueryJson::condition).toList();
    }

    private static Json.Obj tagged(String tag, Json content) {
        return Json.Obj.of(Map.of(tag, content));
    }

    private static Map.Entry<String, Json.Obj> tag(Json json, String what) {
        var o = obj(json, what);
        if (o.members().size() != 1) {
            throw new IllegalArgumentException(what + " is an object with one member, its name");
        }
        var entry = o.members().entrySet().iterator().next();
        return Map.entry(entry.getKey(), obj(entry.getValue(), entry.getKey()));
    }

    private static void members(Json.Obj o, Set<String> required, Set<String> optional) {
        for (var key : required) {
            if (!o.members().containsKey(key)) {
                throw new IllegalArgumentException("missing member " + key);
            }
        }
        for (var key : o.members().keySet()) {
            if (!required.contains(key) && !optional.contains(key)) {
                throw new IllegalArgumentException("unknown member " + key);
            }
        }
    }

    private static Json.Str name(Enum<?> e) {
        return new Json.Str(e.name().toLowerCase(Locale.ROOT));
    }

    private static <E extends Enum<E>> E named(Class<E> type, Json json, String what) {
        var text = str(json, what);
        for (var e : type.getEnumConstants()) {
            if (e.name().toLowerCase(Locale.ROOT).equals(text)) {
                return e;
            }
        }
        throw new IllegalArgumentException("unknown " + what + " " + text);
    }

    private static Json.Arr strings(List<String> items) {
        return new Json.Arr(items.stream().<Json>map(Json.Str::new).toList());
    }

    private static List<String> strings(Json json, String what) {
        return arr(json, what).items().stream().map(i -> str(i, what)).toList();
    }

    private static Json.Obj obj(Json json, String what) {
        if (!(json instanceof Json.Obj o)) {
            throw new IllegalArgumentException(what + " is an object");
        }
        return o;
    }

    private static Json.Arr arr(Json json, String what) {
        if (!(json instanceof Json.Arr a)) {
            throw new IllegalArgumentException(what + " is an array");
        }
        return a;
    }

    private static String str(Json json, String what) {
        if (!(json instanceof Json.Str s)) {
            throw new IllegalArgumentException(what + " is a string");
        }
        return s.value();
    }
}
