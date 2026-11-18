package khora.kernel.log.nats;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import khora.kernel.log.LogRecord;

/**
 * Subjects of the log stream: {@code khora.log.<eventType>.<entity>}, where {@code <entity>} is the
 * primary entity in base64url without padding, or {@code _} when the record has no entity. The
 * encoding keeps any entity id to one subject token: no {@code .}, {@code *}, {@code >} or spaces.
 */
public final class Subjects {

    static final String PREFIX = "khora.log.";
    static final String ALL = PREFIX + ">";
    static final String NO_ENTITY = "_";

    private Subjects() {
    }

    public static String forRecord(LogRecord record) {
        return PREFIX + record.eventType() + "." + record.primaryEntity().map(Subjects::token).orElse(NO_ENTITY);
    }

    /** The filter that matches every record whose primary entity is {@code entityId}. */
    public static String historyFilter(String entityId) {
        return PREFIX + "*." + token(entityId);
    }

    /** {@code entityId} as one subject token. An empty id has no token and is refused. */
    static String token(String entityId) {
        if (entityId.isEmpty()) {
            throw new IllegalArgumentException("entity id must not be empty");
        }
        return encode(entityId);
    }

    static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String token) {
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    }

    /** The event type of a stored message's subject. */
    static String eventType(String subject) {
        String[] tokens = subject.split("\\.");
        if (tokens.length != 4 || !subject.startsWith(PREFIX)) {
            throw new IllegalArgumentException("not a log subject: " + subject);
        }
        return tokens[2];
    }
}
