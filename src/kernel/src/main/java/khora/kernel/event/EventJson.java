package khora.kernel.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import khora.kernel.json.Json;

/**
 * Events to and from canonical JSON.
 *
 * <p>Provisional: the layout follows the Issue #18 plan:
 * {@code {"action":{"args":…,"definition":…},"by":{"grant","on_behalf_of"?,"principal","via"},
 * "causes":[…],"id"?,"occurred"?,"recorded","salt"}}.
 */
public final class EventJson {
    private EventJson() {}

    /** The event as JSON, with or without its id. */
    public static Json.Obj toJson(Event e, boolean withId) {
        var content = content(e.causes(), e.recorded(), e.occurred(), e.by(), e.salt(), e.action());
        if (!withId) {
            return content;
        }
        var members = new HashMap<>(content.members());
        members.put("id", new Json.Str(e.id().value()));
        return Json.Obj.of(members);
    }

    /** The event read from JSON; refuses an id that does not match the content. */
    public static Event fromJson(Json.Obj o) {
        var by = o.obj("by");
        var action = o.obj("action");
        var causes = new TreeSet<EventId>();
        for (var cause : o.arr("causes").items()) {
            if (!(cause instanceof Json.Str s)) {
                throw new IllegalArgumentException("a cause is not a string");
            }
            causes.add(new EventId(s.value()));
        }
        Optional<Long> occurred = o.get("occurred").isPresent() ? Optional.of(o.integer("occurred")) : Optional.empty();
        Optional<String> onBehalfOf = by.get("on_behalf_of").isPresent()
                ? Optional.of(by.str("on_behalf_of")) : Optional.empty();
        return new Event(new EventId(o.str("id")), causes, o.integer("recorded"), occurred,
                new By(by.str("principal"), onBehalfOf, by.str("grant"), by.str("via")), o.str("salt"),
                new Action(new EventId(action.str("definition")), action.obj("args")));
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

    static Json.Obj content(SortedSet<EventId> causes, long recorded, Optional<Long> occurred, By by, String salt,
            Action action) {
        var byMembers = new HashMap<String, Json>();
        byMembers.put("principal", new Json.Str(by.principal()));
        by.onBehalfOf().ifPresent(o -> byMembers.put("on_behalf_of", new Json.Str(o)));
        byMembers.put("grant", new Json.Str(by.grant()));
        byMembers.put("via", new Json.Str(by.via()));

        var causeItems = new ArrayList<Json>();
        causes.forEach(c -> causeItems.add(new Json.Str(c.value())));

        var members = new HashMap<String, Json>();
        members.put("action", Json.Obj.of(java.util.Map.of(
                "definition", new Json.Str(action.definition().value()), "args", action.args())));
        members.put("by", Json.Obj.of(byMembers));
        members.put("causes", new Json.Arr(causeItems));
        occurred.ifPresent(t -> members.put("occurred", new Json.Int(t)));
        members.put("recorded", new Json.Int(recorded));
        members.put("salt", new Json.Str(salt));
        return Json.Obj.of(members);
    }
}
