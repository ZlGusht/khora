package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrontierTest {

    @Test
    void emptyReachesEmpty() {
        assertTrue(Frontier.empty().reaches(Frontier.empty()));
    }

    @Test
    void laterReachesEarlier() {
        var later = Frontier.empty().advance("a", 5);
        var earlier = Frontier.empty().advance("a", 3);
        assertTrue(later.reaches(earlier));
    }

    @Test
    void earlierDoesNotReachLater() {
        var earlier = Frontier.empty().advance("a", 3);
        var later = Frontier.empty().advance("a", 5);
        assertFalse(earlier.reaches(later));
    }

    @Test
    void missingOriginIsNotReached() {
        var a = Frontier.empty().advance("a", 5);
        var b = Frontier.empty().advance("b", 1);
        assertFalse(a.reaches(b));
    }

    @Test
    void negativePositionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Frontier(Map.of("a", -1L)));
    }

    @Test
    void positionsAreCopied() {
        var positions = new HashMap<String, Long>();
        positions.put("a", 1L);
        var frontier = new Frontier(positions);

        positions.put("a", 9L);

        assertEquals(1L, frontier.positions().get("a"));
    }

    @Test
    void latestReadsAtNoFixedFrontier() {
        var latest = ReadTime.latest();
        assertTrue(latest.at().isEmpty());
        assertEquals(TimeAxis.RECORDED, latest.by());
        assertEquals(Meaning.THEN, latest.meaning());
    }
}
