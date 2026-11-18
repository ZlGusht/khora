package khora.kernel.log.nats;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * A NATS server in a container, with the image and config of {@code docker-compose.yml}.
 * The JetStream data directory is a host directory, so {@link #stop()} then {@link #start()}
 * restarts the server on the same data. The client port can change across a restart; ask
 * {@link #url()} again after {@link #start()}.
 */
public final class NatsTestServer implements AutoCloseable {

    public static final String IMAGE = "nats:2.12-alpine";
    private static final int CLIENT_PORT = 4222;
    private static final int MONITOR_PORT = 8222;

    private final Path dataDir;
    private GenericContainer<?> container;

    public NatsTestServer() {
        try {
            this.dataDir = Files.createTempDirectory("khora-nats-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public NatsTestServer start() {
        if (container != null) {
            return this;
        }
        GenericContainer<?> c = new GenericContainer<>(IMAGE)
                .withCopyFileToContainer(MountableFile.forHostPath(configPath()), "/etc/nats/nats-server.conf")
                .withFileSystemBind(dataDir.toAbsolutePath().toString(), "/data", BindMode.READ_WRITE)
                .withCommand("-c", "/etc/nats/nats-server.conf")
                .withExposedPorts(CLIENT_PORT, MONITOR_PORT)
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
        c.start();
        container = c;
        return this;
    }

    /** Stops the container. The data directory is kept for the next {@link #start()}. */
    public void stop() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    public String url() {
        if (container == null) {
            throw new IllegalStateException("NATS test server is not running");
        }
        return "nats://" + container.getHost() + ":" + container.getMappedPort(CLIENT_PORT);
    }

    /** The server's log output so far. */
    public String logs() {
        return container == null ? "" : container.getLogs();
    }

    /** Stops the container and deletes the data directory. */
    @Override
    public void close() {
        stop();
        try (Stream<Path> paths = Files.walk(dataDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path configPath() {
        String configured = System.getProperty("khora.nats.conf");
        Path path = configured != null ? Paths.get(configured) : Paths.get("../../nats/nats-server.conf");
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("NATS config not found: " + path.toAbsolutePath());
        }
        return path;
    }
}
