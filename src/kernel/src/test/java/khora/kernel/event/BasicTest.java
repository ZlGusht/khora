package khora.kernel.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import khora.kernel.json.Json;
import org.junit.jupiter.api.Test;

class BasicTest {

    static final EventId X = EventId.hashOf("x");
    static final EventId Y = EventId.hashOf("y");
    static final EventId Z = EventId.hashOf("z");

    @Test
    void wellKnownIdsAreHashesOfTheirNames() {
        assertEquals(EventId.hashOf("khora/basic/assert"), Basic.ASSERT);
        assertEquals(EventId.hashOf("khora/basic/retracted"), Basic.RETRACTED);
    }

    @Test
    void assertsBuildPinnedArgs() {
        assertEquals(Basic.ASSERT, Basic.entity().definition());
        assertEquals("{}", Basic.entity().args().canonical());
        assertEquals("{\"attribute\":\"" + Y.value() + "\",\"thing\":\"" + X.value() + "\",\"value\":5}",
                Basic.property(X, Y, new Json.Int(5)).args().canonical());
        assertEquals("{\"from\":\"" + X.value() + "\",\"relator\":\"" + Z.value() + "\",\"to\":\"" + Y.value() + "\"}",
                Basic.relationship(X, Y, Z).args().canonical());
        assertEquals("{\"behavior\":\"" + X.value() + "\",\"thing\":\"" + Y.value() + "\"}",
                Basic.binding(X, Y).args().canonical());
    }

    @Test
    void retractionIsAPropertyWithTheRetractedAttribute() {
        assertEquals(Basic.property(X, Basic.RETRACTED, new Json.Str("true")), Basic.retraction(X));
    }
}
