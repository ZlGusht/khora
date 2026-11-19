package khora.kernel.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import khora.kernel.surface.Meaning;
import khora.kernel.surface.Operation;
import khora.kernel.surface.Principal;
import khora.kernel.surface.ReadRequest;
import khora.kernel.surface.ReadTime;
import khora.kernel.surface.Reads;
import khora.kernel.surface.TimeAxis;
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

    static Condition unread() {
        return new Condition(Frontier.empty(), Optional.empty());
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
        var answer = surface.write(new Operation("x", "1"), unread(), CONTEXT);

        assertEquals(List.of("write"), actions.calls);
        assertEquals(actions.ids.get(0), answer.action());
    }

    @Test
    void writeThenReadSeesValue() {
        var answer = surface.write(new Operation("x", "1"), unread(), CONTEXT);
        var result = surface.read(readLatest("x"), CONTEXT);

        assertInstanceOf(WriteAnswer.Unconfirmed.class, answer);
        assertInstanceOf(Completeness.Complete.class, result.completeness());
        assertEquals(Optional.of("1"), result.value());
        assertEquals(Frontier.empty().advance("stub", 1), result.seen());
    }

    @Test
    void readBeyondFrontierIsPartial() {
        var ahead = Frontier.empty().advance("stub", 5);
        var request = new ReadRequest("x", new ReadTime(Optional.of(ahead), TimeAxis.RECORDED, Meaning.THEN));

        var result = surface.read(request, CONTEXT);

        assertInstanceOf(Completeness.Partial.class, result.completeness());
        assertEquals(Frontier.empty(), result.seen());
    }

    @Test
    void staleConditionIsRejected() {
        surface.write(new Operation("x", "1"), unread(), CONTEXT);
        var second = surface.write(new Operation("x", "2"), unread(), CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, second);
        assertEquals(Optional.of("1"), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void unexpectedValueIsRejected() {
        var condition = new Condition(Frontier.empty(), Optional.of("something"));

        var answer = surface.write(new Operation("x", "1"), condition, CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, answer);
        assertEquals(Optional.empty(), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void readAtPastFrontierIsPartial() {
        surface.write(new Operation("x", "1"), unread(), CONTEXT);
        surface.write(new Operation("x", "2"), new Condition(Frontier.empty().advance("stub", 1), Optional.of("1")),
                CONTEXT);
        var past = Frontier.empty().advance("stub", 1);
        var request = new ReadRequest("x", new ReadTime(Optional.of(past), TimeAxis.RECORDED, Meaning.THEN));

        var result = surface.read(request, CONTEXT);

        assertInstanceOf(Completeness.Partial.class, result.completeness());
        assertEquals(Frontier.empty().advance("stub", 2), result.seen());
    }

    @Test
    void staleFrontierAloneIsRejected() {
        surface.write(new Operation("y", "1"), unread(), CONTEXT);

        var answer = surface.write(new Operation("x", "1"), unread(), CONTEXT);

        assertInstanceOf(WriteAnswer.Rejected.class, answer);
        assertEquals(Optional.empty(), surface.read(readLatest("x"), CONTEXT).value());
    }

    @Test
    void nullActionIdLeavesSurfaceUnchanged() {
        Actions nullForWrites = (context, call) -> call.equals("write") ? null : new EventId("r");
        var broken = new InMemorySurface(nullForWrites);

        assertThrows(NullPointerException.class, () -> broken.write(new Operation("x", "1"), unread(), CONTEXT));

        var result = broken.read(readLatest("x"), CONTEXT);
        assertEquals(Optional.empty(), result.value());
        assertEquals(Frontier.empty(), result.seen());
    }
}
