package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BasicTest {

    @Test
    void wellKnownIdsAreHashesOfTheirNames() {
        assertEquals(EventId.hashOf("khora/basic/assert"), Basic.ASSERT);
        assertEquals(EventId.hashOf("khora/basic/retracted"), Basic.RETRACTED);
        assertEquals(EventId.hashOf("khora/basic/label"), Basic.LABEL);
    }
}
