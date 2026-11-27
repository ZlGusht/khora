package khora.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Column names and the checks on them. A column is a lowercase name, {@code [a-z_][a-z0-9_]*}, at
 * most 64 characters.
 */
final class Columns {
    private static final Pattern NAME = Pattern.compile("[a-z_][a-z0-9_]{0,63}");

    private Columns() {}

    /** {@code name} if it is a column name. */
    static String check(String name) {
        Objects.requireNonNull(name, "column");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("not a column name: " + name);
        }
        return name;
    }

    /** {@code names} as an unmodifiable list, if each is a column name and none repeats. */
    static List<String> distinct(List<String> names, String what) {
        var copy = List.copyOf(names);
        var seen = new HashSet<String>();
        for (var name : copy) {
            if (!seen.add(check(name))) {
                throw new IllegalArgumentException(what + " names column " + name + " twice");
            }
        }
        return copy;
    }

    /** Refuses a column of {@code used} that {@code given} does not hold. */
    static void within(Collection<String> used, List<String> given, String what) {
        for (var name : used) {
            if (!given.contains(name)) {
                throw new IllegalArgumentException(what + " uses column " + name + ", which its input does not give "
                        + given);
            }
        }
    }
}
