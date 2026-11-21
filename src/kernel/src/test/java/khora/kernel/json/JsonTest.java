package khora.kernel.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class JsonTest {

    static Json.Obj obj(Map<String, Json> members) {
        return new Json.Obj(new TreeMap<>(members));
    }

    @Test
    void canonicalSortsKeysAndHasNoWhitespace() {
        var value = obj(Map.of(
                "b", new Json.Int(1),
                "a", new Json.Arr(List.of(new Json.Int(2), new Json.Str("x")))));
        assertEquals("{\"a\":[2,\"x\"],\"b\":1}", value.canonical());
    }

    @Test
    void canonicalEscapesAsRfc8785() {
        var value = new Json.Str("\"\\\n\t\u0001é");
        assertEquals("\"\\\"\\\\\\n\\t\\u0001é\"", value.canonical());
    }

    @Test
    void keysSortByUtf16CodeUnits() {
        var value = obj(Map.of("é", new Json.Int(1), "z", new Json.Int(2), "😀", new Json.Int(3)));
        assertEquals("{\"z\":2,\"é\":1,\"😀\":3}", value.canonical());
    }

    @Test
    void integersAtLimitsRoundTrip() {
        for (long n : new long[] {Long.MIN_VALUE, Long.MAX_VALUE}) {
            var value = new Json.Int(n);
            assertEquals(value, Json.parse(value.canonical()));
        }
    }

    @Test
    void parseRoundTripsCanonicalText() {
        var value = obj(Map.of(
                "list", new Json.Arr(List.of(new Json.Str("a\u0000b😀"), obj(Map.of()))),
                "n", new Json.Int(-42),
                "nested", obj(Map.of("k", new Json.Arr(List.of())))));
        assertEquals(value, Json.parse(value.canonical()));
    }

    @Test
    void parseAcceptsWhitespaceAndEscapes() {
        assertEquals(obj(Map.of("a", new Json.Str("😀"))), Json.parse(" { \"a\" : \"\\ud83d\\ude00\" } "));
    }

    @Test
    void parseRefusesOtherValues() {
        for (String text : List.of("1.5", "1e3", "true", "null", "{\"a\":1,\"a\":2}", "{} x",
                "92233720368547758070", "-", "\"open", "[1,]")) {
            assertThrows(IllegalArgumentException.class, () -> Json.parse(text), text);
        }
    }

    @Test
    void helpersRefuseMissingOrMistypedMembers() {
        var value = obj(Map.of("s", new Json.Str("x"), "n", new Json.Int(1)));
        assertEquals("x", value.str("s"));
        assertEquals(1L, value.integer("n"));
        assertThrows(IllegalArgumentException.class, () -> value.str("n"));
        assertThrows(IllegalArgumentException.class, () -> value.integer("missing"));
        assertThrows(IllegalArgumentException.class, () -> value.obj("s"));
        assertThrows(IllegalArgumentException.class, () -> value.arr("s"));
    }

    @Test
    void loneSurrogatesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Json.Str("a\uD800"));
        assertThrows(IllegalArgumentException.class, () -> new Json.Str("\uDC00b"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"\\ud800\""));
        assertThrows(IllegalArgumentException.class, () -> Json.Obj.of(Map.of("\uDC00", new Json.Int(1))));
        assertEquals("😀", new Json.Str("😀").value());
    }

    @Test
    void deepNestingIsRefusedNotOverflowed() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[".repeat(200_000)));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":".repeat(200_000)));
    }
}
