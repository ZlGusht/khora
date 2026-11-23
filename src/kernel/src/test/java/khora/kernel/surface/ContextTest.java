package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import khora.kernel.event.By;
import org.junit.jupiter.api.Test;

class ContextTest {

    @Test
    void aCallThroughASurfaceIsByItsCaller() {
        var context = new Context("anna", Optional.of("ivan"), "g3", 10);
        assertEquals(new By("anna", Optional.of("ivan"), "g3", "projects"), context.by("projects"));
    }

    @Test
    void negativeBudgetIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Context("anna", Optional.empty(), "g3", -1));
    }
}
