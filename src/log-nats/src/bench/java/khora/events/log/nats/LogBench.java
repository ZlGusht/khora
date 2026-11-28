package khora.events.log.nats;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.support.JsonParser;
import io.nats.client.support.JsonValue;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Optional;
import khora.events.Emitted;
import khora.events.Assertion;
import khora.events.By;
import khora.events.Event;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;

/**
 * Measures {@link NatsEventLog} on one NATS server and prints one Markdown table, also written to
 * {@code build/bench/results-<host>-<port>.md}.
 *
 * <p>Every scenario starts from an empty stream: the bench deletes the whole {@value NatsEventLog#STREAM}
 * stream through the JetStream management API (the stream denies deleting messages, not deleting the
 * stream) and {@link NatsEventLog#open} creates it again. So run it only against a server whose log
 * may be thrown away, such as the {@code nats-default-sync} service of {@code docker-compose.yml}
 * (port 4223). It refuses to start without {@code --wipe}, which says so.
 *
 * <p>Arguments: {@code --url nats://host:port} (required); {@code --wipe} (required: agrees that the
 * log on that server is deleted); {@code --monitor http://host:port} to read
 * the server's sync setting from {@code /varz}; {@code --appends N} (10 000); {@code --warmup N}
 * (1 000); {@code --history-log N} (1 000 000, a multiple of 100 000).
 */
public final class LogBench {

    private static final int BODY_BYTES = 200;
    private static final int FILL_BATCH = 1000;
    private static final int HISTORY_READS = 20;
    private static final int SMALL_ENTITY = 1_000;
    private static final int LARGE_ENTITY = 100_000;
    private static final int ENTITIES = 1000;
    private static final By BY = new By("bench", Optional.empty(), "bench", "bench");
    private static final Identity VALUE = Identity.named("bench/value");
    private static final String WIPE = "--wipe";

    private final String url;
    private final int appends;
    private final int warmup;
    private final int historyLog;
    private final String body = "x".repeat(BODY_BYTES);
    private final Hlc clock = Hlc.system();
    private final Map<String, String> rows = new LinkedHashMap<>();

    private LogBench(String url, int appends, int warmup, int historyLog) {
        this.url = url;
        this.appends = appends;
        this.warmup = warmup;
        this.historyLog = historyLog;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        String url = options.get("--url");
        if (url == null) {
            throw new IllegalArgumentException("usage: --url nats://host:port --wipe [--monitor http://host:port]"
                    + " [--appends N] [--warmup N] [--history-log N]");
        }
        if (!options.containsKey(WIPE)) {
            System.err.println("[bench] refused: the bench deletes the stream " + NatsEventLog.STREAM + " on " + url
                    + " and every record in it. Pass --wipe to agree, and only against a server whose log may be"
                    + " thrown away, e.g. --url nats://localhost:4223 --monitor http://localhost:8223 --wipe.");
            System.exit(2);
        }
        int historyLog = Integer.parseInt(options.getOrDefault("--history-log", "1000000"));
        if (historyLog < 2 * LARGE_ENTITY || historyLog % LARGE_ENTITY != 0) {
            throw new IllegalArgumentException("--history-log must be a multiple of 100000, at least 200000");
        }
        LogBench bench = new LogBench(url,
                Integer.parseInt(options.getOrDefault("--appends", "10000")),
                Integer.parseInt(options.getOrDefault("--warmup", "1000")),
                historyLog);
        String table = bench.run(options.get("--monitor"));
        System.out.println(table);
        URI uri = URI.create(url);
        Path out = Path.of("build", "bench", "results-" + uri.getHost() + "-" + uri.getPort() + ".md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, table);
        System.out.println("Written to " + out.toAbsolutePath());
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(WIPE)) {
                options.put(WIPE, "");
            } else if (i + 1 < args.length) {
                options.put(args[i], args[++i]);
            } else {
                throw new IllegalArgumentException("missing value for " + args[i]);
            }
        }
        return options;
    }

    private String run(String monitor) throws Exception {
        String server = describeServer(monitor);
        Instant started = Instant.now();

        latency("Append 1 record", 0, 1);
        latency("Append 1 + 3 records", 3, 1);
        latency("Append 1 + 50 records", 50, 1);
        latency("Append 1 + 3 records, 4 concurrent appenders", 3, 4);
        history();

        StringBuilder table = new StringBuilder();
        table.append("### ").append(server).append("\n\n");
        table.append("| Scenario | Result |\n| --- | --- |\n");
        rows.forEach((scenario, result) -> table.append("| ").append(scenario).append(" | ").append(result).append(" |\n"));
        table.append("\nRun at ").append(started).append(", took ").append(format(Duration.between(started, Instant.now())))
                .append(". Each scenario on an empty stream; ").append(group(warmup))
                .append(" warm-up appends of the same shape first; ").append(BODY_BYTES).append("-byte bodies.\n");
        return table.toString();
    }

    /** Appends {@code appends} batches of 1 + {@code extra} records, split over {@code appenders}. */
    private void latency(String scenario, int extra, int appenders) throws Exception {
        progress(scenario + " ...");
        freshStream();
        List<NatsEventLog> stores = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(appenders);
        try {
            for (int i = 0; i < appenders; i++) {
                stores.add(NatsEventLog.open(url));
            }
            runAppenders(pool, stores, extra, warmup / appenders, new CountDownLatch(appenders));
            CountDownLatch ready = new CountDownLatch(appenders);
            long start = System.nanoTime();
            List<long[]> perAppender = runAppenders(pool, stores, extra, appends / appenders, ready);
            long elapsed = System.nanoTime() - start;
            long[] all = perAppender.stream().flatMapToLong(Arrays::stream).sorted().toArray();
            long records = (long) all.length * (1 + extra);
            String suffix = appenders > 1 ? " (" + group(appends / appenders) + " per appender)" : "";
            rows.put(scenario + ", " + group(all.length) + " appends" + suffix,
                    "p50 " + millis(percentile(all, 50)) + ", p99 " + millis(percentile(all, 99))
                            + "; " + group(Math.round(records / (elapsed / 1e9))) + " events/s");
            if (extra == 50 && appenders == 1) {
                rows.put("Throughput, batches of 1 + 50, one appender",
                        group(Math.round(records / (elapsed / 1e9))) + " events/s");
            }
        } finally {
            pool.shutdownNow();
            stores.forEach(NatsEventLog::close);
        }
    }

    private List<long[]> runAppenders(ExecutorService pool, List<NatsEventLog> stores, int extra, int count,
                                      CountDownLatch ready) throws Exception {
        List<Future<long[]>> futures = new ArrayList<>();
        for (int a = 0; a < stores.size(); a++) {
            NatsEventLog store = stores.get(a);
            int appender = a;
            futures.add(pool.submit(() -> {
                long[] latencies = new long[count];
                ready.countDown();
                ready.await();
                for (int i = 0; i < count; i++) {
                    List<Event> batch = batch("e-" + appender + "-" + (i % ENTITIES), extra);
                    long t0 = System.nanoTime();
                    List<Long> positions = store.append(batch);
                    latencies[i] = System.nanoTime() - t0;
                    if (positions.size() != batch.size()) {
                        throw new IllegalStateException("append returned " + positions.size() + " positions");
                    }
                }
                return latencies;
            }));
        }
        List<long[]> results = new ArrayList<>();
        for (Future<long[]> f : futures) {
            results.add(f.get());
        }
        return results;
    }

    /** {@code 1 + extra} events about {@code entity}, appended as one atomic batch. */
    private List<Event> batch(String entity, int extra) {
        List<Event> batch = new ArrayList<>(1 + extra);
        for (int i = 0; i <= extra; i++) {
            batch.add(event(entity));
        }
        return batch;
    }

    /** An accepted assert of one value, about {@code entity}, with a body of about {@value #BODY_BYTES} bytes. */
    private Event event(String entity) {
        return Event.create(Optional.empty(), clock.next(), Optional.empty(), new Emitted(BY, Assertion.action(
                Assertion.of(Identity.named(entity), VALUE, new Assertion.Literal(new Json.Str(body))))));
    }

    /**
     * Fills a log of {@code historyLog} events in batches of 1 000, with entity {@code small} on 1 000
     * and {@code large} on 100 000 events spread evenly through it, then reads the events about both.
     * {@link NatsEventLog#about} has no index yet, so this measures a scan of the whole log.
     */
    private void history() {
        progress("Filling a log of " + group(historyLog) + " events ...");
        freshStream();
        int smallEvery = historyLog / SMALL_ENTITY;
        int largeEvery = historyLog / LARGE_ENTITY;
        try (NatsEventLog store = NatsEventLog.open(url)) {
            long start = System.nanoTime();
            for (int b = 0; b < historyLog / FILL_BATCH; b++) {
                List<Event> batch = new ArrayList<>(FILL_BATCH);
                for (int j = 0; j < FILL_BATCH; j++) {
                    int g = b * FILL_BATCH + j;
                    String entity = g % smallEvery == 0 ? "small"
                            : g % largeEvery == 1 ? "large"
                            : "other-" + (g % 10_000);
                    batch.add(event(entity));
                }
                store.append(batch);
                if ((b + 1) % 100 == 0) {
                    progress("  " + group((long) (b + 1) * FILL_BATCH) + " events, "
                            + format(Duration.ofNanos(System.nanoTime() - start)));
                }
            }
            long elapsed = System.nanoTime() - start;
            rows.put("Fill of the " + group(historyLog) + "-event log, batches of " + group(FILL_BATCH) + ", one appender",
                    format(Duration.ofNanos(elapsed)) + " (" + group(Math.round(historyLog / (elapsed / 1e9))) + " events/s)");
            readHistory(store, "small", SMALL_ENTITY);
            readHistory(store, "large", LARGE_ENTITY);
        }
    }

    private void readHistory(NatsEventLog store, String entity, int expected) {
        progress("History of " + group(expected) + " events ...");
        long[] times = new long[HISTORY_READS];
        for (int i = 0; i < HISTORY_READS; i++) {
            long t0 = System.nanoTime();
            List<Event> found = store.about(Identity.named(entity));
            times[i] = System.nanoTime() - t0;
            if (found.size() != expected) {
                throw new IllegalStateException("history of " + entity + " has " + found.size() + " records, not " + expected);
            }
        }
        long first = times[0];
        Arrays.sort(times);
        rows.put("History of an entity with " + group(expected) + " records, log of " + group(historyLog),
                "median " + millis(percentile(times, 50)) + " of " + HISTORY_READS + " reads (min "
                        + millis(times[0]) + ", max " + millis(times[HISTORY_READS - 1]) + ", first " + millis(first) + ")");
    }

    /** Deletes the whole log stream, if there is one; the next {@link NatsEventLog#open} creates it empty. */
    private void freshStream() {
        try (Connection connection = Nats.connect(url)) {
            JetStreamManagement management = connection.jetStreamManagement();
            try {
                management.deleteStream(NatsEventLog.STREAM);
            } catch (JetStreamApiException e) {
                if (e.getErrorCode() != 404) {
                    throw e;
                }
            }
            try (NatsEventLog store = NatsEventLog.open(url)) {
                long messages = management.getStreamInfo(NatsEventLog.STREAM).getStreamState().getMsgCount();
                if (messages != 0) {
                    throw new IllegalStateException("fresh stream has " + messages + " messages");
                }
            }
        } catch (IOException | JetStreamApiException e) {
            throw new IllegalStateException("cannot recreate stream " + NatsEventLog.STREAM, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String describeServer(String monitor) throws Exception {
        String version;
        try (Connection connection = Nats.connect(url)) {
            version = connection.getServerInfo().getVersion();
        }
        String sync = "sync unknown (no --monitor)";
        if (monitor != null) {
            try (HttpClient client = HttpClient.newHttpClient()) {
                String varz = client.send(HttpRequest.newBuilder(URI.create(monitor + "/varz")).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
                JsonValue syncAlways = JsonParser.parse(varz).map.get("jetstream").map.get("config").map.get("sync_always");
                sync = syncAlways != null && Boolean.TRUE.equals(syncAlways.bool)
                        ? "sync always (sync_always: true)" : "default sync (sync_always: false)";
            }
        }
        return "NATS " + version + ", " + sync + ", " + url;
    }

    private static long percentile(long[] sorted, int p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        return sorted[Math.max(0, rank - 1)];
    }

    private static String millis(long nanos) {
        double ms = nanos / 1e6;
        return (ms < 10 ? String.format("%.2f", ms) : ms < 100 ? String.format("%.1f", ms) : String.format("%.0f", ms))
                + " ms";
    }

    private static String format(Duration d) {
        long s = d.toSeconds();
        return s >= 60 ? (s / 60) + " min " + (s % 60) + " s" : String.format("%.1f s", d.toMillis() / 1000.0);
    }

    /** {@code n} with a space between thousands: 1 000 000. */
    private static String group(long n) {
        return String.format("%,d", n).replace(',', ' ');
    }

    private static void progress(String message) {
        System.err.println("[bench] " + message);
    }
}
