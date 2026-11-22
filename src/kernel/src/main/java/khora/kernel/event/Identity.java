package khora.kernel.event;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The identity of a thing: an assertion within an event, {@code <event id>/<index>}, or a well-known
 * name's hash, {@code sha256:<hex>} ([[An event carries several assertions]]).
 *
 * <p>Provisional: identities from other deployments are not covered yet (ADR-0XZ decision 21).
 */
public record Identity(String value) {
    private static final Pattern FORMAT = Pattern.compile("sha256:[0-9a-f]{64}(/(0|[1-9][0-9]*))?");

    public Identity {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("not an identity: " + value);
        }
    }

    /** The identity of the {@code index}-th assertion of {@code event}. */
    public static Identity of(EventId event, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index is negative: " + index);
        }
        return new Identity(event.value() + "/" + index);
    }

    /** The identity of a well-known name, such as a basic attribute. */
    public static Identity named(String name) {
        return new Identity(EventId.hashOf(name).value());
    }
}
