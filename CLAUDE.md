# Working on khora

## Where things live

This repository is the single source of truth for khora's design. Read and edit the documents here; do not keep copies elsewhere.

- `docs/PRD.md`: product requirements (draft). Sections marked `#TODO` are still template placeholders.
- `docs/adr/`: architecture decision records, one file per decision area.
- `docs/systems/`: sample domain system specs, plus `template.md`.
- `docs/architecture.canvas`: an Obsidian canvas of the projected runtime architecture (components and flows), derived from the ADRs. It is not a source of truth: when an ADR adds or changes a component or flow, update the canvas.
- The GitHub wiki and the claude.ai Project only point here. Do not add documents to either.

There is no implementation code yet.

## Conventions

**System specs** follow `docs/systems/template.md`: Overview, Context and boundaries, Requirements (ID, type, requirement, priority, verification), Architecture, Constraints/assumptions/risks, Verification, Open questions. A spec may start with a status note.

**ADRs** use this shape:
- Header block: Status, Date (DD.MM.YYYY), Issue, Related, Number.
- Sections: Context, Terms (table), Decisions (numbered, bold lead sentence), Caveats, Consequences (positive, negative), Alternatives considered, Open questions (table with a "Lean" column), Out of scope (TODO), Done when.
- ADRs are referred to by placeholder ids (ADR-0XX, ADR-0XY, ADR-0XZ, ADR-0YA, ADR-0YB) until numbers are assigned. Cite decisions as "ADR-0XY decision 10".
- When an ADR amends another, it says so in the decision and lists the amendment under "Done when".

**Markers:**
- `(interop: review)`: depends on interoperability across deployments; revisit when that is designed.
- `*Suggestion*`: proposed, not decided.
- `#TODO`: not yet written.

**Style:** plain, short sentences. One idea per bullet. Lists over long paragraphs. Define terms in a Terms table rather than inline.

## Known follow-ups

- ADR-0XZ makes the company brain spec's placement rule, ownership language, participant khoras and scope-to-khora promotion obsolete. `docs/systems/company-brain.md` still needs the rewrite: seats as scopes, rights instead of ownership and placement.
- ADR-0XZ and ADR-0YA amend ADR-0XX and ADR-0XY (frontiers, pure updates, `invoke`, the shape of actions). The amended ADRs do not yet reflect this in their own text.
- ADR-0YB (action log, processes and reactivity) is provisional. It amends ADR-0XY decision 7 and ADR-0XX decision 6; neither reflects this yet.
