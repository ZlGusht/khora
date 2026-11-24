package khora.query;

import java.util.List;
import java.util.Objects;
import khora.events.Assertion.Value;
import khora.events.Frontier;

/**
 * What a datastore answers to a query: the frontier it answered at, the returned columns, and one
 * list of values per row, in the columns' order.
 *
 * <p>Provisional: no annotations (confidence, age) yet.
 */
public record Answer(Frontier at, List<String> columns, List<List<Value>> rows) {
    public Answer {
        Objects.requireNonNull(at, "at");
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
        for (var row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("a row has " + row.size() + " values for " + columns.size() + " columns");
            }
        }
    }
}
