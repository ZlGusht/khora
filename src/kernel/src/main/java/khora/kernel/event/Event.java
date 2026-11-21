package khora.kernel.event;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An action khora accepted, with its causes and its time
 * ([[An event is an accepted action, and an assert is the basic action]]).
 *
 * <p>The id is the hash of the event's canonical JSON without the id
 * ([[Event ids are salted hashes of canonical JSON]]); the constructor refuses an id that does not
 * match the content.
 *
 * <p>Provisional: the fields follow the Issue #18 design and may change during execution.
 *
 * @param id the hash of everything else
 * @param causes the heads of what the action read
 * @param recorded the hybrid logical clock time it was recorded at ({@link Hlc})
 * @param occurred when it happened, in milliseconds, if earlier than recorded
 * @param by who acted
 * @param salt 16 random bytes as 32 lowercase hex digits, so the id cannot be guessed from content
 * @param action what was done
 */
public record Event(EventId id, SortedSet<EventId> causes, long recorded, Optional<Long> occurred, By by,
        String salt, Action action) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Event {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(causes, "causes");
        Objects.requireNonNull(occurred, "occurred");
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(action, "action");
        if (!salt.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("salt is not 32 lowercase hex digits");
        }
        causes = Collections.unmodifiableSortedSet(new TreeSet<>(causes));
        var expected = EventId.hashOf(EventJson.content(causes, recorded, occurred, by, salt, action).canonical());
        if (!expected.equals(id)) {
            throw new IllegalArgumentException("id does not match content");
        }
    }

    /** A new event with a random salt. */
    public static Event create(Collection<EventId> causes, long recorded, Optional<Long> occurred, By by,
            Action action) {
        var bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return create(causes, recorded, occurred, by, action, HexFormat.of().formatHex(bytes));
    }

    /** A new event with the given salt. */
    public static Event create(Collection<EventId> causes, long recorded, Optional<Long> occurred, By by,
            Action action, String salt) {
        var sorted = new TreeSet<>(causes);
        var id = EventId.hashOf(EventJson.content(sorted, recorded, occurred, by, salt, action).canonical());
        return new Event(id, sorted, recorded, occurred, by, salt, action);
    }
}
