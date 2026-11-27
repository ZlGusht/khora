package khora.query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import khora.events.Assertion;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;

/**
 * A search as data: an envelope around a body of operators (ADR-0XX decisions 1 to 3). It is the
 * shape a planner hands to datastores ([[Stage 1's tasks and gate are stated in events]]).
 *
 * <p>A query is checked when it is built, so one that exists is valid: its {@code returns} are
 * columns of its body, its order sorts returned columns, and a page has an order to page through.
 * Its canonical JSON ({@link #toJson}) is how it is stored: as the value of the {@code khora/query}
 * attribute ({@link #stored}), like any other statement.
 *
 * <p>Provisional: the envelope's time is a frontier only; reads by recorded or occurred time wait
 * for K7. No annotations yet. The JSON carries no version of its own.
 *
 * @param body what to compute
 * @param at the frontier to answer at; absent, the datastore answers at everything it holds
 * @param semantics whether repeated rows are kept
 * @param returns the columns of the answer, in order: columns of the body
 * @param order how the answer is sorted, by returned columns; empty leaves the datastore's order
 * @param page which part of the sorted answer to give
 */
public record Query(Operator body, Optional<Frontier> at, Semantics semantics, List<String> returns, List<Sort> order,
        Optional<Page> page) {

    /** The attribute whose value is a stored query's canonical JSON. */
    public static final Identity ATTRIBUTE = Identity.named("khora/query");

    public Query {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(semantics, "semantics");
        Objects.requireNonNull(page, "page");
        returns = Columns.distinct(returns, "return");
        if (returns.isEmpty()) {
            throw new IllegalArgumentException("a query returns at least one column");
        }
        Columns.within(returns, body.columns(), "return");
        order = List.copyOf(order);
        Columns.distinct(order.stream().map(Sort::column).toList(), "order");
        Columns.within(order.stream().map(Sort::column).toList(), returns, "order");
        if (page.isPresent() && order.isEmpty()) {
            throw new IllegalArgumentException("a page needs an order");
        }
    }

    /** Whether an answer keeps repeated rows. {@code set} drops them after every operator. */
    public enum Semantics {
        SET, MULTISET
    }

    /** Sorting by one returned column. */
    public record Sort(String column, boolean descending) {
        public Sort {
            Columns.check(column);
        }

        public static Sort ascending(String column) {
            return new Sort(column, false);
        }

        public static Sort descending(String column) {
            return new Sort(column, true);
        }
    }

    /** At most {@code limit} rows, after skipping {@code offset}. */
    public record Page(long limit, long offset) {
        public Page {
            if (limit < 1) {
                throw new IllegalArgumentException("a page's limit is at least 1: " + limit);
            }
            if (offset < 0) {
                throw new IllegalArgumentException("a page's offset is negative: " + offset);
            }
        }
    }

    /** {@code body} with every column returned, as a set, unsorted, at everything the datastore holds. */
    public static Query of(Operator body) {
        return new Query(body, Optional.empty(), Semantics.SET, body.columns(), List.of(), Optional.empty());
    }

    /** This query answered at {@code frontier}. */
    public Query at(Frontier frontier) {
        return new Query(body, Optional.of(frontier), semantics, returns, order, page);
    }

    /** This query with {@code semantics}. */
    public Query with(Semantics semantics) {
        return new Query(body, at, semantics, returns, order, page);
    }

    /** This query returning {@code columns}; an order on a column no longer returned is refused. */
    public Query returning(String... columns) {
        return new Query(body, at, semantics, List.of(columns), order, page);
    }

    /** This query sorted by {@code sorts}. */
    public Query orderedBy(Sort... sorts) {
        return new Query(body, at, semantics, returns, List.of(sorts), page);
    }

    /** This query's page of {@code limit} rows after {@code offset}. */
    public Query page(long limit, long offset) {
        return new Query(body, at, semantics, returns, order, Optional.of(new Page(limit, offset)));
    }

    /** The query as JSON; its canonical text is what {@link #stored} keeps. */
    public Json.Obj toJson() {
        return QueryJson.write(this);
    }

    /**
     * The query {@code json} describes.
     *
     * @throws IllegalArgumentException if it is not a valid query: an unknown or missing member, an
     *     unknown operator, or any check a query makes when it is built
     */
    public static Query fromJson(Json json) {
        return QueryJson.read(json);
    }

    /** The tuple that stores this query as {@code thing}'s {@link #ATTRIBUTE}. */
    public Assertion stored(Identity thing) {
        return Assertion.of(thing, ATTRIBUTE, new Assertion.Literal(new Json.Str(toJson().canonical())));
    }

    /**
     * The query a statement stores, as {@link #stored} writes it.
     *
     * @throws IllegalArgumentException if the statement is not of {@link #ATTRIBUTE}, or its value
     *     is not a valid query's JSON
     */
    public static Query from(Statement statement) {
        var tuple = statement.assertion();
        if (tuple.attribute().filter(ATTRIBUTE::equals).isEmpty()) {
            throw new IllegalArgumentException("not a stored query: " + statement.id().value());
        }
        if (!(tuple.value().orElse(null) instanceof Assertion.Literal(Json.Str text))) {
            throw new IllegalArgumentException("a stored query is a string literal: " + statement.id().value());
        }
        return fromJson(Json.parse(text.value()));
    }
}
