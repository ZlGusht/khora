package khora.kernel.event;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What an event is about. An assert is about the identities its tuples name, the identities they
 * refer to, and the tuples themselves; a read is about the thing it reads.
 *
 * <p>Provisional: only accepted actions are about anything.
 */
public final class About {
    private About() {}

    /** The things {@code event} is about, in the order its action names them. */
    public static Set<Identity> of(Event event) {
        var things = new LinkedHashSet<Identity>();
        if (!(event.body() instanceof AcceptedAction accepted)) {
            return things;
        }
        Basic.readOf(accepted.action()).ifPresent(things::add);
        var tuples = Assertion.in(accepted.action()).orElse(List.of());
        for (int i = 0; i < tuples.size(); i++) {
            var tuple = tuples.get(i).in(event.id());
            tuple.identity().ifPresent(things::add);
            things.add(Identity.of(event.id(), i));
            tuple.value().ifPresent(v -> {
                if (v instanceof Assertion.Ref ref) {
                    things.add(ref.id());
                }
            });
        }
        return things;
    }
}
