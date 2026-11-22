package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import khora.kernel.event.EventId;
import org.junit.jupiter.api.Test;

class ContextTest {

    @Test
    void contextHoldsItsParts() {
        var principal = new Principal("simeon");
        var grant = new Grant("grant-1");
        var budget = new Budget(10);

        var context = new Context(principal, grant, budget, Optional.empty());

        assertEquals(principal, context.principal());
        assertEquals(grant, context.grant());
        assertEquals(budget, context.budget());
        assertTrue(context.trigger().isEmpty());
    }

    @Test
    void nullPrincipalIsRefused() {
        var e = assertThrows(NullPointerException.class,
                () -> new Context(null, new Grant("g"), new Budget(0), Optional.empty()));
        assertTrue(e.getMessage().contains("principal"));
    }

    @Test
    void malformedEventIdIsRefused() {
        for (var value : new String[] {" ", "a1", "sha256:XYZ", EventId.hashOf("x").value() + "/0"}) {
            assertThrows(IllegalArgumentException.class, () -> new EventId(value), value);
        }
    }

    @Test
    void negativeBudgetIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Budget(-1));
    }
}
