package khora.kernel.log.nats;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import khora.kernel.event.EventId;

/**
 * Subjects of the event stream: {@code khora.events.<id>}, where {@code <id>} is the event id in
 * base64url without padding, so any id is one subject token and an event is found by its id.
 */
public final class Subjects {
    static final String PREFIX = "khora.events.";
    static final String ALL = PREFIX + ">";

    private Subjects() {
    }

    /** The subject of the event with {@code id}. */
    public static String forEvent(EventId id) {
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(id.value().getBytes(StandardCharsets.UTF_8));
    }
}
