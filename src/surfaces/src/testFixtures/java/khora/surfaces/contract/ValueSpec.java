package khora.surfaces.contract;

import java.util.Objects;
import khora.events.json.Json;

/** The value of a tuple in a scenario: a literal, or a reference. */
public sealed interface ValueSpec {
    /** A literal string or integer. */
    record Literal(Json value) implements ValueSpec {
        public Literal {
            Objects.requireNonNull(value, "value");
            if (!(value instanceof Json.Str || value instanceof Json.Int)) {
                throw new IllegalArgumentException("a literal is a string or an integer");
            }
        }
    }

    /** A reference to an identity. */
    record RefValue(Ref ref) implements ValueSpec {
        public RefValue {
            Objects.requireNonNull(ref, "ref");
        }
    }

    static ValueSpec text(String text) {
        return new Literal(new Json.Str(text));
    }

    static ValueSpec integer(long number) {
        return new Literal(new Json.Int(number));
    }

    static ValueSpec ref(Ref ref) {
        return new RefValue(ref);
    }
}
