package khora.query;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Assertion.Value;
import khora.events.Identity;

/**
 * What a {@link Operator.Filter} tests on each row. Negation is not a condition: it is the
 * {@link Operator.Not} operator (ADR-0XX decision 4).
 *
 * <p>Provisional: no text matching; that is {@code Search}, a capability operator for later.
 */
public sealed interface Condition permits Condition.Has, Condition.Compare, Condition.And, Condition.Or {

    /** The columns the condition reads. */
    Set<String> columns();

    /**
     * The thing in {@code column} has a statement with {@code attribute} that holds, and, if given,
     * with {@code value}.
     */
    record Has(String column, Identity attribute, Optional<Value> value) implements Condition {
        public Has {
            Columns.check(column);
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(value, "value");
        }

        public Has(String column, Identity attribute) {
            this(column, attribute, Optional.empty());
        }

        public Has(String column, Identity attribute, Value value) {
            this(column, attribute, Optional.of(value));
        }

        @Override
        public Set<String> columns() {
            return Set.of(column);
        }
    }

    /**
     * The value in {@code column} compared with {@code value}. {@code eq} and {@code ne} hold for
     * any two values; the others only between two integers or two strings, and are false otherwise.
     */
    record Compare(String column, Op op, Value value) implements Condition {
        public Compare {
            Columns.check(column);
            Objects.requireNonNull(op, "op");
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Set<String> columns() {
            return Set.of(column);
        }
    }

    /** How {@link Compare} compares. */
    enum Op {
        EQ, NE, LT, LE, GT, GE
    }

    /** Every condition holds. */
    record And(List<Condition> all) implements Condition {
        public And {
            all = nonEmpty(all, "and");
        }

        @Override
        public Set<String> columns() {
            return union(all);
        }
    }

    /** At least one condition holds. */
    record Or(List<Condition> any) implements Condition {
        public Or {
            any = nonEmpty(any, "or");
        }

        @Override
        public Set<String> columns() {
            return union(any);
        }
    }

    private static List<Condition> nonEmpty(List<Condition> conditions, String what) {
        var copy = List.copyOf(conditions);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(what + " needs at least one condition");
        }
        return copy;
    }

    private static Set<String> union(List<Condition> conditions) {
        var columns = new LinkedHashSet<String>();
        conditions.forEach(c -> columns.addAll(c.columns()));
        return columns;
    }
}
