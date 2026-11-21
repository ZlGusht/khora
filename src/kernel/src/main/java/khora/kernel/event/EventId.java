package khora.kernel.event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The id of an event in the log, and of whatever an assert asserts.
 *
 * <p>Provisional: a placeholder until K1 (#18) defines event ids as content hashes over their causes.
 * {@link #hashOf} already makes K1's ids; the constructor starts refusing other values when the
 * placeholder is replaced.
 */
public record EventId(String value) implements Comparable<EventId> {
    public EventId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
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
