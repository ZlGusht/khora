package khora.query.neo4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.json.Json;

/**
 * A {@link Neo4jDatastore}'s content at one position of its log: the frontier it had taken in, and
 * its rows (events, statements, retractions) as canonical JSON, one per line. A datastore rebuilt
 * from a snapshot and the log after it equals one built from the start of the log (ADR-0XY
 * decision 10).
 *
 * <p>Two snapshots are equal when they hold the same content.
 *
 * <p>Provisional: kept whole in memory and in one file; no compression, no checksum.
 *
 * @param position the last log position taken in
 * @param at the frontier of everything taken in
 * @param rows the rows, events first by position, then statements, then retractions
 */
public record Snapshot(long position, Frontier at, List<String> rows) {
    static final int VERSION = 1;

    public Snapshot {
        if (position < 0) {
            throw new IllegalArgumentException("position is negative: " + position);
        }
        Objects.requireNonNull(at, "at");
        rows = List.copyOf(rows);
    }

    /** The first line of a snapshot file. */
    private Json.Obj header() {
        return Json.Obj.of(Map.of("type", new Json.Str("snapshot"), "version", new Json.Int(VERSION),
                "position", new Json.Int(position),
                "at", new Json.Arr(at.heads().stream().<Json>map(h -> new Json.Str(h.value())).toList())));
    }

    /** Writes the snapshot to {@code file}: the header line, then one line per row. */
    public void write(Path file) throws IOException {
        var lines = new ArrayList<String>(rows.size() + 1);
        lines.add(header().canonical());
        lines.addAll(rows);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * The snapshot {@link #write} wrote to {@code file}.
     *
     * @throws IllegalArgumentException if the file is not a snapshot of this version
     */
    public static Snapshot read(Path file) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an empty file is not a snapshot");
        }
        if (!(Json.parse(lines.get(0)) instanceof Json.Obj header) || header.get("type").isEmpty()
                || !header.str("type").equals("snapshot")) {
            throw new IllegalArgumentException("the first line is not a snapshot header");
        }
        if (header.integer("version") != VERSION) {
            throw new IllegalArgumentException("snapshot version " + header.integer("version") + " is not " + VERSION);
        }
        var heads = new TreeSet<EventId>();
        for (var h : header.arr("at").items()) {
            if (!(h instanceof Json.Str s)) {
                throw new IllegalArgumentException("a head is an event id");
            }
            heads.add(new EventId(s.value()));
        }
        var rows = lines.subList(1, lines.size());
        for (var row : rows) {
            if (!(Json.parse(row) instanceof Json.Obj o)) {
                throw new IllegalArgumentException("a row is an object");
            }
            Content.fromJson(o);
        }
        return new Snapshot(header.integer("position"), new Frontier(heads), rows);
    }

    /** The rows, parsed. */
    List<Content.Row> content() {
        return rows.stream().map(r -> Content.fromJson((Json.Obj) Json.parse(r))).toList();
    }
}
