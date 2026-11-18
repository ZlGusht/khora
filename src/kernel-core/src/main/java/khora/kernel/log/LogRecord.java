package khora.kernel.log;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One entry of the log. {@code position} is 0 until the record is appended.
 * The first of {@code entityIds} is the primary entity.
 */
public record LogRecord(long position, String eventId, String eventType, List<String> entityIds, byte[] body) {

    private static final Set<String> EVENT_TYPES = Set.of("action", "outcome", "effect", "observation");

    public LogRecord {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be null or blank");
        }
        if (!EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException(
                    "eventType must be one of action, outcome, effect, observation: " + eventType);
        }
        entityIds = List.copyOf(Objects.requireNonNull(entityIds, "entityIds"));
        Objects.requireNonNull(body, "body");
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
