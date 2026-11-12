# Working on khora

## Where things live

This repository is the single source of truth for khora's design. Read and edit the documents here; do not keep copies elsewhere.

- `docs/PRD.md`: product requirements (draft). Sections marked `#TODO` are still template placeholders.
- `docs/adr/`: architecture decision records, one file per decision area.
- `docs/systems/`: sample domain system specs, plus `template.md`.
- The GitHub wiki and the claude.ai Project only point here. Do not add documents to either.

There is no implementation code yet.

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

- The system specs predate the current model and use older terms (proxies, governed actions, action layer, controllers, projections). Update a spec's wording when it is next touched.
- `docs/systems/company-brain.md` still relies on seats, agreements, ownership, placement and participant khoras, none of which ADR-0XZ keeps. It needs a rewrite.
- ADR-0YE (security model) is a stub.
