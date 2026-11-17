---
areas:
  - "[[Kit]]"
---

# khora kit, slice 1: implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn this repository into the khora kit's vault: entities in `blob/`, decisions from transcripts through pull requests, documents written from decisions, rendered dashboards and a stale report.

**Architecture:** The repository root is an Obsidian vault. Entities, processes and rules are content; three small Python scripts do the deterministic work (validation, canvas geometry, staleness). Everything reaches entities through the Obsidian CLI and the khora plugin.

**Tech Stack:** Obsidian 1.13 with its CLI; plugins khora 0.1.0, Dataview 0.5.68 (JavaScript queries on), Smart Connections 4.5.0; Python 3.8+ with PyYAML; Git Bash on Windows; `gh`; Claude Code with superpowers.

**Spec:** `docs/khora-kit.md` (approved). Decisions are cited as "kit d12".

## Sources

| Name | Path |
| --- | --- |
| Loops starter | The team's loops starter export (2026-11-13), kept outside this repository |
| Unity | The team's venture vault, kept outside this repository |

## Global Constraints

- Transcripts and exported conversations never enter the repository: it is public. They live in `KHORA_TRANSCRIPTS`, or `khora-transcripts` beside the repository, and are only read.
- The entity folder is `blob/`, lower case. Every reused file that says `Blob` or `Blob/` is rewritten.
- The vault name is the name of the repository's root folder (`khora` here). No script hard-codes it. `KHORA_VAULT` overrides it.
- Every CLI call carries `vault=<name>` before the verb.
- Markdown, JSON, canvas and script files are committed with LF endings.
- `properties__author` is set on every entity written: a team member's name, or `agent:<model>`.
- Every entity written passes `bash .claude/scripts/khora-validate "<name>"`.
- Copied kinds keep the copied text, with only the changes a task names.
- Anchors in copied entities that point at the source vault's own hubs become `[[Kit]]`.
- Writing in `CLAUDE.md`, rules and process bodies follows the repository's style: plain short sentences, one idea per bullet, no filler.

## Review Focus

1. A clone whose folder isn't named `khora` (e.g. `khora-main`): scripts must target the vault Obsidian knows, via the folder name or `KHORA_VAULT`, and say which one they used. Test in Task 2.
2. Obsidian not running: every script fails within 30 seconds with "Obsidian is not running or the CLI is off", never hangs. Test in Task 2.
3. A trace marker naming a Decision id that doesn't exist: reported as stale ("unknown decision"), never a crash. Test in Task 6.
4. `gh` missing or not logged in: `khora-stale` skips the issue and pull-request checks and says so in the report; the other checks still run. Test in Task 6.
5. A Windows checkout with CRLF entities: frontmatter still parses and the stale report still finds markers. Test in Task 6 (CRLF fixture), and `.gitattributes` in Task 1.

---

### Task 1: Vault layout and Obsidian config

**Files:**
- Create: `blob/.gitkeep`, `src/.gitkeep`, `canvas/.gitkeep`, `dashboards/views/.gitkeep`. No `transcripts/` folder: transcripts never enter the repository (see Global Constraints).
- Create: `.gitignore`, `.gitattributes`
- Create: `.obsidian/app.json`, `.obsidian/community-plugins.json`
- Create: `.obsidian/plugins/{khora,dataview,smart-connections}/` (copied from the loops starter)
- Create: `.obsidian/plugins/dataview/data.json` (copied from Unity)

**Interfaces:**
- Produces: the folders every later task writes into.

- [ ] **Step 1: Create the folders** with `.gitkeep` files.
- [ ] **Step 2: Write `.gitignore`** with exactly: `.obsidian/workspace*.json`, `.smart-env/`, `dashboards/stale.md`, `__pycache__/`.
- [ ] **Step 3: Write `.gitattributes`** with `* text=auto eol=lf`.
- [ ] **Step 4: Copy the three plugins** from the loops starter's `.obsidian/plugins/`. Copy Unity's `.obsidian/plugins/dataview/data.json` over the starter's (it has `enableDataviewJs: true`).
- [ ] **Step 5: Write `.obsidian/community-plugins.json`** as `["dataview", "smart-connections", "khora"]`, and `.obsidian/app.json` as `{"userIgnoreFilters": ["src/"]}`.
- [ ] **Step 6: A team member opens the repository folder in Obsidian** ("Open folder as vault"), trusts it, and turns community plugins on.
- [ ] **Step 7: Verify.**
  Run: `obsidian vault=khora commands | grep -c "khora:"`
  Expected: `5` or more.
- [ ] **Step 8: Commit.** `git add -A && git commit -m "Set up the repository as the kit's vault"`

### Task 2: Firmware

**Files:**
- Create: `.claude/` (copied from the loops starter: `rules/`, `skills/`, `agents/`, `scripts/`, `settings.json`)
- Create: `.claude/scripts/khora-canvas`, `.claude/scripts/khora-canvas.py` (copied from Unity)
- Create: `.claude/scripts/khora_common.py`
- Modify: `.claude/scripts/khora-validate-entity.py`, `.claude/scripts/khora-canvas.py`, every rule and skill (`Blob` → `blob`, `vault=Khora` → `vault=<name>`)
- Test: `.claude/scripts/tests/test_khora_common.py`

**Interfaces:**
- Produces, in `khora_common.py`:
  - `vault_name() -> str`: `KHORA_VAULT` if set, else the basename of `git rev-parse --show-toplevel`.
  - `obsidian(*args: str, timeout: int = 30) -> str`: runs `obsidian vault=<vault_name()> *args`, returns stdout with leading whitespace stripped, UTF-8. Raises `KitError("Obsidian is not running or the CLI is off")` on timeout or a connection error.
  - `class KitError(Exception)`.
- `khora-canvas.py` paths: `ENTITIES = "blob"`, `VIEWS = "dashboards/views"`. `graph_dump()` queries `FROM "blob"`.

- [ ] **Step 1: Write the failing tests** in `test_khora_common.py` (stdlib `unittest`):
  - `test_vault_name_env_override`: with `KHORA_VAULT=x`, `vault_name() == "x"`.
  - `test_vault_name_from_repo_folder`: in a temp git repo named `khora-main`, `vault_name() == "khora-main"`.
  - `test_obsidian_timeout_raises_kiterror`: with `obsidian` replaced by a script that sleeps 5 s and `timeout=1`, `obsidian("files")` raises `KitError` whose message is `"Obsidian is not running or the CLI is off"`.
- [ ] **Step 2: Run them to see them fail.**
  Run: `python -m unittest discover -s .claude/scripts/tests`
  Expected: FAIL, `ModuleNotFoundError: khora_common`.
- [ ] **Step 3: Implement `khora_common.py`.**
- [ ] **Step 4: Run the tests.** Same command. Expected: 3 tests OK.
- [ ] **Step 5: Copy the firmware and the canvas engine**, then make `khora-validate-entity.py` and `khora-canvas.py` call `khora_common.obsidian()` instead of building `["obsidian", "vault=…"]` themselves. Rewrite `Blob` to `blob`, and every `vault=Khora` in rules and skills to `vault=<name>` with one line in `khora-vault.md` saying `<name>` is the repository folder name.
- [ ] **Step 6: Verify nothing still hard-codes the old names.**
  Run: `grep -rn "Blob\|vault=Khora\|vault=Unity\|VAULT = \"" .claude`
  Expected: no output.
- [ ] **Step 7: Commit.** `git commit -m "Add the kit's firmware with the vault name taken from the repository"`

### Task 3: Kinds copied from the starter and Unity

**Files:**
- Create in `blob/`: `BaseEntity v1.0.0`, `Process(n) v1.0.0`, `Process Instance v1.0.0`, `Execute Process v1.0.0`, `Deposit process v1.0.0`, `Letter v1.0.0`, `Loop v2.0.0`, `Inception v1.0.0`, `Laydown v2.0.0`, `Operate v2.0.0`, `Observe v2.0.0`, `Process(v) v2.0.0`, `Cadence Loop v1.0.0`, `Weekly Loop v1.0.0` (from the starter, unchanged)
- Create in `blob/`: `Call v1.0.0`, `Person v1.0.0`, `Decision v1.0.0`, `Call Ingestion v1.0.0`, `Render Canvas v1.0.0` (from Unity, changed as below)

**Interfaces:**
- Produces: `Decision v1.0.0` with the fields in kit d5; `Call Ingestion v1.0.0` that writes no Tasks.

- [ ] **Step 1: Copy the 14 starter entities** unchanged.
- [ ] **Step 2: Copy Unity's five,** with these changes only:
  - All: the source vault's hub anchors → `[[Kit]]`; `Blob/` → `blob/`; `Dashboards/` → `dashboards/`.
  - `Call v1.0.0`: transcripts live outside the repository; a Call names its transcript's file.
  - `Decision v1.0.0`: add `quote = { type="text", description="The transcript line where it was decided, verbatim." }` and `answers = { type="list<connection>", description="Questions this Decision answers.", optional=true }`; add `design` to `kind` values, described as "a choice about khora's model or architecture, the kind ADRs are made of".
  - `Call Ingestion v1.0.0`: Step 6 becomes "List work items as proposed issues in the pull request description: title, assignee, the Call and Decision each one comes from. Do not create Task entities or issues." Add a Step 0: "Work on a branch `ingest/YYYY-MM-DD`." Add a final step: "Open one pull request labelled `ingestion` with every created entity and the proposed issues." Step 5 also writes `quote`. Step 10 re-renders `dashboards/Areas.canvas` and `dashboards/Loops.canvas`.
  - `Render Canvas v1.0.0`: views live in `dashboards/views/`; the canvas list is Areas, Loops and Stale.
- [ ] **Step 3: Validate all 19.**
  Run: `for f in blob/*.md; do bash .claude/scripts/khora-validate "$(basename "$f" .md)" | grep -m1 Result; done`
  Expected: 19 lines of `Result: PASS`.
- [ ] **Step 4: Commit.** `git commit -m "Add the kit's kinds, copied from the loops starter and Unity"`

### Task 4: Areas, Questions and the kit's own processes

**Files:**
- Create in `blob/`: `Area v1.0.0`, `Question v1.0.0`, `Update View v1.0.0`, `Team Weekly v1.0.0`
- Create in `blob/`, each `is_a [[Area v1.0.0]]`: `Log`, `Surfaces`, `Surface logic`, `Scopes`, `Runtime`, `Behaviors`, `Security`, `Product`, `Design`, `Kit`
- Modify: frontmatter of `docs/adr/*.md`, `docs/PRD.md`, `docs/implementation-plan.md`, `docs/khora-kit.md`, `docs/khora-kit-plan.md`, `CLAUDE.md`

**Interfaces:**
- Produces:
  - `Area v1.0.0` with no fields of its own.
  - Documents name their Areas: each document in `docs/` (and `CLAUDE.md`) starts with frontmatter `areas: ["[[<Area>]]", …]`. That is the only frontmatter key a document carries.
  - `Question v1.0.0`: `originates_in` (connection, optional), `lean` (text).
  - Trace markers, defined in `Update View v1.0.0`: `<!-- d:<decision uuid> -->` after a sentence that decides; `<!-- q:<question uuid> -->` in an open-question row.
- Document `areas` values: Log → `docs/adr/khora-log.md`; Surfaces → `docs/adr/surfaces.md`; Surface logic → `docs/adr/surface-logic.md`; Scopes → `docs/adr/scopes-khoras-and-rights.md`; Runtime → `docs/adr/runtime-and-executors.md`; Behaviors → `docs/adr/behaviors-and-processes.md`; Security → `docs/adr/security-model.md`; Product → `docs/PRD.md`, `docs/implementation-plan.md`; Design → `docs/adr/origins-behaviors-surfaces-executors.md`; Kit → `docs/khora-kit.md`, `docs/khora-kit-plan.md`, `CLAUDE.md`.

- [ ] **Step 1: Write `Area v1.0.0` and `Question v1.0.0`.**
- [ ] **Step 2: Write `Update View v1.0.0`** (`is_a [[Process(n) v1.0.0]]`). Its steps:
  1. Read the document and its Area.
  2. Query active Decisions `in_context_of` the Area that the document doesn't trace yet.
  3. For each, add or change only the sentences that state it, in the document's own conventions, each ending with its `d:` marker. Superseded Decisions: replace their sentences and markers.
  4. Add nothing a Decision doesn't state. A sentence needed only to connect two others is allowed if it decides nothing.
  5. Open questions: one row per open Question, with its `q:` marker.
  6. Open a pull request; the reviewer checks each new sentence against its Decision.
- [ ] **Step 3: Write `Team Weekly v1.0.0`** (`is_a [[Weekly Loop v1.0.0]]`), overriding `Subprocesses` with Weekly Loop's four phases, plus:
  - Observe, before the interview: read `dashboards/stale.md`; list open `ingestion` pull requests; compute the acceptance rate since the last weekly (Decisions merged active ÷ Decisions proposed); sample three active Decisions and ask whether each still holds.
  - Process(v): close `ingestion` pull requests older than this weekly's start, and deposit a Letter on each Call they carried (kit d11).
  - The execution log is the week's Calls, merged pull requests and closed issues, not Daily Loops.
- [ ] **Step 4: Write the 10 Areas, and add `areas` frontmatter to each document** listed above.
- [ ] **Step 5: Validate all 14.** Same loop as Task 3. Expected: every new entity `Result: PASS`.
- [ ] **Step 6: Commit.** `git commit -m "Add Areas, Questions, Update View and Team Weekly"`

### Task 5: Live views

**Files:**
- Create in `dashboards/views/` (copied from Unity, `"Blob"` → `"blob"`): `loop-card.js`, `open-loops.js`, `closed-loops.js`, `decisions.js`, `declared-state.js`, `type-count.js`, `type-links.js`, `calls.js`
- Create: `dashboards/views/questions.js`
- Not copied: `tasks.js`, `contacts.js` (no Task or Organization kinds).

**Interfaces:**
- `decisions.js` input becomes `{kinds?: string[], area?: string, loop?: string, limit?: number}`. With `area`, it lists active Decisions `in_context_of` that Area. No default loop.
- `questions.js` input `{area: string, limit?: number}`: Questions `in_context_of` the Area with no inbound `answers` from an active Decision, newest first, each with its `lean`.

- [ ] **Step 1: Copy and rewrite the eight views.**
- [ ] **Step 2: Write `questions.js`.**
- [ ] **Step 3: Verify.** Create a scratch note `canvas/views-check.md` with one `dataviewjs` block per view, using `area: "Kit"`, and open it in Obsidian. Expected: each block renders, either a list or `*None.*`, with no red error. Delete the note.
- [ ] **Step 4: Commit.** `git commit -m "Add live dashboard views"`

### Task 6: The stale report

**Files:**
- Create: `.claude/scripts/khora-stale`, `.claude/scripts/khora_stale.py`
- Test: `.claude/scripts/tests/test_khora_stale.py`, fixtures in `.claude/scripts/tests/fixtures/stale/`

**Interfaces:**
- Consumes: `khora_common.obsidian`, `KitError`; `khora-canvas graph` JSON (`[{name, fm}]`).
- Produces:
  - `@dataclass Finding(kind: str, subject: str, reason: str)`. `kind` is one of `sentence`, `document`, `transcript`, `dashboard`, `issue`, `question`, `proposal`, `skipped`.
  - `find_stale(snapshot: Snapshot, today: date) -> list[Finding]`: pure.
  - `@dataclass Snapshot(entities: list[dict], documents: dict[str, Doc], transcripts: list[str], dashboards: dict[str, Dash], issues: list[dict] | None, proposals: list[dict] | None, changed: dict[str, date])`. `Doc(text: str, committed: date, areas: list[str])`; `Dash(rendered: date | None, spec: str)`. `None` for issues or proposals means `gh` was unavailable.
  - `collect() -> Snapshot`: all I/O (graph via `khora-canvas graph`, documents and dates via git, `gh issue list --state open --json number,title,body`, `gh pr list --state open --label ingestion --json number,title,createdAt`).
  - `render_report(findings: list[Finding], today: date) -> str`: Markdown, grouped by `kind`, titled `# Stale · YYYY-MM-DD`.
  - CLI: `bash .claude/scripts/khora-stale` writes `dashboards/stale.md` and prints the count; `--stdout` prints the report instead.

Rules, from kit d17:

| kind | Stale when |
| --- | --- |
| sentence | a `d:` marker names a Decision whose status is superseded or reversed, or no Decision has that id ("unknown decision") |
| document | its `areas` frontmatter names an Area that has active Decisions with `decided_on` after the document's last commit, and none of them is traced in it |
| transcript | no Call's `transcript` link names its file |
| dashboard | `rendered` is older than the newest `changed` date among entities whose kind appears on the Spec card's `Kinds:` line (all entities if the line is absent), or `rendered` is missing |
| issue | open, and its body names a Decision file that is superseded or reversed |
| question | a `q:` marker names a Question that an active Decision `answers` |
| proposal | an `ingestion` pull request created before the newest `Team Weekly v1.0.0` instance's `started_at` |
| skipped | `gh` unavailable: one finding per skipped check, reason `"gh not available"` |

- [ ] **Step 1: Write the failing tests**, one per row, each building a `Snapshot` by hand and asserting the exact `Finding`. Plus:
  - `test_unknown_decision_marker`: a `d:` id with no entity → `Finding("sentence", "docs/x.md", "unknown decision <id>")`.
  - `test_gh_unavailable_skips_only_github_checks`: `issues=None, proposals=None` → two `skipped` findings, and the transcript check still reports.
  - `test_crlf_document_markers_found`: a document text with `\r\n` line endings still yields its `sentence` finding.
  - `test_fresh_vault_reports_nothing`: a consistent snapshot → `[]`.
- [ ] **Step 2: Run them to see them fail.**
  Run: `python -m unittest discover -s .claude/scripts/tests`
  Expected: FAIL, `ModuleNotFoundError: khora_stale`.
- [ ] **Step 3: Implement `find_stale` and `render_report`.**
- [ ] **Step 4: Run the tests.** Expected: all OK.
- [ ] **Step 5: Implement `collect()` and the CLI**, with the same Python-finding wrapper as `khora-validate`.
- [ ] **Step 6: Verify against the vault.**
  Run: `bash .claude/scripts/khora-stale --stdout`
  Expected: a report titled `# Stale · <today>`. With no Calls yet and no transcripts, its only findings are dashboards not yet rendered.
- [ ] **Step 7: Commit.** `git commit -m "Add the stale report"`

### Task 7: Dashboards

**Files:**
- Create: `dashboards/Areas.canvas`, `dashboards/Loops.canvas`, `dashboards/Stale.canvas`

**Interfaces:**
- Consumes: `khora-canvas render`, the views from Task 5, `dashboards/stale.md` from Task 6.

- [ ] **Step 1: Write each dashboard's Spec card** (node id `spec`, colour 6), in the shape of Unity's Loops Spec: Purpose, what to show, context, live vs agent, edges (none), triggers, and a `Kinds:` line.
  - Areas: one labelled group per Area: live `decisions` card, live `questions` card, the documents whose `areas` name it, as file cards, and an agent card listing open issues that link the Area's Decisions. `Kinds: Area v1.0.0, Decision v1.0.0, Question v1.0.0`.
  - Loops: Unity's Loops Spec, without contacts and Tasks; issues in place of Tasks; `Team Weekly v1.0.0` runs as the top loop. `Kinds: Loop v2.0.0, Team Weekly v1.0.0, Letter v1.0.0, Call v1.0.0`.
  - Stale: a header card and one file card embedding `dashboards/stale.md`, with the rule table from Task 6 as a legend. `Kinds:` absent.
- [ ] **Step 2: Render each** by executing `Render Canvas v1.0.0` through `Execute Process v1.0.0`.
- [ ] **Step 3: A team member looks at each in Obsidian** (kit d15). Fix what they flag and re-render.
- [ ] **Step 4: Verify.** Run `bash .claude/scripts/khora-stale --stdout`. Expected: no `dashboard` findings.
- [ ] **Step 5: Commit.** `git commit -m "Add the Areas, Loops and Stale dashboards"`

### Task 8: CLAUDE.md and the setup rule

**Files:**
- Modify: `CLAUDE.md` (rewrite)
- Modify: `.claude/rules/khora-setup.md`
- Modify: `README.md` (one short section)

- [ ] **Step 1: Rewrite `CLAUDE.md`** with these sections, in this order:
  1. **What this repository is:** khora's design and code, run as the kit's vault (`docs/khora-kit.md`).
  2. **Layout:** kit d1, one line per folder.
  3. **How work flows:** recording → transcripts folder outside the repository → `Call Ingestion` → pull request → merge accepts → issues created → `Update View` → stale report. One line each, citing kit decisions.
  4. **Rules for agents:** never write a sentence in `docs/` that no Decision states (kit d13); never edit an accepted Decision except to supersede it, in a pull request (kit d12); never change `canvas/` (kit d14); work items are issues, not entities.
  5. **Design writing conventions:** the current `CLAUDE.md`'s Conventions and Markers sections, unchanged.
  6. **Superpowers:** a run is a Loop instance (kit d18); specs and plans go to `docs/`, not `docs/superpowers/`.
  7. **Entity rules:** five lines, pointing to `.claude/rules/`.
  8. **Known follow-ups:** the current list, unchanged.
- [ ] **Step 2: Update `khora-setup.md`:** the vault is the cloned repository folder; Git is already present; recordings are Wispr Flow exports saved outside the repository, in `KHORA_TRANSCRIPTS` or `../khora-transcripts/`, as `YYYY-MM-DD <topic>.md`; `gh auth login` is part of setup.
- [ ] **Step 2b: Pin superpowers for the project.** In `.claude/settings.json`, add `"enabledPlugins": {"superpowers@claude-plugins-official": true}`, so every team member's Claude Code offers to install it on first run. The setup check confirms `/plugin` lists superpowers at v7.0.2 or later.
- [ ] **Step 3: Add a README section** "Working in this repository", three lines: open the folder in Obsidian, run `claude`, read `docs/khora-kit.md`.
- [ ] **Step 4: Verify.** Start a fresh Claude Code session in the repository and ask "where do decisions come from, and where do I put a recording?" Expected: the answer names the transcripts folder outside the repository, `Call Ingestion`, and the pull request.
- [ ] **Step 5: Commit.** `git commit -m "Rewrite CLAUDE.md for the kit"`

### Task 9: The first Call

**Files:**
- Create, outside the repository: `../khora-transcripts/2026-11-17 Designing the khora kit.md`. Never inside it: the repository is public.
- Created by the run: the Call, its Decisions and Questions, a pull request

- [ ] **Step 1: Export the conversation** that produced `docs/khora-kit.md` (Claude Code `/export`) into that file, outside the repository, unedited.
- [ ] **Step 2: Run `Call Ingestion v1.0.0`** on it through `Execute Process v1.0.0`. Decisions are `in_context_of [[Kit]]`, kind `rule` or `design`.
- [ ] **Step 3: A team member reviews the pull request** against the quotes, and merges it.
- [ ] **Step 4: Run `Update View v1.0.0` on `docs/khora-kit.md`** (kit d19). Expected: each decision in the document carries a `d:` marker.
- [ ] **Step 5: Verify.** Run `bash .claude/scripts/khora-stale --stdout`. Expected: no `transcript` or `document` findings for the Kit Area.

## Not in slice 1

The trace check and the protected-Decision check (kit d21, slice 2).
