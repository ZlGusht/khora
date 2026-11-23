package khora.events.log.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.PublishOptions;
import io.nats.client.api.MessageInfo;
import io.nats.client.api.PublishAck;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamState;
import io.nats.client.impl.Headers;
import io.synadia.bp.BatchPublishException;
import io.synadia.bp.BatchPublisher;
import io.synadia.bp.BatchUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spike: does atomic batch publish to JetStream work from Java, with io.synadia:batch-publish
 * against {@value NatsTestServer#IMAGE}? Each test creates a fresh stream with the log's settings.
 * The tests print what NATS did, prefixed with "SPIKE", so the run can record it.
 *
 * <p>They also pin the server behaviour {@link NatsEventLog}'s duplicate handling depends on, observed
 * on 2.12.15: a batch holding a stored message id is refused with error 10201, and the duplicate check
 * runs before the expected-last-msg-id check. Run them again before changing the image.
 */
class AtomicBatchSpikeTest {

    private static final String STREAM = "KHORA_LOG";

    private static NatsTestServer server;
    private static Connection connection;
    private static JetStreamManagement jsm;

    @BeforeAll
    static void startServer() throws Exception {
        server = new NatsTestServer().start();
        connection = Nats.connect(server.url());
        jsm = connection.jetStreamManagement();
        String version = connection.getServerInfo().getVersion();
        report("server version " + version + ", max payload " + connection.getMaxPayload());
        assertTrue(atLeast(version, 2, 12, 1), "NATS server must be 2.12.1 or later, was " + version);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (connection != null) {
            connection.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @BeforeEach
    void createStream() throws Exception {
        jsm.addStream(logStream().build());
    }

    @AfterEach
    void deleteStream() throws Exception {
        jsm.deleteStream(STREAM);
    }

    /** The log stream's settings: file storage, no limits, deletes and purges denied, atomic publish allowed. */
    private static StreamConfiguration.Builder logStream() {
        return StreamConfiguration.builder()
                .name(STREAM)
                .subjects("khora.log.>")
                .storageType(StorageType.File)
                .maxAge(Duration.ZERO)
                .maxMessages(-1)
                .maxBytes(-1)
                .denyDelete(true)
                .denyPurge(true)
                .allowAtomicPublish(true);
    }

    @Test
    void batchOfFourIsStoredTogether() throws Exception {
        BatchPublisher batch = BatchPublisher.builder().connection(connection).build();
        batch.add("khora.log.a", bytes("m1"));
        batch.add("khora.log.b", bytes("m2"));
        batch.add("khora.log.a", bytes("m3"));
        PublishAck ack = batch.commit("khora.log.b", bytes("m4"));
        report("batchOfFour ack: " + describe(ack));

        StreamState state = jsm.getStreamInfo(STREAM).getStreamState();
        report("batchOfFour stream: msgs=" + state.getMsgCount() + " first=" + state.getFirstSequence()
                + " last=" + state.getLastSequence());
        assertEquals(4, state.getMsgCount());
        assertEquals(1, state.getFirstSequence());
        assertEquals(4, state.getLastSequence());
        assertEquals(4, ack.getSeqno());
        assertEquals(4, ack.getBatchSize());

        List<String> stored = new ArrayList<>();
        for (long seq = 1; seq <= 4; seq++) {
            MessageInfo m = jsm.getMessage(STREAM, seq);
            stored.add(m.getSeq() + ":" + m.getSubject() + ":" + new String(m.getData(), StandardCharsets.UTF_8));
        }
        report("batchOfFour stored: " + stored);
        assertEquals(List.of("1:khora.log.a:m1", "2:khora.log.b:m2", "3:khora.log.a:m3", "4:khora.log.b:m4"), stored);
    }

    @Test
    void refusedBatchStoresNothing() throws Exception {
        byte[] tooBig = new byte[(int) connection.getMaxPayload() + 1];
        BatchPublisher batch = BatchPublisher.builder().connection(connection).build();
        Exception failure = assertThrows(Exception.class, () -> {
            batch.add("khora.log.a", bytes("m1"));
            batch.add("khora.log.a", bytes("m2"));
            batch.add("khora.log.a", tooBig);
            batch.commit("khora.log.a", bytes("m4"));
        });
        // Observed: jnats refuses the oversized message in the client (IllegalArgumentException, wrapped by
        // batch-publish), so messages 1 and 2 reach the server but the batch is never committed.
        report("refusedBatch failure: " + describe(failure));
        batch.discard();

        assertEquals(0, storedCount(), "a refused batch must leave nothing stored");
        // The server discards an uncommitted batch after its idle timeout; it must still hold nothing then.
        int idleSeconds = BatchUtils.getBatchIdleTimeoutSeconds(connection);
        report("refusedBatch batch idle timeout seconds: " + idleSeconds);
        Thread.sleep(Duration.ofSeconds(idleSeconds + 1));
        report("refusedBatch stream msgs after idle timeout: " + storedCount());
        assertEquals(0, storedCount(), "a refused batch must leave nothing stored after the idle timeout");
    }

    @Test
    void batchRefusedByServerStoresNothing() throws Exception {
        // The client never sends a message over max payload, so refusedBatchStoresNothing fails in the client.
        // This test makes the server refuse the third message: it exceeds the stream's max message size.
        jsm.updateStream(logStream().maximumMessageSize(1024).build());
        BatchPublisher batch = BatchPublisher.builder().connection(connection).build();
        BatchPublishException failure = assertThrows(BatchPublishException.class, () -> {
            batch.add("khora.log.a", bytes("m1"));
            batch.add("khora.log.a", bytes("m2"));
            batch.add("khora.log.a", new byte[2048]);
            batch.commit("khora.log.a", bytes("m4"));
        });
        report("serverRefusedBatch failure: " + describe(failure));
        // Observed on 2.12.15: the commit fails with API error 10176 ("atomic publish batch is incomplete").
        if (!batch.isClosed()) {
            batch.discard();
        }
        assertEquals(0, storedCount(), "a batch the server refuses must leave nothing stored");
    }

    @Test
    void duplicateMsgIdInsideBatch() throws Exception {
        // Observed on 2.12.15: the server refuses the whole batch at commit with API error 10201
        // ("atomic publish batch contains duplicate message id") and stores none of it.
        BatchPublisher batch = BatchPublisher.builder().connection(connection).build();
        BatchPublishException failure = assertThrows(BatchPublishException.class, () -> {
            batch.add("khora.log.a", msgId("dup-1"), bytes("m1"));
            batch.add("khora.log.a", msgId("dup-1"), bytes("m2"));
            batch.commit("khora.log.a", msgId("other"), bytes("m3"));
        });
        report("duplicateMsgId failure: " + describe(failure));
        report("duplicateMsgId stream msgs: " + storedCount());
        assertEquals(10201, failure.getApiErrorCode());
        assertEquals(0, storedCount(), "a batch with a duplicate Nats-Msg-Id must leave nothing stored");
    }

    @Test
    void batchHoldingAStoredMsgIdIsRefused() throws Exception {
        // Observed on 2.12.15: API error 10201, and nothing of the batch is stored.
        connection.jetStream().publish("khora.log.a", msgId("stored-1"), bytes("m0"));
        BatchPublisher batch = BatchPublisher.builder().connection(connection).build();
        BatchPublishException failure = assertThrows(BatchPublishException.class, () -> {
            batch.add("khora.log.a", msgId("new-1"), bytes("m1"));
            batch.commit("khora.log.a", msgId("stored-1"), bytes("m2"));
        });
        report("storedMsgIdInBatch failure: " + describe(failure));
        if (!batch.isClosed()) {
            batch.discard();
        }
        assertEquals(10201, failure.getApiErrorCode());
        assertEquals(1, storedCount(), "a batch holding a stored Nats-Msg-Id must leave nothing stored");
    }

    @Test
    void duplicateCheckRunsBeforeExpectedLastMsgId() throws Exception {
        // Observed on 2.12.15: for a stored id the server answers with a duplicate ack carrying the
        // original sequence, although the expected last message id does not match; for a new id it
        // refuses with API error 10070 and stores nothing.
        PublishAck original = connection.jetStream().publish("khora.log.a", msgId("stored-1"), bytes("m0"));
        PublishOptions wrongLast = PublishOptions.builder().expectedLastMsgId("no-such-id").build();
        PublishAck duplicate = connection.jetStream()
                .publish("khora.log.a", msgId("stored-1"), bytes("m0"), wrongLast);
        report("probe of a stored id: " + describe(duplicate));
        assertTrue(duplicate.isDuplicate());
        assertEquals(original.getSeqno(), duplicate.getSeqno());

        JetStreamApiException refused = assertThrows(JetStreamApiException.class, () -> connection.jetStream()
                .publish("khora.log.a", msgId("new-1"), bytes("m1"), wrongLast));
        report("probe of a new id: " + describe(refused));
        assertEquals(10070, refused.getApiErrorCode());
        assertEquals(1, storedCount());
    }

    @Test
    void streamRefusesDelete() throws Exception {
        PublishAck ack = connection.jetStream().publish("khora.log.a", bytes("m1"));
        JetStreamApiException deleteFailure =
                assertThrows(JetStreamApiException.class, () -> jsm.deleteMessage(STREAM, ack.getSeqno()));
        report("delete failure: " + describe(deleteFailure));
        JetStreamApiException purgeFailure = assertThrows(JetStreamApiException.class, () -> jsm.purgeStream(STREAM));
        report("purge failure: " + describe(purgeFailure));
        assertEquals(1, storedCount());
    }

    private static long storedCount() throws Exception {
        return jsm.getStreamInfo(STREAM).getStreamState().getMsgCount();
    }

    private static Headers msgId(String id) {
        return new Headers().put("Nats-Msg-Id", id);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String describe(PublishAck ack) {
        if (ack == null) {
            return "none";
        }
        return "stream=" + ack.getStream() + " seq=" + ack.getSeqno() + " duplicate=" + ack.isDuplicate()
                + " batchId=" + ack.getBatchId() + " batchSize=" + ack.getBatchSize();
    }

    private static String describe(Throwable t) {
        if (t == null) {
            return "none";
        }
        String s = t.getClass().getName() + ": " + t.getMessage();
        if (t instanceof BatchPublishException b) {
            s += " [errorCode=" + b.getErrorCode() + " apiErrorCode=" + b.getApiErrorCode()
                    + " description=" + b.getErrorDescription() + "]";
        }
        if (t instanceof JetStreamApiException j) {
            s += " [errorCode=" + j.getErrorCode() + " apiErrorCode=" + j.getApiErrorCode()
                    + " description=" + j.getErrorDescription() + "]";
        }
        return s;
    }

    private static boolean atLeast(String version, int major, int minor, int patch) {
        String[] parts = version.split("[.-]");
        int[] v = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        if (v[0] != major) {
            return v[0] > major;
        }
        if (v[1] != minor) {
            return v[1] > minor;
        }
        return v[2] >= patch;
    }

    private static void report(String line) {
        System.out.println("SPIKE " + line);
    }
}
