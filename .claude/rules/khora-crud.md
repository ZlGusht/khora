# Khora Vault — CRUD Patterns

The `obsidian` CLI is the only channel for vault I/O (`khora-vault.md` rule 1). Run `obsidian help` and `obsidian help <verb>` to discover the current verb surface. This file is about patterns and constraints, not verb syntax.

## What the CLI directly supports

- **Reading** full content (frontmatter + body): `obsidian vault=<vault> read path="blob/<name>.md"`. Output starts with a blank line — strip leading whitespace before parsing.
- **Frontmatter properties:** `property:read`, `property:set` (with `type=`), `property:remove`.
- **Body append / prepend.** No mid-body insertion.
- **Creating** a file with literal content: `obsidian vault=<vault> create path="blob/<name>.md" content="…"`. Add `overwrite` to replace an existing file. Never pass `open`.
- **Deletion.** Backlinks pointing at a deleted entity are not repaired automatically.
- **Backlinks:** `obsidian vault=<vault> backlinks path="blob/<name>.md"` — use before deleting or relating.

## Creating an entity

Write the full file content yourself — no Templater, no templates:

```yaml
---
identity__uuid: <output of `bash .claude/scripts/khora-uuid`>
identity__type: universal | particular
identity__created_at: <YYYY-MM-DD, from `date +%F`>
identity__tags: []
relationships__is_a:
  - "[[<Parent> v1.0.0]]"
properties__author: <your name, or agent:<model> when an AI agent writes it — see below>
relationships__in_context_of:
  - "[[<anchor>]]"
---
```

…followed by the body (a ```toml fenced block for universals: `Description`, `[Metadata.Relationships]`, `[Metadata.Properties]`, `[Data]`, and for processes `Subprocesses`). Write the content to a temp file first, then pass it with `content="$(cat <file>)"` — this preserves newlines, quotes, and wikilinks. Validate immediately after (`khora-validate-entity`).

## Authorship

Every entity you create or substantially rewrite carries `properties__author` (declared on `[[BaseEntity v1.0.0]]`, so it is valid on any entity):

- **A human wrote it** (or dictated it and you only transcribed): their name, e.g. `properties__author: Jane Doe`. Ask once for the name and reuse it.
- **An AI agent wrote it** (you composed the content): `agent:` + your model or agent name, e.g. `properties__author: agent:claude-opus-5-5`.

The `agent:` prefix is the distinction — find everything agents wrote with `WHERE startswith(properties__author, "agent:")`. Don't overwrite an existing human author when you make a small edit to their entity.

## Patterns without a direct primitive

- **Mid-body edits:** read full content → modify locally → `create … overwrite`.
- **Appending to a list-shaped property:** read the current values, splice, write the full list back with `property:set type=list`.
- **Walking an is_a chain:** read the entity, then each parent, until `[[BaseEntity v1.0.0]]`.
- **Anything else:** use the khora plugin (below). Never `obsidian eval`.

## Khora plugin — queue-dir contract

The khora plugin (bundled in `.obsidian/plugins/khora/`) exposes Dataview queries and Smart Connections semantic search as palette commands that take their input from files.

```
<tmpdir>/khora/dataview/in/<id>.dql      ← you write (raw DQL)
<tmpdir>/khora/dataview/out/<id>.json    ← plugin writes
<tmpdir>/khora/semantic/in/<id>.json     ← you write {"query": "...", "limit": N}
<tmpdir>/khora/semantic/out/<id>.json    ← plugin writes
```

**`<tmpdir>` is Obsidian's temp folder, not yours.** Get it with `bash .claude/scripts/khora-tmp` — it resolves the right folder on macOS (`getconf DARWIN_USER_TEMP_DIR`) and Windows (`%LOCALAPPDATA%Temp`, converted for Git Bash). Do NOT trust `$TMPDIR` inside the agent's shell — the agent may run with a different temp dir than the Obsidian app, and the plugin would never see your request.

Steps:
1. `ID=$(bash .claude/scripts/khora-uuid)`; `TMP=$(bash .claude/scripts/khora-tmp)`; `mkdir -p "$TMP/khora/<cmd>/in" "$TMP/khora/<cmd>/out"`.
2. Write the request file into `in/`.
3. `obsidian vault=<vault> command id="khora:dataview-query"` (or `khora:semantic-search`).
4. Poll for `out/$ID.json` (sub-second sleeps, ~10 s timeout).
5. Read the envelope `{ id, success, value?, error? }`.
6. Delete both files.

Dataview paths in this vault: entities are under `"blob"`, e.g. `TABLE file.name FROM "blob" WHERE contains(relationships__is_a, [[Letter v1.0.0]])`.

## Post-change validation

After every mutation run `bash .claude/scripts/khora-validate "blob/<name>.md"` (via the `khora-validate-entity` skill). Invariants: is_a chain reaches `[[BaseEntity v1.0.0]]`; every relationship and property key is declared along the chain; frontmatter parses as YAML; the TOML body parses; required fields are present.
