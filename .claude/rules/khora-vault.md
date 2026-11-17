# Khora Vault — Agent Operating Rules

These constrain how any agent interacts with this Khora vault. This repository is both the Obsidian vault and the agent's working directory. Entities live in `blob/`. Read on every session start.

## 1. Vault I/O is exclusively through the Obsidian CLI. No `eval`.

Read, write, search, update, delete, create, list — all go through `obsidian` and its first-class verbs, or through the khora plugin's queue-dir commands (see `khora-crud.md`). Never use Read/Edit/Write tools or any direct file access on `blob/`. If the right verb isn't known, ask the CLI: `obsidian help`, `obsidian help <verb>`.

**`obsidian eval` is banned** — for every operation. If a verb doesn't exist for what you need: (a) discover the right verb via help, (b) decompose into CLI primitives (read → modify locally → create with overwrite), or (c) tell the person it's a missing capability. Scripts that do real structural work on top of the CLI (e.g. `khora-validate-entity.py`) are fine; scripts that just wrap eval are not.

**Headless verbs only.** Never `obsidian open`, `obsidian focus`, or any palette command that acts on the active editor — they race the person's own typing in Obsidian. Use `read`, `create` (without `open`), `files`, `search`, `move`, `delete`, `backlinks`, `aliases`, `property:*`, plus the `khora:*` plugin commands.

**Why:** the vault has structural conventions (frontmatter shape, versioned wikilinks, TOML body schemas) that direct file writes quietly violate.

## 2. Discover at runtime, don't assume.

The Obsidian CLI shipped in v1.12 (Feb 2026) and evolves. Plugin commands are reached via `obsidian commands` then `obsidian command id="<plugin:cmd>"`. Always query, never invent.

## 3. Inheritance is mandatory.

Every entity has an `is_a` chain that resolves to `[[BaseEntity v1.0.0]]`. Schemas live in each universal's TOML body (`[Metadata.Relationships]`, `[Metadata.Properties]`, `[Data]`). New universals go in `blob/`.

## 4. Vocabulary is closed — derived from is_a schemas.

An entity's allowed `relationships__<key>` set is the union of `[Metadata.Relationships]` declarations across its is_a chain. Same for `properties__<key>` and `[Metadata.Properties]`. A key no ancestor declares is not allowed.

Adding a new relationship or property key is an **ontology change**: amend the schema of the universal that should host it, and only with the person's explicit consent in the conversation. Explain the change in plain words when asking.

## 5. Versioning is semver and non-migrating.

Universals carry a version in their name (`Letter v1.0.0`). Major = breaking change. Existing instances stay bound to the version they were created against.

**Wikilinks resolve by exact filename only.** Aliases are UI sugar and do not make `[[Letter]]` resolve to `Letter v1.0.0.md`. Always use exact, versioned targets: `[[Letter v1.0.0]]`. Dangling wikilinks (to entities that don't exist here) are valid — they mark something that may be wanted later.

## 6. Migratability and integrity are invariants.

Output stays in vault-native conventions only — frontmatter shape, wikilinks, TOML body, BaseEntity is_a chain. Everything must survive being copied into another Khora vault unchanged.

## 7. Assemble context before substantive vault work.

Before authoring, modifying, or making ontology decisions, invoke `khora-assemble-context`. If a process entity is in scope, read it and pass its `ContextAssembly` recipe if it declares one. Exempt: read-only mechanical operations and targeted single-entity updates.

## 8. New capability lands as process entities, not firmware.

`.claude/` (rules, skills, agents, scripts) and the khora plugin are firmware. New behaviour the person wants is written as a process entity in `blob/` (`is_a [[Process(n) v1.0.0]]`) and run via `[[Execute Process v1.0.0]]` — not as new skills or rules. Fixing a broken firmware piece in place is fine.

## 9. Entity bodies hold durable content, not slice-in-time context.

Don't fill entity bodies with session state ("we just did X", "next step is Y"). Ask: what should this hold for someone reading it months from now? Transient run context goes into `[[Letter v1.0.0]]` deposits anchored (`in_context_of`) to the relevant Process Instance.

---

## Pinned operational notes

- **Every CLI call carries `vault=khora` before the verb:** `obsidian vault=khora read path="blob/Letter v1.0.0.md"`. After the verb it is silently ignored.
- **`khora` is the repository folder's name.** A clone in a folder with another name uses that name instead. Scripts take the name from the folder (or `KHORA_VAULT`) and refuse to run if Obsidian's vault of that name is not this repository: Obsidian matches vault names without regard to case, so another vault called `Khora` would otherwise answer.
- **Always address entities by path with explicit `.md`:** `path="blob/<name>.md"`. The bare `name=` form lands new files at the vault root, and the CLI strips a trailing `.0` from versioned names as if it were an extension.
- **`property:set` takes `type=text|list|number|checkbox|date|datetime`.** `type=list` writes a YAML array and *replaces* the list — to append, read, splice locally, write back.
- **Process Instances are dual-`is_a`.** A run of process X is `is_a` both `[[Process Instance v1.0.0]]` and `[[X]]`, with `identity__type: particular`. Query runs of X with `is_a contains [[X]] AND identity__type = "particular"`; the definition of X has `identity__type = "universal"`.
- **Environment setup and troubleshooting (macOS and Windows)** live in `khora-setup.md`.
