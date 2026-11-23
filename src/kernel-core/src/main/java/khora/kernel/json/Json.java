package khora.kernel.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A JSON value with a canonical form (RFC 8785), used to hash events.
 *
 * <p>Provisional: only objects, arrays, strings and 64-bit integers, which is all events need for
 * now ([[Event ids are salted hashes of canonical JSON]]).
 */
public sealed interface Json permits Json.Obj, Json.Arr, Json.Str, Json.Int {

    /** The canonical text: no whitespace, members sorted by UTF-16 code units, RFC 8785 escapes. */
    default String canonical() {
        var out = new StringBuilder();
        write(this, out);
        return out.toString();
    }

    /** Parses JSON text made of objects, arrays, strings and integers only. */
    static Json parse(String text) {
        return new JsonParser(text).parseDocument();
    }

    /** An object, its members kept sorted by key. */
    record Obj(SortedMap<String, Json> members) implements Json {
        public Obj {
            Objects.requireNonNull(members, "members");
            var copy = new TreeMap<String, Json>();
            members.forEach((key, value) -> copy.put(
                    checkUnicode(Objects.requireNonNull(key, "key")), Objects.requireNonNull(value, "value")));
            members = Collections.unmodifiableSortedMap(copy);
        }

        /** An object with the given members, in any order. */
        public static Obj of(Map<String, Json> members) {
            return new Obj(new TreeMap<>(members));
        }

        public Optional<Json> get(String key) {
            return Optional.ofNullable(members.get(key));
        }

        public String str(String key) {
            return member(key, Str.class).value();
        }

        public long integer(String key) {
            return member(key, Int.class).value();
        }

        public Obj obj(String key) {
            return member(key, Obj.class);
        }

        public Arr arr(String key) {
            return member(key, Arr.class);
        }

        private <T extends Json> T member(String key, Class<T> type) {
            var value = members.get(key);
            if (value == null) {
                throw new IllegalArgumentException("missing member " + key);
            }
            if (!type.isInstance(value)) {
                throw new IllegalArgumentException("member " + key + " is not " + type.getSimpleName());
            }
            return type.cast(value);
        }
    }

    /** An array. */
    record Arr(List<Json> items) implements Json {
        public Arr {
            items = List.copyOf(items);
        }
    }

    /** A string. */
    record Str(String value) implements Json {
        public Str {
            checkUnicode(Objects.requireNonNull(value, "value"));
        }
    }

    /** A 64-bit integer. */
    record Int(long value) implements Json {}

    /** Refuses lone surrogates: canonical JSON needs valid Unicode, and UTF-8 cannot encode them. */
    private static String checkUnicode(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                throw new IllegalArgumentException("lone surrogate at " + i);
            }
        }
        return s;
    }

    private static void write(Json value, StringBuilder out) {
        switch (value) {
            case Obj o -> {
                out.append('{');
                var first = true;
                for (var member : o.members().entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    writeString(member.getKey(), out);
                    out.append(':');
                    write(member.getValue(), out);
                }
                out.append('}');
            }
            case Arr a -> {
                out.append('[');
                for (int i = 0; i < a.items().size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    write(a.items().get(i), out);
                }
                out.append(']');
            }
            case Str s -> writeString(s.value(), out);
            case Int n -> out.append(n.value());
        }
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
