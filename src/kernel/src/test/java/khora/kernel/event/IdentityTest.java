package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityTest {

    @Test
    void anAssertionsIdentityIsItsEventAndIndex() {
        var e = EventId.hashOf("e");
        assertEquals(e.value() + "/2", Identity.of(e, 2).value());
    }

    @Test
    void aNamedIdentityIsTheHashOfItsName() {
        assertEquals(EventId.hashOf("khora/basic/label").value(), Identity.named("khora/basic/label").value());
    }

    @Test
    void malformedIdentitiesAreRefused() {
        var e = EventId.hashOf("e").value();
        for (var value : List.of("", "x", "sha256:XYZ", e + "/", e + "/-1", e + "/01", e + "/1/2")) {
            assertThrows(IllegalArgumentException.class, () -> new Identity(value), value);
        }
        assertThrows(IllegalArgumentException.class, () -> Identity.of(EventId.hashOf("e"), -1));
    }
}
