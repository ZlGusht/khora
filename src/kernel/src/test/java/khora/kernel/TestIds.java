package khora.kernel;

import java.util.Set;
import java.util.TreeSet;
import khora.kernel.event.EventId;
import khora.kernel.event.Frontier;

/** Valid event ids and frontiers for tests that need some, not particular ones. */
public final class TestIds {
    private TestIds() {}

    /** The {@code n}-th test event id. */
    public static EventId id(int n) {
        return EventId.hashOf("test/" + n);
    }

    /** A frontier whose only head is the {@code n}-th test event id. */
    public static Frontier frontier(int n) {
        return new Frontier(new TreeSet<>(Set.of(id(n))));
    }
}
