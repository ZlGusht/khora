---
name: khora-query-entities
description: Run a Dataview (DQL) query against the Khora vault. Use when the user wants entities matching structural criteria — by is_a, properties, relationships, folder, file metadata, or any DQL-expressible filter. Returns the Dataview result object. For semantic / free-text retrieval use khora-search-entities instead.
---

# khora/query-entities

Atomic structural retrieval via Dataview. The caller composes the full DQL — no flag DSL, full DQL surface available. Composability with other skills happens at the caller level.

## Inputs

A complete DQL string. Examples:

- `TABLE file.name FROM "blob" WHERE contains(relationships__is_a, [[Letter v1.0.0]])`
- `LIST FROM "blob" WHERE properties__priority > 3`
- `TABLE file.mtime FROM "blob" WHERE econtains(aliases, "Loop") SORT file.mtime DESC LIMIT 10`

**Important — DQL does NOT resolve aliases.** Wikilinks in `contains(relationships__is_a, [[X]])` are matched as exact wikilink objects, not via alias indirection. Always use the versioned form (`[[Letter v1.0.0]]`) when filtering by structural references — entities pin their `is_a` to specific versions. The third example above queries via the `aliases` frontmatter list directly when you actually want to find entities aliased to a name.

Khora uses the frontmatter `tags` list (YAML array, values without `#`). Query it as `tags` — not `file.tags`. Property and relationship fields are likewise flat top-level frontmatter keys (`properties__<key>`, `relationships__<key>`).

## Procedure

Follow the queue-dir contract in `khora-crud.md` § "Khora plugin — escape valve for non-CLI Obsidian operations". The plugin command id is `khora:dataview-query`. Queue dir: `<tmpdir>/khora/dataview/{in,out}/`. Input file name: `<id>.dql` (raw DQL text). Output file name: `<id>.json` (envelope).

Concrete shape:

```bash
TMP=$(bash .claude/scripts/khora-tmp)   # Obsidian's temp dir (macOS or Windows) — never $TMPDIR (see khora-crud.md)
mkdir -p "$TMP/khora/dataview/in" "$TMP/khora/dataview/out"
ID=$(bash .claude/scripts/khora-uuid)
cat > "$TMP/khora/dataview/in/$ID.dql" <<'EOF'
<the DQL string goes here>
EOF
obsidian vault=khora command id="khora:dataview-query"
# obsidian command blocks until the async callback completes; the polling loop
# below is a safety net for edge cases, not a hard requirement.
for i in 1 2 3 4 5 6 7 8 9 10; do
  [ -f "$TMP/khora/dataview/out/$ID.json" ] && break
  sleep 1
done
cat "$TMP/khora/dataview/out/$ID.json"
rm -f "$TMP/khora/dataview/in/$ID.dql" "$TMP/khora/dataview/out/$ID.json"
```

## Output

The envelope's `value` field, surfaced as-is. For TABLE queries it's `{ type, headers, values, idMeaning }`. Preserve the shape — callers may consume it programmatically.

## Failure passthrough

Plugin not loaded, Dataview disabled, malformed DQL, runtime error → `success: false` with `error` populated. Surface the error to the caller; do not try to repair (e.g. don't auto-quote-escape DQL — that's the caller's call).

## Out of scope

- Combining with semantic results — use `khora-search-entities` and let the caller fuse
- Result re-shaping or filtering beyond what DQL itself does — caller composes
- Caching across invocations — every call goes to the plugin
