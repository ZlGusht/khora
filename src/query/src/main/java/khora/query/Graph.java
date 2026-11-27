package khora.query;

import java.util.List;
import khora.events.Assertion.Value;
import khora.events.Identity;

/**
 * What holds at one frontier, as {@link Evaluator} reads it: the things and the statements about
 * them. A datastore gives one per answer, from its own storage.
 *
 * <p>Provisional: the three lookups the stage 1 operators need, and no more.
 */
public interface Graph {

    /** The things brought into being by an empty tuple that holds, in the datastore's order. */
    List<Identity> things();

    /** The holding statements with {@code attribute}: each one's thing and value, in the datastore's order. */
    List<Fact> with(Identity attribute);

    /** The values of {@code thing}'s holding statements with {@code attribute}. */
    List<Value> values(Identity thing, Identity attribute);

    /** A statement's thing and value. */
    record Fact(Identity thing, Value value) {}
}
