package khora.events;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The identity of a thing: an assertion within an event, {@code <event id>/<index>}, or a well-known
 * name's hash, {@code sha256:<hex>} ([[An event carries several assertions]]). Inside an event, a
 * tuple names another tuple of the same event as {@code /<index>}, since the event's id is not known
 * until the event is made; {@link #in} resolves it.
 *
 * <p>Provisional: identities from other deployments are not covered yet (ADR-0XZ decision 21).
 */
public record Identity(String value) {
    private static final Pattern FORMAT = Pattern.compile("(sha256:[0-9a-f]{64})?(/(0|[1-9][0-9]{0,8}))?");

    public Identity {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty() || !FORMAT.matcher(value).matches()) {
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

    /** The {@code index}-th tuple of the event this identity is written in. */
    public static Identity local(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index is negative: " + index);
        }
        return new Identity("/" + index);
    }

    /** This identity as written in {@code event}: a local one becomes that event's tuple. */
    public Identity in(EventId event) {
        return value.startsWith("/") ? new Identity(event.value() + value) : this;
    }

    /** The event whose tuple this is, if it is a tuple of an event. */
    public Optional<EventId> event() {
        int slash = value.indexOf('/');
        return slash > 0 ? Optional.of(new EventId(value.substring(0, slash))) : Optional.empty();
    }

    /** The identity of a well-known name, such as a basic attribute. */
    public static Identity named(String name) {
        return new Identity(EventId.hashOf(name).value());
    }
}
