package khora.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import khora.events.Identity;

/**
 * One building block of a query's body: rows in, rows out (ADR-0XX decision 3). A row maps each of
 * the operator's {@link #columns} to a value, a literal or a reference. Each operator checks, when
 * it is built, that it uses only columns its input gives, so a body that can be built is valid.
 *
 * <p>Provisional: {@code Propagate}, joins and paths come in stage 2; {@code Search} and other
 * capability operators with the second datastore.
 */
public sealed interface Operator permits Operator.Scan, Operator.Filter, Operator.Not, Operator.Aggregate {

    /** The columns of the rows this operator gives, in order. */
    List<String> columns();

    /**
     * Without an attribute: one row per thing an empty tuple brought into being, {@code as} holding a
     * reference to it. With one: one row per statement with that attribute, {@code as} holding a
     * reference to the thing the statement is about and {@code value} its value. Only statements
     * that hold at the frontier count.
     */
    record Scan(String as, Optional<Identity> attribute, Optional<String> value) implements Operator {
        public Scan {
            Columns.check(as);
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(value, "value");
            if (attribute.isPresent() != value.isPresent()) {
                throw new IllegalArgumentException("a scan of statements names both an attribute and a value column");
            }
            value.ifPresent(Columns::check);
            if (value.filter(as::equals).isPresent()) {
                throw new IllegalArgumentException("a scan names column " + as + " twice");
            }
        }

        /** Every thing, in column {@code as}. */
        public static Scan things(String as) {
            return new Scan(as, Optional.empty(), Optional.empty());
        }

        /** Every statement with {@code attribute}: the thing in {@code as}, the value in {@code value}. */
        public static Scan statements(String as, Identity attribute, String value) {
            return new Scan(as, Optional.of(attribute), Optional.of(value));
        }

        @Override
        public List<String> columns() {
            return value.map(v -> List.of(as, v)).orElse(List.of(as));
        }
    }

    /** The rows of {@code input} that {@code where} holds for. */
    record Filter(Operator input, Condition where) implements Operator {
        public Filter {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(where, "where");
            Columns.within(where.columns(), input.columns(), "a filter");
        }

        @Override
        public List<String> columns() {
            return input.columns();
        }
    }

    /**
     * The rows of {@code input} that no row of {@code minus} matches. Rows match when they agree on
     * every column of {@code minus}; each of those must be a column of {@code input}.
     */
    record Not(Operator input, Operator minus) implements Operator {
        public Not {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(minus, "minus");
            Columns.within(minus.columns(), input.columns(), "a not");
        }

        @Override
        public List<String> columns() {
            return input.columns();
        }
    }

    /**
     * The rows of {@code input} grouped by {@code group}, one row per group: the group's values and,
     * in {@code as}, {@code function} over the group's values of {@code of}. {@code count} counts
     * rows and takes no {@code of}; the others need one. Without groups and without rows,
     * {@code count} and {@code sum} give one row of 0, {@code min} and {@code max} none.
     */
    record Aggregate(Operator input, List<String> group, Function function, Optional<String> of, String as)
            implements Operator {
        public Aggregate {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(function, "function");
            Objects.requireNonNull(of, "of");
            group = Columns.distinct(group, "an aggregate's group");
            Columns.within(group, input.columns(), "an aggregate");
            if ((function == Function.COUNT) == of.isPresent()) {
                throw new IllegalArgumentException(function == Function.COUNT
                        ? "count takes no column" : function.name().toLowerCase() + " needs a column");
            }
            of.ifPresent(column -> Columns.within(List.of(Columns.check(column)), input.columns(), "an aggregate"));
            Columns.check(as);
            if (group.contains(as)) {
                throw new IllegalArgumentException("an aggregate's result column " + as + " is also a group column");
            }
        }

        /** Counts the rows of each group into {@code as}. */
        public static Aggregate count(Operator input, List<String> group, String as) {
            return new Aggregate(input, group, Function.COUNT, Optional.empty(), as);
        }

        /** {@code function} over column {@code of} of each group, into {@code as}. */
        public static Aggregate of(Operator input, List<String> group, Function function, String of, String as) {
            return new Aggregate(input, group, function, Optional.of(of), as);
        }

        @Override
        public List<String> columns() {
            var columns = new ArrayList<>(group);
            columns.add(as);
            return List.copyOf(columns);
        }
    }

    /**
     * What an {@link Aggregate} computes. {@code sum} adds the integers and skips other values;
     * {@code min} and {@code max} follow {@link Values#ORDER}.
     */
    enum Function {
        COUNT, SUM, MIN, MAX
    }
}
