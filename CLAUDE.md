---
areas:
  - "[[Kit]]"
---

# Working on khora

## What this repository is

khora's design, and later its code, run as the khora kit: a provisional khora built from Obsidian, Claude Code and superpowers (`docs/khora-kit.md`). The repository is the single source of truth. Do not keep copies elsewhere; the GitHub wiki and the claude.ai Project only point here.

## Layout

- `blob/`: entities: kinds, processes, Areas, Calls, Decisions, Questions, Persons, loop runs.
- `docs/`: documents. `PRD.md`, `adr/`, `implementation-plan.md`, `systems/`, and the kit's own `khora-kit.md` and `khora-kit-plan.md`.
- `docs/visuals/`: the README images, exported from the "khora visuals" design canvas. Update the canvas and export again; do not edit the images.
- `src/`: code. There is none yet.
- `canvas/`: canvases the team draws.
- `dashboards/`: canvases the kit renders, and their live views in `dashboards/views/`.

## How work flows

1. A recording is exported from Wispr Flow to the transcripts folder outside this repository (`KHORA_TRANSCRIPTS`, by default `../khora-transcripts/`), as `YYYY-MM-DD <topic>.md`, unedited (kit d8). Exported conversations go there too.
2. `[[Call Ingestion v1.0.0]]` turns it into a Call, Decisions and Questions on a branch, and opens one pull request labelled `ingestion` (kit d9).
3. A team member checks each Decision against its quote. Merging accepts; `status: reversed` in review rejects (kit d10).
4. Whoever merges creates the proposed GitHub issues. Work lives in issues, not in entities.
5. An ingestion pull request not merged by the next Team Weekly is closed (kit d11).
6. `[[Update View v1.0.0]]` writes documents from active Decisions, with trace markers (kit d13).
7. `bash .claude/scripts/khora-stale` reports what no longer matches; `dashboards/Stale.canvas` shows it (kit d17).

## Rules for agents

- Never write a sentence in `docs/` that no active Decision states. Mark each deciding sentence with `<!-- d:<decision uuid> -->`.
- Never edit an accepted Decision, except to supersede it, in a pull request. Only team members fix them.
- Never change anything in `canvas/`.
- Never create Task entities. Work items are GitHub issues.
- Transcripts and exported conversations are only read and parsed. Never copy them into the repository: it is public.
- A Decision's `quote` holds only the decision's own words. Nothing personal; paraphrase with `[paraphrased]` when needed.
- Set `properties__author` on every entity you write: a team member's name, or `agent:<model>`.
- Run `bash .claude/scripts/khora-validate "<name>"` after every entity you write.

## Pull requests from outside the team

The vault runs code when Obsidian opens it: the plugin builds in `.obsidian/plugins/` and the Dataview views in `dashboards/views/`. A branch can change both.

- Review a pull request from outside the team in a separate clone that is not open in Obsidian.
- Check it out in the vault only after its changes to `.obsidian/`, `dashboards/views/` and `.claude/` are reviewed. `.github/CODEOWNERS` requires an owner's review for those paths.

## Superpowers

- A superpowers run is a Loop instance (kit d18). Brainstorming is the Laydown; the plan and its execution are Operate; verification and review are Observe; the retrospective is Process(v).
- Specs and plans go in `docs/`, not `docs/superpowers/`.
- Decisions made in a superpowers session reach `docs/` the same way as any other: as Decisions, through a pull request.

## Entity rules

- Vault I/O goes through the Obsidian CLI, `obsidian vault=<vault> …`. Never `eval`; never edit `blob/` files directly.
- Wikilinks are exact and versioned: `[[Decision v1.0.0]]`, not `[[Decision]]`.
- Only keys declared along an entity's `is_a` chain are allowed. A new key is an ontology change; ask first.
- Before ontology decisions or new entities, run `khora-assemble-context`.
- Details: `.claude/rules/khora-vault.md`, `khora-crud.md`, `khora-setup.md`.

## Conventions

**System specs** follow `docs/systems/template.md`: Overview, Context and boundaries, Requirements (ID, type, requirement, priority, verification), Architecture, Constraints/assumptions/risks, Verification, Open questions. A spec may start with a status note.

**ADRs** use this shape:
- Header block: Status, Date (DD.MM.YYYY), Issue, Related, Number.
- Sections: Context, Terms (table), Decisions (numbered, bold lead sentence), Costs and risks, Alternatives considered, Open questions (table with a "Lean" column), MVP, Done when.
- Every sentence must decide something, define a term the decisions use, or name a cost or risk that could change a decision. No positive consequences (they restate decisions), no lists of where something is "already implicit", no cross-references that are not needed to read the decision.
- Alternatives: only ones a reader would actually propose, one line each.
- MVP: which decisions are in the MVP and which come later.
- Done when: only where an issue or tests exist.
- ADRs are referred to by placeholder ids until numbers are assigned: ADR-0YA (the model; read first), ADR-0YB, ADR-0XY, ADR-0XX, ADR-0XZ, ADR-0YC, ADR-0YD, ADR-0YE. Cite decisions as "ADR-0XY decision 10".
- When an ADR changes another, update both in the same change.

**Markers:**
- `(interop: review)`: depends on interoperability across deployments; revisit when that is designed.
- `*Suggestion*`: proposed, not decided.
- `#TODO`: not yet written.

**Style:** plain, short sentences. One idea per bullet. Lists over long paragraphs. Define terms in a Terms table rather than inline.

## Known follow-ups

- The system specs predate the current model and use older terms (proxies, governed actions, action layer, action log, controllers, projections). Update a spec's wording when it is next touched.
- `docs/systems/company-brain.md` still relies on seats, agreements, ownership, placement and participant khoras, none of which ADR-0XZ keeps. It needs a rewrite.
- ADR-0YE (security model) is a stub.
