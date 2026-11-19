package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;
import khora.kernel.event.EventId;
import org.junit.jupiter.api.Test;

class ContractTypesTest {

    private static String describe(WriteAnswer answer) {
        return switch (answer) {
            case WriteAnswer.Unconfirmed u -> "unconfirmed";
            case WriteAnswer.Rejected r -> "rejected";
        };
    }

    private static String describe(Completeness completeness) {
        return switch (completeness) {
            case Completeness.Complete c -> "complete";
            case Completeness.Partial p -> "partial";
        };
    }

    @Test
    void writeAnswerSwitchCoversBothCases() {
        var action = new EventId("a1");
        assertEquals("unconfirmed", describe(new WriteAnswer.Unconfirmed(action)));
        assertEquals("rejected", describe(new WriteAnswer.Rejected(action, "stale")));
    }

    @Test
    void completenessSwitchCoversBothCases() {
        assertEquals("complete", describe(new Completeness.Complete()));
        assertEquals("partial", describe(new Completeness.Partial("frontier not reached")));
    }

    @Test
    void nullDeclarationFieldIsRefused() {
        var e = assertThrows(NullPointerException.class,
                () -> new Declaration(true, null, History.NONE, Reads.DETERMINISTIC, Writes.CONDITIONAL));
        assertTrue(e.getMessage().contains("effects"));
    }

    @Test
    void resultAnnotationsAreCopied() {
        var annotations = new ArrayList<Annotation>();
        annotations.add(new Annotation("as observed at 10:42"));
        var result = new Result(new EventId("a1"), Optional.empty(), new Completeness.Complete(),
                new Frontier("f1"), annotations);

        annotations.add(new Annotation("not reproducible"));

        assertEquals(1, result.annotations().size());
    }
}
