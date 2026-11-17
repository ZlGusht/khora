# The khora kit

> **Status:** Draft
> **Date:** 17.11.2026
> **Related:** `docs/systems/application-development.md` (bootstrap step 1: an overlay on khora's repositories); ADR-0YA; ADR-0YD
> **Note:** a bootstrap document. Once the kit works, its decisions become Decision entities and this document becomes their view (decision 19).

## Context

The team talks decisions through, then has an AI write the ADRs, plans and docs. The write-ups add sentences nobody decided, and the decisions get lost under them. Context is spread across calls, documents, canvases and GitHub issues.

The kit is a provisional khora that runs in this repository with Obsidian, Claude Code and superpowers. It is a prosthetic until khora runs: workable, not the absolute system. It keeps every decision as an entity, generates documents only from accepted decisions, renders canvases from the graph, reports what is stale, and runs the team's work in loops.

The team already runs a vault of this kind for the venture (Unity). The kit reuses its kinds, its call ingestion and its canvas renderer, so both vaults share one vocabulary.

## Terms

| Term | Meaning |
| --- | --- |
| **Vault** | The repository opened in Obsidian. Its root is the repository root. |
| **Entity** | A Markdown file in `blob/` with frontmatter and a body. |
| **Kind** | What an entity is, declared by `relationships__is_a`. Every kind reaches `BaseEntity v1.0.0`. |
| **Area** | A hub entity for one design area. Every other entity links to at least one. |
| **Call** | One recorded discussion. It links its transcript file. |
| **Decision** | Something the team committed to, quoting the line where it was decided. |
| **Question** | An open question with its current lean. |
| **Task** | A commitment with an owner. |
| **Letter** | A friction, insight or signal deposited into a loop. |
| **Loop** | A process run in phases: Inception, Laydown, Operate, Observe, Process(v). |
| **Instance** | One run of a process: an entity that is a kind of both `Process Instance v1.0.0` and the process. |
| **Proposal** | An entity written by an agent and not yet merged. |
| **Acceptance** | A team member merging the pull request that carries a proposal. |
| **View** | A document in `docs/` or a canvas in `dashboards/`, generated from the graph. |
| **Stale** | A view or entity that no longer matches the graph it depends on (decision 17). |

## Goals, non-goals and anti-goals

**Goals:**
- Every decision the team makes is recorded once, with its source.
- Documents say only what accepted Decisions say.
- All context is in one repository: calls, decisions, documents, canvases and code.
- What is out of date is visible without anyone looking for it.
- The team works in loops, with a weekly rhythm.
- Automated development with superpowers runs inside loops.

**Non-goals:**
- Agents as entities. They come later.
- Replacing khora. The kit uses files, git and Obsidian, not khora's log.
- Services of our own. Everything runs on existing tools.

**Anti-goals** (the kit fails if these happen, even if it works):
- A view gains sentences no Decision backs.
- Acceptance becomes a rubber stamp.
- The kit's vocabulary drifts from Unity's.
- The kit grows into the product.

## Approaches

| Approach | Trade-off | Chosen |
| --- | --- | --- |
| **The repository root is the vault** | Obsidian config and plugins enter the repository | Yes: Decisions link to the views they produce, and everything is in one place |
| The vault in a subfolder | `docs/` falls outside the vault, so Decisions can't link to views | No |
| The vault in its own repository | Decisions, documents and code split again | No |

| Ontology | Trade-off | Chosen |
| --- | --- | --- |
| **Unity's kinds, extended where khora needs it** | Carries some venture-specific values | Yes: one vocabulary across the team's two vaults, and the kinds are already tested |
| New kinds for khora | Clean fit | No: two vocabularies for the same people |

## Decisions

### Layout

**1. The repository root is the vault.**
- `blob/`: entities.
- `docs/`: documents, including the existing design documents.
- `src/`: code.
- `transcripts/`: Wispr Flow exports, one Markdown file per recording.
- `canvas/`: canvases the team draws.
- `dashboards/`: canvases the kit renders, with their live views in `dashboards/views/`.

**2. Obsidian's shared config is committed; per-machine state is not.**
- Committed: `.obsidian/` settings, plus the khora, Dataview and Smart Connections plugins. Dataview has JavaScript queries on, for live cards.
- Ignored: `.obsidian/workspace*.json` and `.smart-env/`.
- `src/` is in Obsidian's excluded files.

**3. Entities keep the vault format.** Frontmatter keys follow the kit's rules. Kinds have versioned names, such as `Decision v1.1.0`. The validator runs on every entity the kit writes.

### Ontology

**4. The kit ships the loops kit and Unity's kinds.**
- From the loops kit: `BaseEntity v1.0.0` (with `author`), `Process(n) v1.0.0`, `Process Instance v1.0.0`, `Execute Process v1.0.0`, `Deposit process v1.0.0`, `Letter v1.0.0`, `Loop v2.0.0` and its five phases, `Cadence Loop v1.0.0`, `Weekly Loop v1.0.0`.
- From Unity: `Call v1.0.0`, `Task v1.0.0`, `Person v1.0.0`, `Call Ingestion v1.0.0`, `Render Canvas v1.0.0`.
- New or extended for khora: `Area v1.0.0`, `Question v1.0.0`, `Decision v1.1.0`, `Update View v1.0.0`, `Team Weekly v1.0.0`.
- `Organization v1.0.0` is not shipped. Team members are Persons.

**5. `Decision v1.1.0` extends Unity's `Decision v1.0.0`.**
- Adds `quote` (text): the transcript line where it was decided.
- Adds `answers` (Question, optional).
- Adds the kind value `design`: a choice about khora's model or architecture, the kind ADRs are made of.
- Everything else is Unity's: `originates_in`, `decided_by`, `supersedes`, `about`, `decided_on`, `status` (active, superseded, reversed) and the other kind values.
- Unity can adopt v1.1.0 when it wants to. Nothing in it breaks v1.0.0.

**6. New kinds carry only what the flow needs.**

| Kind | Relationships | Properties |
| --- | --- | --- |
| Area | — | — |
| Question | `originates_in` (Call or loop run, optional) | `lean` (text) |

- Every entity has `in_context_of` its Area. Letters also have `in_context_of` their loop instance.
- `author` is set on every entity.
- "Answered" is derived from inbound `answers` links, not stored.

**7. The graph has a spine of Areas.** One Area per design area: log, surfaces, surface logic, scopes, runtime, behaviors, security, product, design.

### Flow

**8. A recording becomes a transcript file before anything is drawn from it.**
- A Wispr Flow export goes into `transcripts/`, named `YYYY-MM-DD <topic>.md`, unedited.
- A transcript is linked from its Call, never copied into it.
- Wispr Flow does not label speakers reliably. Attribution follows `Call Ingestion`'s rule: unattributed, or marked `[?]`.

**9. Proposals arrive as one pull request per ingestion run.**
- Claude Code runs `Call Ingestion v1.0.0` on the new transcripts, through `Execute Process v1.0.0`, on a branch.
- It writes the Calls, Decisions, Questions and Tasks, and opens one pull request.
- Each Decision quotes its transcript line in `quote`.
- Decisions are written with `status: active`, because only a merge brings them to `main`.

**10. Merging is acceptance.**
- A team member reviews the pull request. Reviewing means checking each Decision against its quote.
- To reject a Decision, the reviewer sets `status: reversed` in the pull request and adds a line of why. Rejected Decisions are kept.
- GitHub records who accepted, and when.

**11. Proposals expire.** A pull request not merged by the next weekly is closed, and a Letter on the Call records it. Nothing is accepted by default.

**12. Only team members change accepted Decisions.**
- A change of mind is a new Decision that `supersedes` the old one. The old one's `status` becomes superseded or reversed, in the same pull request.
- A typo may be fixed in place, by a team member.
- Agents change an accepted Decision only to supersede it, and only in a pull request.

**13. Documents are written only from accepted Decisions.**
- The process `Update View v1.0.0` edits a document in `docs/` from active Decisions.
- Every sentence it adds that decides something carries a trace marker naming its Decision. The marker is invisible on GitHub. *Suggestion:* an HTML comment holding the Decision's id.
- Documents already in `docs/` are not traced retroactively. The rule applies to changes from now on.

### Canvases

**14. Canvases the team draws stay theirs.** `canvas/` holds product and design canvases drawn by hand. Agents may read them and link to them, and never change them.

**15. Dashboards are rendered from the graph, as in Unity.**
- `Render Canvas v1.0.0` and the layout engine `khora-canvas` are reused. The vault name is no longer hard-coded in the engine.
- Each dashboard carries its own Spec card, written and owned by the team. The renderer never changes it.
- The agent decides what a dashboard shows. The engine only computes geometry. Agents never write coordinates.
- Lists that change between renders are live cards (Dataview views in `dashboards/views/`). Judgement goes in Reading cards.
- Unity's review rules hold: no long edges, structure by labelled groups and placement, one colour grammar, a founder's look before a layout is trusted.

**16. The kit starts with three dashboards.**
- **Areas:** one group per Area: active Decisions, open Questions, open Tasks, and the documents that are its views.
- **Loops:** Unity's Loops dashboard, for the team's open loops and the weekly.
- **Stale:** decision 17's report.

### Staleness

**17. Staleness is computed, never judged.** A script, `khora-stale`, reads the graph, the trace markers and git history, and reports:

| What | Stale when |
| --- | --- |
| A sentence in a document | its trace marker names a Decision that is superseded or reversed |
| A document | its Area has active Decisions newer than the document's last commit, and none of them is traced in it |
| A transcript | no Call links it |
| A dashboard | its render date is older than the newest change to an entity of a kind its Spec lists |
| A Task | it is open and originates in a Decision that is superseded or reversed, or it is past due |
| A Question | an active Decision answers it, but a document still lists it as open |
| A proposal | its pull request is open past the weekly |

- The report is a live card on the Stale dashboard and the first thing the weekly's Observe reads.
- `khora-stale` reports. It never fixes.

### Loops

**18. Work runs in loops.**
- A bet, push or experiment is a Loop instance.
- The team's rhythm is a `Team Weekly v1.0.0` instance: a kind of `Weekly Loop v1.0.0` for this team.
- Its Observe reads the stale report, open proposals, the acceptance rate, and three accepted Decisions sampled for a second look.
- A superpowers run is a Loop instance. Brainstorming is the Laydown. The plan and its execution are Operate. Verification and review are Observe. The retrospective is Process(v). Superpowers writes its design documents to `docs/`, and their decisions go through decisions 9 to 13.

**19. The kit's own decisions are its first Call.** The conversation that produced this document is recorded as a transcript and ingested. Its Decisions go through the first pull request, and this document becomes their view.

### CLAUDE.md

**20. `CLAUDE.md` is rewritten for the kit.**
- What the repository is, and its layout (decision 1).
- How work flows: transcript, Call, proposals, pull request, documents (decisions 8 to 13).
- The design writing conventions, kept from the current `CLAUDE.md`.
- The entity rules in short, with details in `.claude/rules/`.
- Setup for macOS and Windows, in `.claude/rules/`.
- How superpowers is used (decision 18).

### Slices

**21. The kit lands in two slices.**
- **Slice 1:** decisions 1 to 20, run by convention and by hand, including `khora-stale` and the three dashboards.
- **Slice 2:**
  - a check that every new deciding sentence in a document is traced;
  - a check that rejects agent commits that edit an accepted Decision outside a supersede, using the `Co-Authored-By` trailer;
  - a one-way mirror from Tasks to GitHub issues, and an import of the open issues as Tasks.

## Costs and risks

- Obsidian plugins add a few megabytes to the repository.
- Documents lag behind decisions until someone runs `Update View`. The stale report makes the lag visible, not shorter.
- Reviewing Decisions depends on quotes being exact. Wispr Flow transcripts without speakers make attribution weak.
- Acceptance fatigue is measured, not prevented. The weekly sample is the only check in slice 1.
- Until slice 2, decisions 12 and 13 hold only by convention.
- The layout engine is two weeks old. Dashboards need a person's look until its layouts are trusted.
- The kit and Unity can drift if one changes a shared kind without the other.
- The kit's kinds and khora's future kinds may diverge. Migration is out of scope.

## Open questions

| Question | Lean |
| --- | --- |
| The Obsidian vault name | The repository folder name, `khora` |
| Who runs `Call Ingestion` after a recording | Whoever recorded it |
| Whether the designer works in Obsidian or only in canvases and pull requests | Ask the designer |
| Where Tasks start before the mirror exists | GitHub issues, until slice 2 |
| Whether shared kinds live in one place both vaults copy from | Copy by hand for now; revisit if they drift |
