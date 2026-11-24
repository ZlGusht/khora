package khora.query;

import java.util.Comparator;
import java.util.Optional;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Assertion.Value;
import khora.events.json.Json;

/**
 * How values order and compare.
 *
 * <p>Provisional: without kinds, values have no declared type; the order is by what each value is.
 */
public final class Values {
    /** Integers by number, then strings by UTF-16 code units, then references by identity. */
    public static final Comparator<Value> ORDER = Comparator.comparingInt(Values::rank).thenComparing(Values::within);

    private Values() {}

    /** Whether {@code left op right} holds, as {@link Condition.Compare} says. */
    public static boolean compare(Value left, Condition.Op op, Value right) {
        return switch (op) {
            case EQ -> left.equals(right);
            case NE -> !left.equals(right);
            case LT, LE, GT, GE -> {
                if (rank(left) != rank(right) || left instanceof Ref) {
                    yield false;
                }
                int c = ORDER.compare(left, right);
                yield switch (op) {
                    case LT -> c < 0;
                    case LE -> c <= 0;
                    case GT -> c > 0;
                    default -> c >= 0;
                };
            }
        };
    }

    /** The integer {@code value} holds, if it is an integer literal. */
    static Optional<Long> integer(Value value) {
        return value instanceof Literal(Json.Int n) ? Optional.of(n.value()) : Optional.empty();
    }

    private static int rank(Value value) {
        return switch (value) {
            case Literal(Json.Int n) -> 0;
            case Literal l -> 1;
            case Ref r -> 2;
        };
    }

    private static int within(Value a, Value b) {
        return switch (a) {
            case Literal(Json.Int n) -> Long.compare(n.value(), ((Json.Int) ((Literal) b).value()).value());
            case Literal(Json.Str s) -> s.value().compareTo(((Json.Str) ((Literal) b).value()).value());
            case Literal l -> throw new IllegalStateException("a literal is a string or an integer");
            case Ref r -> r.id().value().compareTo(((Ref) b).id().value());
        };
    }
}
