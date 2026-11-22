package khora.kernel.event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The id of an event: {@code sha256:} and the SHA-256 of the event's canonical JSON
 * ([[Event ids are salted hashes of canonical JSON]]).
 *
 * <p>Provisional: the hash function is fixed until interoperability chooses an identity scheme
 * (ADR-0XZ decision 21).
 */
public record EventId(String value) implements Comparable<EventId> {
    public EventId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("not an event id: " + value);
        }
    }

    /** The id {@code sha256:<hex>} of the UTF-8 bytes of {@code text}. */
    public static EventId hashOf(String text) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return new EventId("sha256:" + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Override
    public int compareTo(EventId other) {
        return value.compareTo(other.value);
    }
}
