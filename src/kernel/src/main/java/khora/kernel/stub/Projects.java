package khora.kernel.stub;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import khora.kernel.event.AcceptedAction;
import khora.kernel.event.Assertion;
import khora.kernel.event.Basic;
import khora.kernel.event.Event;
import khora.kernel.event.FrontierCheck;
import khora.kernel.event.Identity;
import khora.kernel.log.EventLog;
import khora.kernel.surface.Authority;

/**
 * A stub authority for projects. It takes reads and asserts. A write linked {@code based_on} a read
 * is rejected when an event it did not know of asserts on the same identity and attribute
 * ([[Reads return statements and writes follow a read]]); a write without one is blind and accepted.
 *
 * <p>Provisional: no attribute definitions or merge rules; a conflict check walks the whole log.
 */
public final class Projects implements Authority {

    @Override
    public String name() {
        return "projects";
    }

    @Override
    public Verdict decide(Event candidate, EventLog log) {
        var accepted = (AcceptedAction) candidate.body();
        if (Basic.readOf(accepted.action()).isPresent()) {
            return new Accept();
        }
        if (Assertion.in(accepted.action()).isEmpty()) {
            return new Reject("projects takes only reads and asserts");
        }
        if (!accepted.links().containsKey("based_on")) {
            return new Accept();
        }
        var known = FrontierCheck.closure(candidate.causes(), log);
        var touched = touched(candidate);
        for (long from = 1; ; from += EventLog.MAX_APPEND) {
            var page = log.read(from, EventLog.MAX_APPEND);
            for (var logged : page) {
                var event = logged.event();
                if (known.contains(event.id())) {
                    continue;
                }
                var overlap = touched(event);
                overlap.retainAll(touched);
                if (!overlap.isEmpty()) {
                    return new Reject("conflicts with " + event.id().value() + ", which the write did not know of");
                }
            }
            if (page.size() < EventLog.MAX_APPEND) {
                return new Accept();
            }
        }
    }

    /** The (identity, attribute) pairs an event asserts on. */
    private static Set<List<Identity>> touched(Event event) {
        var pairs = new HashSet<List<Identity>>();
        if (event.body() instanceof AcceptedAction accepted) {
            for (var tuple : Assertion.in(accepted.action()).orElse(List.of())) {
                var resolved = tuple.in(event.id());
                if (resolved.identity().isPresent() && resolved.attribute().isPresent()) {
                    pairs.add(List.of(resolved.identity().get(), resolved.attribute().get()));
                }
            }
        }
        return pairs;
    }
}
