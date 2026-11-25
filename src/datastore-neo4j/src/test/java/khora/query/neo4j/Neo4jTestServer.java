package khora.query.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * One Neo4j server in a container for all tests of a JVM, with the image of
 * {@code docker-compose.yml}. Each test clears the datastore it uses.
 */
final class Neo4jTestServer {
    /** Pinned, as in {@code docker-compose.yml}. Upgrade both together. */
    static final String IMAGE = "neo4j:2026.10.2-community";
    private static final String PASSWORD = "khora-test-password";

    private static Driver driver;

    private Neo4jTestServer() {}

    /** The driver to the shared server, started on first use and stopped when the JVM exits. */
    static synchronized Driver driver() {
        if (driver == null) {
            var container = new Neo4jContainer(IMAGE).withAdminPassword(PASSWORD);
            container.start();
            driver = GraphDatabase.driver(container.getBoltUrl(), AuthTokens.basic("neo4j", PASSWORD));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                driver.close();
                container.stop();
            }));
        }
        return driver;
    }

    /** An empty datastore on the shared server. */
    static Neo4jDatastore empty() {
        var store = new Neo4jDatastore(driver(), "neo4j");
        store.clear();
        return store;
    }
}
