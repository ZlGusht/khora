package khora.kernel.log.nats;

import io.nats.client.support.JsonParser;
import io.nats.client.support.JsonValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * {@link #url()} again after {@link #start()}. Every start checks that the server syncs every
 * write ({@code jetstream.config.sync_always} in {@code /varz}).
 */
public final class NatsTestServer implements AutoCloseable {

    /**
     * Pinned, as in {@code docker-compose.yml}: {@link NatsLogStore}'s duplicate handling depends on
     * server behaviour observed on 2.12.15 (error 10201 for a batch holding a stored event id; the
     * duplicate check runs before the expected-last-msg-id check). {@code AtomicBatchSpikeTest} pins it.
     */
    public static final String IMAGE = "nats:2.12.15-alpine";
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
        requireSyncAlways();
        return this;
    }

    /** Fails unless the server's {@code /varz} reports {@code jetstream.config.sync_always: true}. */
    private void requireSyncAlways() {
        String varz = monitorUrl() + "/varz";
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(varz)).build(), HttpResponse.BodyHandlers.ofString());
            JsonValue syncAlways = JsonParser.parse(response.body())
                    .map.get("jetstream").map.get("config").map.get("sync_always");
            if (syncAlways == null || !Boolean.TRUE.equals(syncAlways.bool)) {
                throw new IllegalStateException("NATS test server does not sync every write: "
                        + varz + " reports jetstream.config.sync_always = " + syncAlways);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted reading " + varz, e);
        }
    }

    /** Stops the container. The data directory is kept for the next {@link #start()}. */
    public void stop() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    /**
     * Kills the server process without a graceful shutdown ({@code docker kill}), then removes the
     * container. The data directory is kept for the next {@link #start()}.
     */
    public void kill() {
        if (container != null) {
            container.getDockerClient().killContainerCmd(container.getContainerId()).exec();
            stop();
        }
    }

    /** Freezes the server process ({@code docker pause}): connections stay open but get no answer. */
    public void pause() {
        requireRunning();
        container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
    }

    /** Resumes a server frozen by {@link #pause()}. */
    public void unpause() {
        requireRunning();
        container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
    }

    private void requireRunning() {
        if (container == null) {
            throw new IllegalStateException("NATS test server is not running");
        }
    }

    public String url() {
        if (container == null) {
            throw new IllegalStateException("NATS test server is not running");
        }
        return "nats://" + container.getHost() + ":" + container.getMappedPort(CLIENT_PORT);
    }

    /** The base URL of the server's HTTP monitor. */
    public String monitorUrl() {
        if (container == null) {
            throw new IllegalStateException("NATS test server is not running");
        }
        return "http://" + container.getHost() + ":" + container.getMappedPort(MONITOR_PORT);
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
        Path path = configured != null ? Paths.get(configured) : Paths.get("../../nats/nats-server-always-sync.conf");
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("NATS config not found: " + path.toAbsolutePath());
        }
        return path;
    }
}
