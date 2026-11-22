package khora.kernel.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import khora.kernel.json.Json;

/**
 * Events to and from canonical JSON.
 *
 * <p>Provisional: the layout follows the Issue #18 design:
 * {@code {"body":{"accepted_action":{"action":{"args":…,"definition":…},
 * "by":{"grant","on_behalf_of"?,"principal","via"}}},"causes":[…],"id"?,"occurred"?,"recorded","salt"}}.
 * A body is an object with one member, named after its kind. Unknown members are refused at every
 * level, because the id covers only what is read.
 */
public final class EventJson {
    private EventJson() {}

    /** The event as JSON, with or without its id. */
    public static Json.Obj toJson(Event e, boolean withId) {
        var content = content(e.causes(), e.recorded(), e.occurred(), e.salt(), e.body());
        if (!withId) {
            return content;
        }
        var members = new HashMap<>(content.members());
        members.put("id", new Json.Str(e.id().value()));
        return Json.Obj.of(members);
    }

    /** The event read from JSON; refuses an id that does not match the content. */
    public static Event fromJson(Json.Obj o) {
        only(o, Set.of("body", "causes", "id", "occurred", "recorded", "salt"));
        var causes = new TreeSet<EventId>();
        for (var cause : o.arr("causes").items()) {
            if (!(cause instanceof Json.Str s)) {
                throw new IllegalArgumentException("a cause is not a string");
            }
            causes.add(new EventId(s.value()));
        }
        Optional<Long> occurred = o.get("occurred").isPresent() ? Optional.of(o.integer("occurred")) : Optional.empty();
        return new Event(new EventId(o.str("id")), causes, o.integer("recorded"), occurred, o.str("salt"),
                body(o.obj("body")));
    }

    /** The event's canonical text, with its id. */
    public static String write(Event e) {
        return toJson(e, true).canonical();
    }

    /** The event read from text; refuses an id that does not match the content. */
    public static Event read(String text) {
        if (!(Json.parse(text) instanceof Json.Obj o)) {
            throw new IllegalArgumentException("an event is a JSON object");
        }
        return fromJson(o);
    }

    static Json.Obj content(SortedSet<EventId> causes, long recorded, Optional<Long> occurred, String salt,
            Body body) {
        var causeItems = new ArrayList<Json>();
        causes.forEach(c -> causeItems.add(new Json.Str(c.value())));

        var members = new HashMap<String, Json>();
        members.put("body", body(body));
        members.put("causes", new Json.Arr(causeItems));
        occurred.ifPresent(t -> members.put("occurred", new Json.Int(t)));
        members.put("recorded", new Json.Int(recorded));
        members.put("salt", new Json.Str(salt));
        return Json.Obj.of(members);
    }

    private static Json.Obj body(Body body) {
        return switch (body) {
            case AcceptedAction a -> Json.Obj.of(Map.of("accepted_action", Json.Obj.of(Map.of(
                    "by", by(a.by()),
                    "action", Json.Obj.of(Map.of(
                            "definition", new Json.Str(a.action().definition().value()),
                            "args", a.action().args()))))));
        };
    }

    private static Body body(Json.Obj o) {
        if (o.members().size() != 1) {
            throw new IllegalArgumentException("a body has exactly one kind");
        }
        var kind = o.members().firstKey();
        if (kind.equals("accepted_action")) {
            var a = o.obj(kind);
            only(a, Set.of("action", "by"));
            var action = a.obj("action");
            only(action, Set.of("args", "definition"));
            return new AcceptedAction(by(a.obj("by")),
                    new Action(new Identity(action.str("definition")), action.obj("args")));
        }
        throw new IllegalArgumentException("unknown kind of body: " + kind);
    }

    private static Json.Obj by(By by) {
        var members = new HashMap<String, Json>();
        members.put("principal", new Json.Str(by.principal()));
        by.onBehalfOf().ifPresent(o -> members.put("on_behalf_of", new Json.Str(o)));
        members.put("grant", new Json.Str(by.grant()));
        members.put("via", new Json.Str(by.via()));
        return Json.Obj.of(members);
    }

    private static By by(Json.Obj o) {
        only(o, Set.of("grant", "on_behalf_of", "principal", "via"));
        Optional<String> onBehalfOf = o.get("on_behalf_of").isPresent()
                ? Optional.of(o.str("on_behalf_of")) : Optional.empty();
        return new By(o.str("principal"), onBehalfOf, o.str("grant"), o.str("via"));
    }

    /** Refuses members outside {@code allowed}: the id covers only what is read, so nothing else may be in the text. */
    private static void only(Json.Obj o, Set<String> allowed) {
        for (var key : o.members().keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("unknown member " + key);
            }
        }
    }
}
