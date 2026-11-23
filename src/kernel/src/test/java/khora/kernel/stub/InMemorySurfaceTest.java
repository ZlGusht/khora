package khora.kernel.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import khora.kernel.event.EventId;
import khora.kernel.surface.Actions;
import khora.kernel.surface.Budget;
import khora.kernel.surface.Completeness;
import khora.kernel.surface.Condition;
import khora.kernel.surface.Context;
import khora.kernel.surface.Effects;
import khora.kernel.surface.Frontier;
import khora.kernel.surface.Grant;
import khora.kernel.surface.History;
import khora.kernel.surface.Operation;
import khora.kernel.surface.Principal;
import khora.kernel.surface.ReadRequest;
import khora.kernel.surface.ReadTime;
import khora.kernel.surface.Reads;
import khora.kernel.surface.Surface;
import khora.kernel.surface.WriteAnswer;
import khora.kernel.surface.Writes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemorySurfaceTest {

    /** Records each call and hands out a1, a2, … as action ids. */
    static final class CountingActions implements Actions {
        final List<String> calls = new ArrayList<>();
        final List<EventId> ids = new ArrayList<>();

        @Override
        public synchronized EventId record(Context context, String call) {
            calls.add(call);
            var id = new EventId("a" + (ids.size() + 1));
            ids.add(id);
            return id;
        }
    }

    static final Context CONTEXT =
            new Context(new Principal("simeon"), new Grant("g"), new Budget(10), Optional.empty());

    CountingActions actions;
    InMemorySurface surface;

    @BeforeEach
    void setUp() {
        actions = new CountingActions();
        surface = new InMemorySurface(actions);
    }

    static ReadRequest readLatest(String target) {
        return new ReadRequest(target, ReadTime.latest());
    }

    static ReadRequest readAt(String target, Frontier at) {
        return new ReadRequest(target, new ReadTime(Optional.of(at)));
    }

    /** A condition on what a latest read of {@code target} sees now. */
    static Condition asRead(Surface surface, String target) {
        var result = surface.read(readLatest(target), CONTEXT);
        return new Condition(result.seen(), result.value());
    }

    @Test
    void declaresConditionalWrites() {
        var declaration = surface.declares();
        assertTrue(declaration.authoritative());
        assertEquals(Effects.NATIVE, declaration.effects());
        assertEquals(History.NONE, declaration.history());
        assertEquals(Reads.DETERMINISTIC, declaration.reads());
        assertEquals(Writes.CONDITIONAL, declaration.writes());
    }

    @Test
    void eachReadIsOneAction() {
        var first = surface.read(readLatest("x"), CONTEXT);
        var second = surface.read(readLatest("x"), CONTEXT);

        assertEquals(List.of("read", "read"), actions.calls);
        assertEquals(actions.ids.get(0), first.action());
        assertEquals(actions.ids.get(1), second.action());
    }

    @Test
    void eachWriteIsOneAction() {
        var condition = asRead(surface, "x");

        var answer = surface.write(new Operation("x", "1"), condition, CONTEXT);

        assertEquals(List.of("read", "write"), actions.calls);
        assertEquals(actions.ids.get(1), answer.action());
    }

    @Test
    void writeThenReadSeesValue() {
        var before = asRead(surface, "x");

        var answer = surface.write(new Operation("x", "1"), before, CONTEXT);
        var result = surface.read(readLatest("x"), CONTEXT);

        assertInstanceOf(WriteAnswer.Unconfirmed.class, answer);
        assertInstanceOf(Completeness.Complete.class, result.completeness());
        assertEquals(Optional.of("1"), result.value());
        assertNotEquals(before.read(), result.seen());
    }

    @Test
    void readAtCurrentFrontierIsComplete() {
        var seen = surface.read(readLatest("x"), CONTEXT).seen();

        var result = surface.read(readAt("x", seen), CONTEXT);

        assertInstanceOf(Completeness.Complete.class, result.completeness());
    }

    @Test
    void readAtUnknownFrontierIsPartial() {
        var result = surface.read(readAt("x", new Frontier("somewhere else")), CONTEXT);

        assertInstanceOf(Completeness.Partial.class, result.completeness());
    }

    @Test
    void readAtPastFrontierIsPartial() {
        var first = asRead(surface, "x");
        surface.write(new Operation("x", "1"), first, CONTEXT);
        surface.write(new Operation("x", "2"), asRead(surface, "x"), CONTEXT);

        var result = surface.read(readAt("x", first.read()), CONTEXT);

        assertInstanceOf(Completeness.Partial.class, result.completeness());
        assertEquals(Optional.of("2"), result.value());
    }

    @Test
    void staleConditionIsRejected() {
        var stale = asRead(surface, "x");
        surface.write(new Operation("x", "1"), stale, CONTEXT);

        var second = surface.write(new Operation("x", "2"), stale, CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, second);
        assertEquals(Optional.of("1"), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void unexpectedValueIsRejected() {
        var seen = surface.read(readLatest("x"), CONTEXT).seen();

        var answer = surface.write(new Operation("x", "1"), new Condition(seen, Optional.of("something")), CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, answer);
        assertEquals(Optional.empty(), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void staleFrontierAloneIsRejected() {
        var stale = asRead(surface, "x");
        surface.write(new Operation("y", "1"), asRead(surface, "y"), CONTEXT);

        var answer = surface.write(new Operation("x", "1"), stale, CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, answer);
        assertEquals(Optional.empty(), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void nullActionIdLeavesSurfaceUnchanged() {
        Actions nullForWrites = (context, call) -> call.equals("write") ? null : new EventId("r");
        var broken = new InMemorySurface(nullForWrites);
        var before = asRead(broken, "x");

        assertThrows(NullPointerException.class, () -> broken.write(new Operation("x", "1"), before, CONTEXT));

        var after = broken.read(readLatest("x"), CONTEXT);
        assertEquals(Optional.empty(), after.value());
        assertEquals(before.read(), after.seen());
    }
}
