package khora.events.json;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A recursive-descent parser for the JSON subset {@link Json} holds.
 *
 * <p>Provisional: refuses numbers that are not 64-bit integers, booleans, null, and nesting deeper
 * than {@link #MAX_DEPTH}.
 */
final class JsonParser {
    /** Deeper input is refused rather than overflowing the stack. */
    static final int MAX_DEPTH = 1000;

    private final String text;
    private int pos;
    private int depth;

    JsonParser(String text) {
        this.text = text;
    }

    Json parseDocument() {
        var value = parseValue();
        skipWhitespace();
        if (pos != text.length()) {
            throw error("trailing text");
        }
        return value;
    }

    private Json parseValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{', '[' -> {
                if (++depth > MAX_DEPTH) {
                    throw error("nested deeper than " + MAX_DEPTH);
                }
                var nested = c == '{' ? (Json) parseObject() : parseArray();
                depth--;
                yield nested;
            }
            case '"' -> new Json.Str(parseString());
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield parseInteger();
                }
                throw error("unexpected character '" + c + "'");
            }
        };
    }

    private Json.Obj parseObject() {
        pos++;
        var members = new TreeMap<String, Json>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return new Json.Obj(members);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("expected a key");
            }
            var key = parseString();
            skipWhitespace();
            expect(':');
            var value = parseValue();
            if (members.put(key, value) != null) {
                throw error("duplicate key " + key);
            }
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return new Json.Obj(members);
            }
            if (c != ',') {
                throw error("expected ',' or '}'");
            }
        }
    }

    private Json.Arr parseArray() {
        pos++;
        var items = new ArrayList<Json>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return new Json.Arr(items);
        }
        while (true) {
            items.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return new Json.Arr(items);
            }
            if (c != ',') {
                throw error("expected ',' or ']'");
            }
        }
    }

    private String parseString() {
        pos++;
        var out = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("unescaped control character");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = next();
            switch (e) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("short \\u escape");
                    }
                    int code = 0;
                    for (int i = 0; i < 4; i++) {
                        int digit = Character.digit(text.charAt(pos + i), 16);
                        if (digit < 0 || text.charAt(pos + i) >= 0x80) {
                            throw error("bad \\u escape");
                        }
                        code = code * 16 + digit;
                    }
                    out.append((char) code);
                    pos += 4;
                }
                default -> throw error("bad escape \\" + e);
            }
        }
    }

    private Json.Int parseInteger() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        int digits = pos;
        while (pos < text.length() && Character.isDigit(text.charAt(pos)) && text.charAt(pos) < 0x80) {
            pos++;
        }
        if (pos == digits) {
            throw error("expected digits");
        }
        if (text.charAt(digits) == '0' && pos - digits > 1) {
            throw error("leading zero");
        }
        if (digits > start && pos - digits == 1 && text.charAt(digits) == '0') {
            throw error("-0 is not an integer of its own");
        }
        if (pos < text.length() && (text.charAt(pos) == '.' || text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            throw error("only integers are allowed");
        }
        try {
            return new Json.Int(Long.parseLong(text.substring(start, pos)));
        } catch (NumberFormatException ex) {
            throw error("integer out of range");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            pos++;
        }
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private char next() {
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        return text.charAt(pos++);
    }

    private void expect(char c) {
        if (next() != c) {
            throw error("expected '" + c + "'");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at " + pos);
    }
}
