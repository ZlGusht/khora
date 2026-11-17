# The khora kit

> **Status:** Draft
> **Date:** 17.11.2026
> **Related:** `docs/systems/application-development.md` (bootstrap step 1: an overlay on khora's repositories); ADR-0YA; ADR-0YD
> **Note:** a bootstrap document. Once the kit works, its decisions become Decision entities and this document becomes their view (decision 14).

## Context

The team talks decisions through, then has an AI write the ADRs, plans and docs. The write-ups add sentences nobody decided, and the decisions get lost under them. Context is spread across conversations, documents, canvases and GitHub issues.

The kit is a provisional khora that runs in this repository with Obsidian, Claude Code and superpowers. It is a prosthetic until khora runs: workable, not the absolute system. It keeps every decision as an entity, generates documents only from accepted decisions, and runs the team's work in loops.

## Terms

| Term | Meaning |
| --- | --- |
| **Vault** | The repository opened in Obsidian. Its root is the repository root. |
| **Entity** | A Markdown file in `blob/` with frontmatter and a body. |
| **Kind** | What an entity is, declared by `relationships__is_a`. Every kind reaches `BaseEntity v1.0.0`. |
| **Area** | A hub entity for one design area. Every other entity links to at least one. |
| **Session** | One recorded discussion. Its body holds the transcript. |
| **Decision** | One call made in a Session, quoting the line where it was made. |
| **Question** | An open question with its current lean. |
| **Task** | A piece of work that implements a Decision. |
| **Note** | A friction, insight or signal deposited into a loop. |
| **Loop** | A process run in phases: Inception, Laydown, Operate, Observe, Process(v). |
| **Instance** | One run of a process: an entity that is a kind of both `Process Instance v1.0.0` and the process. |
| **Proposal** | A Decision, Question or Task written by an agent and not yet merged. |
| **Acceptance** | A team member merging the pull request that carries a proposal. |
| **View** | A document in `docs/` generated from accepted Decisions. |

## Goals, non-goals and anti-goals

**Goals:**
- Every decision the team makes is recorded once, with its source.
- Documents say only what accepted Decisions say.
- All context is in one repository: decisions, documents, canvases and code.
- The team works in loops, with a weekly rhythm.
- Automated development with superpowers runs inside loops.

**Non-goals:**
- Agents as entities. They come later.
- Replacing khora. The kit uses files, git and Obsidian, not khora's log.
- Services of our own. Everything runs on existing tools.

**Anti-goals** (the kit fails if these happen, even if it works):
- A view gains sentences no Decision backs.
- Acceptance becomes a rubber stamp.
- The kit grows into the product.

## Approaches

| Approach | Trade-off | Chosen |
| --- | --- | --- |
| **The repository root is the vault** | Obsidian config and plugins enter the repository | Yes: Decisions link to the views they produce, and everything is in one place |
| The vault in a subfolder | `docs/` falls outside the vault, so Decisions can't link to views | No |
| The vault in its own repository | Decisions, documents and code split again | No |

## Decisions

### Layout

**1. The repository root is the vault.**
- `blob/`: entities.
- `docs/`: views and the existing design documents.
- `src/`: code.
- `canvas/`: product and design canvases. *Suggestion*

**2. Obsidian's shared config is committed; per-machine state is not.**
- Committed: `.obsidian/` settings, plus the khora, Dataview and Smart Connections plugins.
- Ignored: `.obsidian/workspace*.json` and `.smart-env/`.
- `src/` is in Obsidian's excluded files.

**3. Entities keep the vault format.** Frontmatter keys follow the kit's rules. Kinds have versioned names, such as `Decision v1.0.0`. The kit's validator runs on every entity it writes.

### Ontology

**4. The kit ships the loops kit and six new kinds.**
- From the loops kit: `BaseEntity v1.0.0` (with `author`), `Process(n) v1.0.0`, `Process Instance v1.0.0`, `Execute Process v1.0.0`, `Loop v2.0.0` and its five phases, `Cadence Loop v1.0.0`, `Weekly Loop v1.0.0`.
- New: `Area`, `Session`, `Decision`, `Question`, `Task` and `Note`, each `v1.0.0` and a kind of `BaseEntity v1.0.0`.
- New processes: `Extract Decisions v1.0.0` (decision 8), `Update View v1.0.0` (decision 12) and `Team Weekly v1.0.0` (decision 13).
- `Letter v1.0.0` is not shipped. `Note v1.0.0` takes its narrow role. `Execute Process` and `Deposit process` get new versions that write Notes, Decisions, Questions and Tasks instead of Letters.

**5. Kinds carry only what the flow needs.**

| Kind | Relationships | Properties |
| --- | --- | --- |
| Area | — | — |
| Session | — | `held_at` (date) |
| Decision | `from_session` (Session), `answers` (Question, optional), `supersedes` (Decision, optional) | `status`: accepted, rejected; `quote` (text) |
| Question | — | `lean` (text) |
| Task | `implements` (Decision, optional) | `status`: open, done; `issue` (URL, optional) |
| Note | — | `type`: friction, insight, signal |

- Every entity has `in_context_of` its Area. Notes also have `in_context_of` their loop instance.
- `author` is set on every entity.
- "Superseded" and "answered" are derived from inbound `supersedes` and `answers` links, not stored. Superseding a Decision then never edits it.

**6. The graph has a spine of Areas.**
- One Area per design area: log, surfaces, surface logic, scopes, runtime, behaviors, security, product, design.
- Each Area has a canvas map in `canvas/`. Obsidian's graph is colored by kind.

### Flow

**7. A Session is recorded before anything is drawn from it.** The transcript tool's text export goes into a new Session's body, unedited.

**8. Proposals arrive as one pull request per Session.**
- Claude Code runs the process `Extract Decisions v1.0.0` on the Session, through `Execute Process v1.0.0`.
- It writes proposed Decisions, Questions and Tasks on a branch and opens one pull request.
- Each proposed Decision quotes its Session line in `quote`.
- Proposed Decisions are written with `status: accepted`, because only a merge brings them to `main`.

**9. Merging is acceptance.**
- A team member reviews the pull request. Reviewing means checking each claim against its quote.
- To reject a proposal, the reviewer sets `status: rejected` in the pull request. Rejected Decisions are kept.
- GitHub records who accepted, and when.

**10. Proposals expire.** A pull request not merged by the next weekly is closed, and a Note on the Session records it. Nothing is accepted by default.

**11. Only team members change accepted Decisions.**
- A wrong Decision is changed by a new Decision that `supersedes` it.
- A typo may be fixed in place, by a team member.
- Agents never edit an accepted Decision.

**12. Views are written only from accepted Decisions.**
- The process `Update View v1.0.0` edits a document in `docs/` from accepted Decisions.
- Every sentence it adds that decides something carries a trace marker naming its Decision. The marker is invisible on GitHub. *Suggestion:* an HTML comment holding the Decision's id.
- Documents already in `docs/` are not traced retroactively. The rule applies to changes from now on.

### Loops

**13. Work runs in loops.**
- A bet, push or experiment is a Loop instance.
- The team's rhythm is a `Team Weekly v1.0.0` instance: a kind of `Weekly Loop v1.0.0` for this team.
- Its Observe also covers open pull requests (decision 10), the acceptance rate, and three accepted Decisions sampled for a second look.

**14. A superpowers run is a Loop instance.**
- Brainstorming is the Laydown.
- The plan and its execution are Operate.
- Verification and review are Observe.
- The retrospective is Process(v).
- Superpowers writes its design documents to `docs/`, not to `docs/superpowers/`. Their decisions go through decisions 8 to 12.

**15. The kit's own decisions are its first Session.** The conversation that produced this document is recorded as a Session. Its Decisions go through the first pull request, and this document becomes their view.

### CLAUDE.md

**16. `CLAUDE.md` is rewritten for the kit.**
- What the repository is, and its layout (decision 1).
- How work flows: Session, proposals, pull request, views (decisions 7 to 12).
- The design writing conventions, kept from the current `CLAUDE.md`.
- The entity rules in short, with details in `.claude/rules/`.
- Setup for macOS and Windows, in `.claude/rules/`.
- How superpowers is used (decision 14).

### Slices

**17. The kit lands in two slices.**
- **Slice 1:** decisions 1 to 16, run by convention and by hand.
- **Slice 2:**
  - a check that every new deciding sentence in a view is traced;
  - a check that rejects agent commits that edit an accepted Decision, using the `Co-Authored-By` trailer;
  - a one-way mirror from Tasks to GitHub issues, and an import of the open issues as Tasks;
  - Area canvases refreshed from the graph.

## Costs and risks

- Obsidian plugins add a few megabytes to the repository.
- Generated views lag behind decisions until someone runs `Update View`.
- A Session transcript is long. Reviewing proposals against quotes depends on the quotes being exact.
- Acceptance fatigue is measured, not prevented. The weekly sample is the only check in slice 1.
- Until slice 2, decisions 11 and 12 hold only by convention.
- The kit's kinds and khora's future kinds may diverge. Migration is out of scope.

## Open questions

| Question | Lean |
| --- | --- |
| Which transcript tool, and its export format | Any tool with a plain-text export |
| The Obsidian vault name | The repository folder name, `khora` |
| Who runs `Extract Decisions` after a Session | Whoever recorded the Session |
| Whether the designer works in Obsidian or only in canvases and pull requests | Ask the designer |
| Where Tasks start before the mirror exists | GitHub issues, until slice 2 |
