package khora.kernel.surface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReadTimeTest {

    @Test
    void latestReadsAtNoFixedFrontier() {
        assertTrue(ReadTime.latest().at().isEmpty());
    }
}
