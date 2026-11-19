package khora.kernel.log;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One entry of the log. {@code position} is 0 until the record is appended.
 * The first of {@code entityIds} is the primary entity.
 *
 * <p>The constructor refuses, with {@link IllegalArgumentException}: an event id that is null,
 * blank, not printable ASCII, or starts or ends with a space; an event type other than action,
 * outcome, effect or observation; a null or blank entity id. {@code body} is copied, so changing
 * the caller's array changes nothing here.
 * Equality compares the body's content.
 */
public record LogRecord(long position, String eventId, String eventType, List<String> entityIds, byte[] body) {

    private static final Set<String> EVENT_TYPES = Set.of("action", "outcome", "effect", "observation");

    public LogRecord {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be null or blank");
        }
        if (!eventId.chars().allMatch(c -> c >= 0x20 && c <= 0x7e)) {
            throw new IllegalArgumentException("eventId must be printable ASCII (0x20 to 0x7E)");
        }
        if (eventId.charAt(0) == ' ' || eventId.charAt(eventId.length() - 1) == ' ') {
            // NATS trims header values, so such an id would be read back without its spaces.
            throw new IllegalArgumentException("eventId must not start or end with a space");
        }
        if (eventType == null || !EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException(
                    "eventType must be one of action, outcome, effect, observation: " + eventType);
        }
        Objects.requireNonNull(entityIds, "entityIds");
        for (String entityId : entityIds) {
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("entity ids must not be null or blank: " + entityIds);
            }
        }
        entityIds = List.copyOf(entityIds);
        body = Objects.requireNonNull(body, "body").clone();
    }

    public static LogRecord unappended(String eventId, String eventType, List<String> entityIds, byte[] body) {
        return new LogRecord(0, eventId, eventType, entityIds, body);
    }

    public Optional<String> primaryEntity() {
        return entityIds.stream().findFirst();
    }

    public LogRecord withPosition(long position) {
        return new LogRecord(position, eventId, eventType, entityIds, body);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LogRecord that
                && position == that.position
                && eventId.equals(that.eventId)
                && eventType.equals(that.eventType)
                && entityIds.equals(that.entityIds)
                && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, eventId, eventType, entityIds, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return "LogRecord[position=" + position + ", eventId=" + eventId + ", eventType=" + eventType
                + ", entityIds=" + entityIds + ", body=" + body.length + " bytes]";
    }
}
