package khora.surfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import khora.events.By;
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
