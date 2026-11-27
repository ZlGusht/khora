package khora.query.neo4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Assertion;
import khora.events.EventId;
import khora.events.Identity;
import khora.events.json.Json;

/**
 * A {@link Neo4jDatastore}'s content at one position of its log, as canonical JSON, one row per
 * line: each thing with its literal attributes, then each relationship. A datastore rebuilt from a
 * snapshot and the log after it equals one built from the start of the log (ADR-0XY decision 10).
 *
 * <p>Rows: {@code {"type": "entity" | "thing", "id", "attributes": {attribute: [value, ...]}}} and
 * {@code {"type": "relation", "statement", "thing", "attribute", "target"}}. Values are written as
 * assertions write them.
 *
 * <p>Provisional: kept whole in memory and in one file; no compression, no checksum.
 *
 * @param position the last log position taken in
 * @param last the id of the event at {@code position}, if any
 * @param rows the rows, things first by id, then relations by statement
 */
public record Snapshot(long position, Optional<EventId> last, List<String> rows) {
    static final int VERSION = 3;

    public Snapshot {
        if (position < 0) {
            throw new IllegalArgumentException("position is negative: " + position);
        }
        Objects.requireNonNull(last, "last");
        if ((position == 0) != last.isEmpty()) {
            throw new IllegalArgumentException("a snapshot has a last event exactly when its position is not 0");
        }
        rows = List.copyOf(rows);
        rows.forEach(Snapshot::check);
    }

    /** Writes the snapshot to {@code file}: a header line, then one line per row. */
    public void write(Path file) throws IOException {
        var header = new HashMap<String, Json>();
        header.put("type", new Json.Str("snapshot"));
        header.put("version", new Json.Int(VERSION));
        header.put("position", new Json.Int(position));
        last.ifPresent(l -> header.put("last", new Json.Str(l.value())));
        var lines = new ArrayList<String>(rows.size() + 1);
        lines.add(Json.Obj.of(header).canonical());
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
        if (lines.isEmpty() || !(Json.parse(lines.get(0)) instanceof Json.Obj header)
                || !header.get("type").equals(Optional.of(new Json.Str("snapshot")))) {
            throw new IllegalArgumentException("the first line is not a snapshot header");
        }
        if (header.integer("version") != VERSION) {
            throw new IllegalArgumentException("snapshot version " + header.integer("version") + " is not " + VERSION);
        }
        var last = header.get("last").isPresent() ? Optional.of(new EventId(header.str("last"))) : Optional.<EventId>empty();
        return new Snapshot(header.integer("position"), last, lines.subList(1, lines.size()));
    }

    /** The rows, parsed. */
    List<Json.Obj> parsed() {
        return rows.stream().map(r -> (Json.Obj) Json.parse(r)).toList();
    }

    /** Refuses a row that is not one {@link Neo4jDatastore} writes. */
    private static void check(String row) {
        if (!(Json.parse(row) instanceof Json.Obj o) || o.get("type").isEmpty()) {
            throw new IllegalArgumentException("a row is an object with a type");
        }
        switch (o.str("type")) {
            case "entity", "thing" -> {
                members(o, Set.of("type", "id", "attributes"));
                new Identity(o.str("id"));
                for (Map.Entry<String, Json> a : o.obj("attributes").members().entrySet()) {
                    new Identity(a.getKey());
                    if (!(a.getValue() instanceof Json.Arr values) || values.items().isEmpty()) {
                        throw new IllegalArgumentException("an attribute holds one or more values");
                    }
                    for (var v : values.items()) {
                        if (!(v instanceof Json.Obj vo) || !(Assertion.valueOf(vo) instanceof Assertion.Literal)) {
                            throw new IllegalArgumentException("an attribute of a thing holds literals");
                        }
                    }
                }
            }
            case "relation" -> {
                members(o, Set.of("type", "statement", "thing", "attribute", "target"));
                List.of("statement", "thing", "attribute", "target").forEach(k -> new Identity(o.str(k)));
            }
            default -> throw new IllegalArgumentException("unknown row type " + o.str("type"));
        }
    }

    private static void members(Json.Obj o, Set<String> keys) {
        if (!o.members().keySet().equals(keys)) {
            throw new IllegalArgumentException("a " + o.str("type") + " row has the members " + keys
                    + ", not " + o.members().keySet());
        }
    }
}
