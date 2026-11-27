package khora.query;

/**
 * Where queries are answered. A datastore holds what the events it has taken in say, and answers
 * a query at the query's frontier, or at everything it holds when the query names none.
 *
 * <p>Provisional: one datastore answers a whole query; planning across datastores and pushdown
 * come with the second datastore (P2). Every datastore passes {@code DatastoreContract}.
 */
public interface Datastore {

    /**
     * The answer to {@code query}.
     *
     * @throws IllegalArgumentException if the query's frontier holds an event the datastore lacks
     * @throws ArithmeticException if a {@code sum} leaves the range of a 64-bit integer
     */
    Answer answer(Query query);
}
