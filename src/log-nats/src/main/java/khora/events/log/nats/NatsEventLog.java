package khora.events.log.nats;

import io.nats.client.ConsumeOptions;
import io.nats.client.Connection;
import io.nats.client.ErrorListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamOptions;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PublishOptions;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.OrderedConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.Status;
import io.synadia.bp.BatchPublishException;
import io.synadia.bp.BatchPublisher;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import khora.events.About;
import khora.events.Event;
import khora.events.EventId;
import khora.events.EventJson;
import khora.events.Identity;
import khora.events.log.EventLog;
import khora.events.log.EventLogException;
import khora.events.log.Following;
import khora.events.log.Logged;

/**
 * An {@link EventLog} on the NATS JetStream stream {@value #STREAM}. An event is one message: the
 * subject is {@link Subjects#forEvent}, so an event is found by its id, {@code Nats-Msg-Id} is the
 * event id, and the data is the event's canonical JSON. Positions are stream sequences. An append
 * is one atomic batch.
 *
 * <p>Connecting, every JetStream request and every publish ack wait at most {@link #TIMEOUT}.
 * Publishes are not buffered while the connection is down, so an append against an unreachable
 * server fails at once instead of being sent later. The client keeps reconnecting.
 */
public final class NatsEventLog implements EventLog {

    static final String STREAM = "KHORA_EVENTS";
    static final String MSG_ID = "Nats-Msg-Id";

    /** Room left in a message for the headers batch publish adds (batch id and sequence). */
    private static final int BATCH_HEADER_RESERVE = 256;
    /** Server error: a batch holds a message id the stream already has (or holds one twice). */
    private static final int DUPLICATE_MESSAGE_ID = 10201;
    /** Server error: the expected last message id does not match. */
    private static final int WRONG_LAST_MSG_ID = 10070;
    /** Server error: no message found. */
    private static final int NO_MESSAGE_FOUND = 10037;
    private static final int APPEND_ATTEMPTS = 3;
    /** The window in which the server de-duplicates event ids. */
    static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(2);
    /** How long a read waits for the next message it knows is there. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    /** The longest wait to connect, for a JetStream request, or for a publish ack. */
    static final Duration TIMEOUT = Duration.ofSeconds(4);
    /**
     * A subscription's pulls expire after this, and the server sends heartbeats at half of it; missed
     * heartbeats (after a lost connection, for example) make the client recreate the consumer.
     */
    private static final ConsumeOptions CONSUME_OPTIONS =
            ConsumeOptions.builder().expiresIn(Duration.ofSeconds(5).toMillis()).build();
    private static final System.Logger LOG = System.getLogger(NatsEventLog.class.getName());

    private final Connection connection;
    private final JetStream jetStream;
    private final JetStreamManagement management;
    private final JetStreamOptions jetStreamOptions;
    private final Set<NatsFollowing> followings = ConcurrentHashMap.newKeySet();

    private NatsEventLog(Connection connection) throws IOException {
        this.connection = connection;
        this.jetStreamOptions = JetStreamOptions.builder().requestTimeout(TIMEOUT).build();
        this.jetStream = connection.jetStream(jetStreamOptions);
        this.management = connection.jetStreamManagement(jetStreamOptions);
    }

    /**
     * Connects to {@code natsUrl} and creates the stream, or checks that the existing stream has the
     * log's settings.
     */
    public static NatsEventLog open(String natsUrl) {
        Connection connection;
        try {
            connection = Nats.connect(options(natsUrl));
        } catch (IOException e) {
            throw new EventLogException("cannot connect to NATS at " + natsUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventLogException("interrupted while connecting to NATS at " + natsUrl, e);
        }
        try {
            NatsEventLog log = new NatsEventLog(connection);
            log.ensureStream();
            return log;
        } catch (IOException | JetStreamApiException | RuntimeException e) {
            closeQuietly(connection);
            if (e instanceof EventLogException l) {
                throw l;
            }
            throw new EventLogException("cannot open stream " + STREAM, e);
        }
    }

    private static Options options(String natsUrl) {
        return Options.builder()
                .server(natsUrl)
                .connectionTimeout(TIMEOUT)
                .maxReconnects(-1)
                .reconnectBufferSize(0)
                .errorListener(new LoggingErrorListener())
                .build();
    }

    /** Sends client errors to the {@code System.Logger} at warning level. */
    private static final class LoggingErrorListener implements ErrorListener {
        @Override
        public void errorOccurred(Connection conn, String error) {
            LOG.log(System.Logger.Level.WARNING, "NATS error: " + error);
        }

        @Override
        public void exceptionOccurred(Connection conn, Exception exp) {
            LOG.log(System.Logger.Level.WARNING, "NATS exception: " + exp, exp);
        }

        @Override
        public void slowConsumerDetected(Connection conn, io.nats.client.Consumer consumer) {
            LOG.log(System.Logger.Level.WARNING, "NATS slow consumer: " + consumer);
        }

        @Override
        public void heartbeatAlarm(Connection conn, JetStreamSubscription sub,
                long lastStreamSequence, long lastConsumerSequence) {
            LOG.log(System.Logger.Level.WARNING, "NATS heartbeat alarm on " + sub.getSubject()
                    + " after stream sequence " + lastStreamSequence);
        }

        @Override
        public void pullStatusError(Connection conn, JetStreamSubscription sub, Status status) {
            LOG.log(System.Logger.Level.WARNING, "NATS pull error on " + sub.getSubject() + ": " + status);
        }
    }

    static StreamConfiguration streamConfiguration() {
        return StreamConfiguration.builder()
                .name(STREAM)
                .subjects(Subjects.ALL)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.Limits)
                .discardPolicy(DiscardPolicy.Old)
                .duplicateWindow(DUPLICATE_WINDOW)
                .maxAge(Duration.ZERO)
                .maxMessages(-1)
                .maxBytes(-1)
                .maxMessagesPerSubject(-1)
                .maximumMessageSize(-1)
                .denyDelete(true)
                .denyPurge(true)
                .allowAtomicPublish(true)
                .build();
    }

    private void ensureStream() throws IOException, JetStreamApiException {
        StreamConfiguration existing;
        try {
            existing = management.getStreamInfo(STREAM).getConfiguration();
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() != 404) {
                throw e;
            }
            management.addStream(streamConfiguration());
            return;
        }
        List<String> differences = differences(existing);
        if (!differences.isEmpty()) {
            throw new EventLogException("stream " + STREAM + " exists with other settings: " + differences);
        }
    }

    private static List<String> differences(StreamConfiguration c) {
        List<String> d = new ArrayList<>();
        if (!c.getSubjects().equals(List.of(Subjects.ALL))) {
            d.add("subjects " + c.getSubjects());
        }
        if (c.getStorageType() != StorageType.File) {
            d.add("storage " + c.getStorageType());
        }
        if (c.getRetentionPolicy() != RetentionPolicy.Limits) {
            d.add("retention " + c.getRetentionPolicy());
        }
        if (c.getDiscardPolicy() != DiscardPolicy.Old) {
            d.add("discard " + c.getDiscardPolicy());
        }
        if (!DUPLICATE_WINDOW.equals(c.getDuplicateWindow())) {
            d.add("duplicate window " + c.getDuplicateWindow());
        }
        if (c.getMaxAge() != null && !c.getMaxAge().isZero()) {
            d.add("max age " + c.getMaxAge());
        }
        if (c.getMaxMsgs() != -1) {
            d.add("max messages " + c.getMaxMsgs());
        }
        if (c.getMaxBytes() != -1) {
            d.add("max bytes " + c.getMaxBytes());
        }
        if (c.getMaxMsgsPerSubject() != -1) {
            d.add("max messages per subject " + c.getMaxMsgsPerSubject());
        }
        if (c.getMaximumMessageSize() != -1) {
            d.add("max message size " + c.getMaximumMessageSize());
        }
        if (!c.getDenyDelete()) {
            d.add("deletes allowed");
        }
        if (!c.getDenyPurge()) {
            d.add("purges allowed");
        }
        if (!c.getAllowAtomicPublish()) {
            d.add("atomic publish not allowed");
        }
        return d;
    }

    /**
     * Appends {@code events} as one atomic batch. A repeated event id inside {@code events} is sent
     * once (the first wins) and every copy gets its position. If the server refuses the batch because
     * an event id is already stored, each event is probed with a plain publish that expects a last
     * message id no event has: for a stored event id the server answers with a duplicate ack that
     * carries the original sequence, and for a new one it refuses and stores nothing. The new events
     * are then sent again as one batch.
     */
    @Override
    public List<Long> append(List<Event> events) {
        if (connection.getStatus() == Connection.Status.CLOSED) {
            throw new EventLogException("append to a closed log");
        }
        if (events.size() > MAX_APPEND) {
            throw new EventLogException("append of " + events.size() + " events exceeds " + MAX_APPEND);
        }
        if (events.isEmpty()) {
            return List.of();
        }
        Map<String, Outgoing> unique = new LinkedHashMap<>();
        for (Event event : events) {
            if (!unique.containsKey(event.id().value())) {
                unique.put(event.id().value(), outgoing(event, events));
            }
        }
        Map<String, Long> positions = new HashMap<>();
        List<Outgoing> pending = new ArrayList<>();
        for (Outgoing out : unique.values()) {
            long stored = storedAt(out.subject);
            if (stored > 0) {
                positions.put(out.eventId, stored);
            } else {
                pending.add(out);
            }
        }
        for (int attempt = 1; !pending.isEmpty(); attempt++) {
            try {
                List<Long> sent = publishBatch(pending);
                for (int i = 0; i < pending.size(); i++) {
                    positions.put(pending.get(i).eventId, sent.get(i));
                }
                pending = List.of();
            } catch (BatchPublishException e) {
                if (e.getApiErrorCode() != DUPLICATE_MESSAGE_ID || attempt == APPEND_ATTEMPTS) {
                    throw new EventLogException(describe(events) + " failed: " + e.getMessage(), e);
                }
                List<Outgoing> notStored = probe(pending, positions, events);
                if (notStored.size() == pending.size()) {
                    throw new EventLogException(
                            describe(events) + " was refused for a duplicate event id, but none is stored", e);
                }
                pending = notStored;
            } catch (RuntimeException e) {
                if (e instanceof EventLogException) {
                    throw e;
                }
                throw new EventLogException(describe(events) + " failed: " + e.getMessage(), e);
            }
        }
        return events.stream().map(r -> positions.get(r.id().value())).toList();
    }

    private record Outgoing(String eventId, String subject, Headers headers, byte[] body) {
    }

    private Outgoing outgoing(Event event, List<Event> events) {
        try {
            String id = event.id().value();
            Headers headers = new Headers().put(MSG_ID, id);
            byte[] body = EventJson.write(event).getBytes(StandardCharsets.UTF_8);
            Outgoing out = new Outgoing(id, Subjects.forEvent(event.id()), headers, body);
            long size = (long) headers.serializedLength() + body.length + BATCH_HEADER_RESERVE;
            if (size > connection.getMaxPayload()) {
                throw new IllegalArgumentException("event of " + body.length
                        + " bytes exceeds the server's max payload of " + connection.getMaxPayload() + " bytes");
            }
            return out;
        } catch (IllegalArgumentException e) {
            throw new EventLogException(
                    describe(events) + " refused, event " + event.id().value() + ": " + e.getMessage(), e);
        }
    }

    /** Publishes {@code batch} atomically and returns the position of each message. */
    private List<Long> publishBatch(List<Outgoing> batch) throws BatchPublishException {
        BatchPublisher publisher = BatchPublisher.builder()
                .connection(connection).ackTimeout(TIMEOUT.toMillis()).build();
        try {
            for (Outgoing out : batch.subList(0, batch.size() - 1)) {
                publisher.add(out.subject, out.headers, out.body);
            }
            Outgoing last = batch.getLast();
            PublishAck ack = publisher.commit(last.subject, last.headers, last.body);
            if (ack.getBatchSize() != batch.size()) {
                throw new IllegalStateException(
                        "batch of " + batch.size() + " messages was acked with size " + ack.getBatchSize());
            }
            long first = ack.getSeqno() - batch.size() + 1;
            List<Long> positions = new ArrayList<>(batch.size());
            for (int i = 0; i < batch.size(); i++) {
                positions.add(first + i);
            }
            return positions;
        } finally {
            if (!publisher.isClosed()) {
                publisher.discard();
            }
        }
    }

    /**
     * Puts the position of each already stored event of {@code batch} into {@code positions} and
     * returns the others. A probe publish expects a last message id that no event has, so the server
     * stores nothing for a new event id; for a stored event id its duplicate check runs first and the
     * ack carries the original sequence. Should the expected id ever match, the event is stored by
     * the probe itself, and its ack still gives its position.
     */
    private List<Outgoing> probe(List<Outgoing> batch, Map<String, Long> positions, List<Event> events) {
        PublishOptions options = PublishOptions.builder()
                .expectedLastMsgId("khora-probe-" + UUID.randomUUID())
                .build();
        List<Outgoing> notStored = new ArrayList<>();
        for (Outgoing out : batch) {
            try {
                PublishAck ack = jetStream.publish(out.subject, out.headers, out.body, options);
                positions.put(out.eventId, ack.getSeqno());
            } catch (JetStreamApiException e) {
                if (e.getApiErrorCode() != WRONG_LAST_MSG_ID) {
                    throw new EventLogException(describe(events) + " failed probing event " + out.eventId, e);
                }
                notStored.add(out);
            } catch (IOException e) {
                throw new EventLogException(describe(events) + " failed probing event " + out.eventId, e);
            }
        }
        return notStored;
    }

    @Override
    public List<Logged> read(long from, int max) {
        return scan(Math.max(from, 1), max, Subjects.ALL);
    }

    @Override
    public Optional<Event> get(EventId id) {
        try {
            var message = management.getLastMessage(STREAM, Subjects.forEvent(id));
            Event found = decode(message.getSeq(), message.getHeaders(), message.getData()).event();
            if (!found.id().equals(id)) {
                throw new EventLogException("message " + message.getSeq() + " under " + id.value() + " holds event " + found.id().value());
            }
            return Optional.of(found);
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() == NO_MESSAGE_FOUND) {
                return Optional.empty();
            }
            throw new EventLogException("getting event " + id.value() + " failed", e);
        } catch (IOException e) {
            throw new EventLogException("getting event " + id.value() + " failed", e);
        }
    }

    /**
     * Reads the whole log, a page of {@value #MAX_APPEND} events at a time, and keeps the events
     * about {@code thing}: no index yet. Other events a page needs are looked up by id.
     */
    @Override
    public List<Event> about(Identity thing) {
        var found = new ArrayList<Event>();
        long position = 1;
        while (true) {
            var page = scan(position, MAX_APPEND, Subjects.ALL);
            if (page.isEmpty()) {
                return found;
            }
            for (var logged : page) {
                if (About.of(logged.event(), this::get).contains(thing)) {
                    found.add(logged.event());
                }
            }
            position = page.getLast().position() + 1;
        }
    }

    /**
     * Reads with an ordered consumer on {@code filter} from {@code from}, up to {@code max} events.
     * It stops at the last sequence matching {@code filter} when the call started, so events
     * appended during the read are not waited for.
     */
    private List<Logged> scan(long from, int max, String filter) {
        if (max <= 0) {
            return List.of();
        }
        try {
            long last = lastSequence(filter);
            if (from > last) {
                return List.of();
            }
            PushSubscribeOptions options = PushSubscribeOptions.builder()
                    .stream(STREAM)
                    .ordered(true)
                    .configuration(ConsumerConfiguration.builder()
                            .deliverPolicy(DeliverPolicy.ByStartSequence)
                            .startSequence(from)
                            .build())
                    .build();
            JetStreamSubscription subscription = jetStream.subscribe(filter, options);
            try {
                List<Logged> found = new ArrayList<>();
                while (found.size() < max) {
                    Message message = subscription.nextMessage(READ_TIMEOUT);
                    if (message == null) {
                        throw new EventLogException("reading " + filter + " from position " + from
                                + " timed out after " + found.size() + " events");
                    }
                    NatsJetStreamMetaData meta = message.metaData();
                    if (meta.streamSequence() > last) {
                        break;
                    }
                    found.add(decode(meta.streamSequence(), message.getHeaders(), message.getData()));
                    if (meta.streamSequence() == last) {
                        break;
                    }
                }
                return found;
            } finally {
                subscription.unsubscribe();
            }
        } catch (IOException | JetStreamApiException e) {
            throw new EventLogException("reading " + filter + " from position " + from + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventLogException("interrupted reading " + filter + " from position " + from, e);
        }
    }

    /** Where the event filed under {@code subject} is stored, or 0: the log keeps each event once, beyond the duplicate window too. */
    private long storedAt(String subject) {
        try {
            return lastSequence(subject);
        } catch (IOException | JetStreamApiException | RuntimeException e) {
            throw new EventLogException("looking up " + subject + " failed: " + e.getMessage(), e);
        }
    }

    /** The last sequence matching {@code filter} now, or 0 if none does. */
    private long lastSequence(String filter) throws IOException, JetStreamApiException {
        if (filter.equals(Subjects.ALL)) {
            return management.getStreamInfo(STREAM).getStreamState().getLastSequence();
        }
        try {
            return management.getLastMessage(STREAM, filter).getSeq();
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() == NO_MESSAGE_FOUND) {
                return 0;
            }
            throw e;
        }
    }

    /** The event a message holds; refuses one whose id does not match its content or its header. */
    private static Logged decode(long seq, Headers headers, byte[] data) {
        String eventId = headers == null ? null : headers.getFirst(MSG_ID);
        if (eventId == null) {
            throw new EventLogException("message " + seq + " has no " + MSG_ID);
        }
        Event event;
        try {
            event = EventJson.read(new String(data == null ? new byte[0] : data, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new EventLogException("message " + seq + " does not hold a valid event: " + e.getMessage(), e);
        }
        if (!event.id().value().equals(eventId)) {
            throw new EventLogException("message " + seq + " holds event " + event.id().value() + ", not " + eventId);
        }
        return new Logged(seq, event);
    }

    /**
     * Delivers with an ordered consumer on the whole log, starting at {@code fromPosition}. The
     * client recreates the consumer from the next expected position if it misses messages, heartbeats
     * or the connection. Messages are handed to {@code handler} on the connection's dispatcher thread,
     * one at a time. A handler that throws is logged at warning level and delivery goes on. A message
     * that cannot be decoded stops the subscription, logged at error level with its position, since
     * {@link #read} fails on it too.
     */
    @Override
    public Following follow(long fromPosition, Consumer<Logged> handler) {
        long from = Math.max(fromPosition, 1);
        NatsFollowing subscription = new NatsFollowing(handler);
        try {
            subscription.consumer = connection.getStreamContext(STREAM, jetStreamOptions)
                    .createOrderedConsumer(new OrderedConsumerConfiguration()
                            .filterSubject(Subjects.ALL)
                            .deliverPolicy(DeliverPolicy.ByStartSequence)
                            .startSequence(from))
                    .consume(CONSUME_OPTIONS, subscription::deliver);
        } catch (IOException | JetStreamApiException | RuntimeException e) {
            throw new EventLogException("following from position " + from + " failed", e);
        }
        followings.add(subscription);
        if (subscription.closed) {
            subscription.close(); // an event failed to decode before the consumer was assigned
        }
        return subscription;
    }

    private final class NatsFollowing implements Following {
        private final Consumer<Logged> handler;
        private volatile MessageConsumer consumer;
        private volatile boolean closed;

        NatsFollowing(Consumer<Logged> handler) {
            this.handler = handler;
        }

        private void deliver(Message message) {
            if (closed) {
                return;
            }
            long position = message.metaData().streamSequence();
            Logged logged;
            try {
                logged = decode(position, message.getHeaders(), message.getData());
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "following stopped: the event at position " + position
                        + " cannot be decoded: " + e.getMessage(), e);
                closed = true;
                // The consumer is closed off the dispatcher thread that is running this method.
                Thread.ofVirtual().start(this::close);
                return;
            }
            try {
                handler.accept(logged);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "subscription handler failed on position " + position
                        + ", event " + logged.event().id().value() + "; delivery goes on", e);
            }
        }

        /** Stops delivery: no event reaches the handler once this returns, except one already in it. */
        @Override
        public void close() {
            closed = true;
            followings.remove(this);
            MessageConsumer c = consumer;
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LOG.log(System.Logger.Level.WARNING, "closing a subscription failed", e);
                }
            }
        }
    }

    @Override
    public void close() {
        List.copyOf(followings).forEach(NatsFollowing::close);
        closeQuietly(connection);
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String describe(List<Event> events) {
        List<String> ids = events.stream().map(r -> r.id().value()).distinct().toList();
        String shown = ids.size() <= 5 ? String.join(", ", ids)
                : String.join(", ", ids.subList(0, 5)) + ", ... (" + (ids.size() - 5) + " more)";
        return "append of " + events.size() + " events [" + shown + "]";
    }
}
