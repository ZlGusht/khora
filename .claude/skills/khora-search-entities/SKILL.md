---
name: khora-search-entities
description: Run a Smart Connections semantic search over the Khora vault. Use when the user wants entities related by meaning to a free-text query — concepts, themes, ideas — rather than by structural filters. Returns block-level paths with similarity scores. For structural retrieval (is_a, properties, etc.) use khora-query-entities.
---

# khora/search-entities

Atomic semantic retrieval via Smart Connections. Query is a free-text string. Output is a ranked list of block-level paths with cosine similarity scores.

## Inputs

A free-text query string. Optional `limit` (default 10).

Smart Connections embeds the query, compares against pre-computed block-level embeddings in `.smart-env/`, returns top-K.

## Procedure

Follow the queue-dir contract in `khora-crud.md` § "Khora plugin — escape valve for non-CLI Obsidian operations". The plugin command id is `khora:semantic-search`. Queue dir: `<tmpdir>/khora/semantic/{in,out}/`. Input file name: `<id>.json` with shape `{"query": "<text>", "limit": <N>}`. Output file name: `<id>.json` (envelope).

Concrete shape:

```bash
TMP=$(bash .claude/scripts/khora-tmp)   # Obsidian's temp dir (macOS or Windows) — never $TMPDIR (see khora-crud.md)
mkdir -p "$TMP/khora/semantic/in" "$TMP/khora/semantic/out"
ID=$(bash .claude/scripts/khora-uuid)
cat > "$TMP/khora/semantic/in/$ID.json" <<EOF
{"query": "<text>", "limit": 10}
EOF
obsidian vault=khora command id="khora:semantic-search"
# obsidian command blocks until the async callback completes; polling is a safety net.
for i in 1 2 3 4 5 6 7 8 9 10; do
  [ -f "$TMP/khora/semantic/out/$ID.json" ] && break
  sleep 1
done
cat "$TMP/khora/semantic/out/$ID.json"
rm -f "$TMP/khora/semantic/in/$ID.json" "$TMP/khora/semantic/out/$ID.json"
```

## Output

The envelope's `value` field: a list of `{path, score}` entries.

**Paths are block-level** — they address specific sections/chunks within notes. Format: `<vault-path>#<H1>#<H2>#…#{<chunk-index>}`. The `#<heading>` segments name the heading hierarchy down to the block; the trailing `#{n}` is a chunk index for splits within a heading (used when a section is long enough to chunk further, or when there's no heading at all and the entire note is chunk-indexed). Examples:

- `blob/Loop v2.0.0.md##Core Concepts#{2}` — chunk 2 under "Core Concepts" (the empty segment before `#Core Concepts` is the document root)
- `Vault/Chats/Looping v2 UPSF 1.md#<u>Assistant</u>[9]#4. Process (Metabolism)#{7}` — chunk 7 under nested headings

Preserve the block-level addressing; do NOT normalize to file paths unless the caller explicitly asks. Block-level is more precise than file-level and useful for context assembly.

Scores are cosine similarities in [0, 1]; higher is more relevant.

## Failure passthrough

Smart Connections disabled, env not initialized, collections still loading, lookup throws → `success: false` with `error` populated. Surface the error.

## Out of scope

- Combining with structural filters — the caller composes results with `khora-query-entities`
- Re-ranking / fusion — caller's job
- Embedding the query yourself — Smart Connections does it
- Reading block content given a block path — use `obsidian read` or a future content-fetch skill
