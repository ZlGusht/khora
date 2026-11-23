package khora.events;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Something that happened, with its causes and its time: an envelope around a {@link Body}
 * ([[An accepted action is one kind of event]]).
 *
 * <p>The id is the hash of the event's canonical JSON without the id
 * ([[Event ids are salted hashes of canonical JSON]]); the constructor refuses an id that does not
 * match the content.
 *
 * <p>Provisional: the fields follow the Issue #18 design and may change during execution.
 *
 * @param id the hash of everything else
 * @param causes the events this one happened after and knew of, and nothing else
 *     ([[An event's causes are what it knew]]); what it read, translated or reacted to are named
 *     links in its body, not causes
 * @param recorded the hybrid logical clock time it was recorded at ({@link Hlc})
 * @param occurred when it happened, in milliseconds, if earlier than recorded
 * @param salt 16 random bytes as 32 lowercase hex digits, so the id cannot be guessed from content
 * @param body what happened
 */
public record Event(EventId id, SortedSet<EventId> causes, long recorded, Optional<Long> occurred, String salt,
        Body body) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Event {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(causes, "causes");
        Objects.requireNonNull(occurred, "occurred");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(body, "body");
        if (!salt.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("salt is not 32 lowercase hex digits");
        }
        var sortedCauses = new TreeSet<EventId>();
        sortedCauses.addAll(causes);
        causes = Collections.unmodifiableSortedSet(sortedCauses);
        var expected = EventId.hashOf(EventJson.content(causes, recorded, occurred, salt, body).canonical());
        if (!expected.equals(id)) {
            throw new IllegalArgumentException("id does not match content");
        }
    }

    /** A new event with a random salt. */
    public static Event create(Collection<EventId> causes, long recorded, Optional<Long> occurred,
            Body body) {
        var bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return create(causes, recorded, occurred, body, HexFormat.of().formatHex(bytes));
    }

    /** A new event with the given salt. */
    public static Event create(Collection<EventId> causes, long recorded, Optional<Long> occurred,
            Body body, String salt) {
        var sorted = new TreeSet<EventId>();
        sorted.addAll(causes);
        var id = EventId.hashOf(EventJson.content(sorted, recorded, occurred, salt, body).canonical());
        return new Event(id, sorted, recorded, occurred, salt, body);
    }
}
