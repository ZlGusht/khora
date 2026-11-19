package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrontierTest {

    @Test
    void blankFrontierIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Frontier(" "));
    }

    @Test
    void frontiersWithTheSameValueAreEqual() {
        assertEquals(new Frontier("f1"), new Frontier("f1"));
    }

    @Test
    void latestReadsAtNoFixedFrontier() {
        assertTrue(ReadTime.latest().at().isEmpty());
    }
}
