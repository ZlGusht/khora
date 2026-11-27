package khora.events;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Something that happened: a fact by whoever emitted it, an envelope around a {@link Body}. Facts
 * do not conflict; what they mean together is for the surfaces that read them.
 *
 * <p>The id is the hash of the event's canonical JSON without the id
 * ([[Event ids are salted hashes of canonical JSON]]); the constructor refuses an id that does not
 * match the content.
 *
 * <p>Exploration: an event names at most one other event, its trigger, the event it was emitted in
 * response to. That is the system trace; why it was done is content its emitter fills in.
 *
 * @param id the hash of everything else
 * @param trigger the event this one was emitted in response to, if any
 * @param recorded the hybrid logical clock time it was recorded at ({@link Hlc})
 * @param occurred when it happened, in milliseconds, if earlier than recorded
 * @param salt 16 random bytes as 32 lowercase hex digits, so the id cannot be guessed from content
 * @param body what happened
 */
public record Event(EventId id, Optional<EventId> trigger, long recorded, Optional<Long> occurred, String salt,
        Body body) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Event {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(occurred, "occurred");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(body, "body");
        if (!salt.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("salt is not 32 lowercase hex digits");
        }
        if (recorded < 0) {
            throw new IllegalArgumentException("recorded is negative: " + recorded);
        }
        if (occurred.isPresent() && (occurred.get() < 0 || occurred.get() > Hlc.millis(recorded))) {
            throw new IllegalArgumentException("occurred is not between 0 and when it was recorded: " + occurred.get());
        }
        var expected = EventId.hashOf(EventJson.content(trigger, recorded, occurred, salt, body).canonical());
        if (!expected.equals(id)) {
            throw new IllegalArgumentException("id does not match content");
        }
    }

    /** A new event with a random salt. */
    public static Event create(Optional<EventId> trigger, long recorded, Optional<Long> occurred, Body body) {
        var bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return create(trigger, recorded, occurred, body, HexFormat.of().formatHex(bytes));
    }

    /** A new event with the given salt. */
    public static Event create(Optional<EventId> trigger, long recorded, Optional<Long> occurred, Body body,
            String salt) {
        var id = EventId.hashOf(EventJson.content(trigger, recorded, occurred, salt, body).canonical());
        return new Event(id, trigger, recorded, occurred, salt, body);
    }
}
