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
        assertTrue(context.cause().isEmpty());
    }

    @Test
    void nullPrincipalIsRefused() {
        var e = assertThrows(NullPointerException.class,
                () -> new Context(null, new Grant("g"), new Budget(0), Optional.empty()));
        assertTrue(e.getMessage().contains("principal"));
    }

    @Test
    void blankEventIdIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EventId(" "));
    }

    @Test
    void negativeBudgetIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Budget(-1));
    }
}
