package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BasicTest {

    @Test
    void wellKnownIdsAreHashesOfTheirNames() {
        assertEquals(Identity.named("khora/basic/assert"), Basic.ASSERT);
        assertEquals(Identity.named("khora/basic/retracted"), Basic.RETRACTED);
        assertEquals(Identity.named("khora/basic/label"), Basic.LABEL);
    }
}
