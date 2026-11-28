package khora.query;

/**
 * Where queries are answered. A datastore is a surface's reading of the events it has taken in, and
 * answers a query at the query's log position, or at everything it has taken in when the query names
 * none.
 *
 * <p>Provisional: one datastore answers a whole query; planning across datastores and pushdown
 * come with the second datastore (P2). Every datastore passes {@code DatastoreContract}.
 */
public interface Datastore {

    /**
     * The answer to {@code query}.
     *
     * @throws IllegalArgumentException if the query's position is past what the datastore has taken in
     * @throws ArithmeticException if a {@code sum} leaves the range of a 64-bit integer
     */
    Answer answer(Query query);
}
