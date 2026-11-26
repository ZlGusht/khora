package khora.events;

import java.util.Objects;

/**
 * An assertion that holds, with its identity.
 *
 * <p>Provisional: as {@link Assertion}.
 *
 * @param id the assertion's identity
 * @param assertion the tuple, with local identities resolved
 */
public record Statement(Identity id, Assertion assertion) {
    public Statement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(assertion, "assertion");
    }
}
