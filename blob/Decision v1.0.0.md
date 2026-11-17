---
identity__uuid: 790bf91b-e7de-439f-af6e-30f8268b69d0
identity__type: universal
identity__created_at: 2026-11-16
identity__tags: []
aliases:
  - Decision
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
Something the team committed to: a choice, a rule, a direction, or a declaration of state.

A particular is_a [[Decision v1.0.0]], named by the decision itself as a short statement (e.g. `Investor conversations start before the MVP`). Decisions are never edited to reflect a change of mind. A new Decision is written that `supersedes` the old one, and the old one's status becomes superseded or reversed. The history of the venture's thinking is therefore the supersedes chain.

The kind property is what lets the venture's declared state be read off the graph: the current thesis, effectuation stage, direction and attention are simply the newest active Decisions of those kinds. They are set deliberately, usually in a Weekly Loop Laydown or a meta review.
"""

[Metadata.Relationships]
originates_in = { type="connection", description="Where it was decided: a Call or a loop run.", optional=true }
decided_by = { type="list<connection>", description="Persons who made it.", optional=true }
supersedes = { type="list<connection>", description="Earlier Decisions this one replaces.", optional=true }
about = { type="list<connection>", description="Entities the decision concerns (Persons, Organizations, loops).", optional=true }
answers = { type="list<connection>", description="Questions this Decision answers.", optional=true }

[Metadata.Properties]
decided_on = { type="date", description="When it was decided." }
status = { type="text", description="Whether it still holds.", values=["active", "superseded", "reversed"] }
quote = { type="text", description="The transcript line where it was decided, verbatim, limited to the decision's own words. Required when the Decision comes from a Call; a Decision made in a loop run has none. The repository is public: nothing personal; paraphrase with [paraphrased] when needed.", optional=true }
kind = { type="text", description="thesis = what we believe about the world or market; direction = where the venture is heading; stage = effectuation stage; attention = what we focus on now; rule = a standing way of working; operational = a one-off choice; design = a choice about khora's model or architecture, the kind ADRs are made of.", values=["thesis", "direction", "stage", "attention", "rule", "operational", "design"] }

[Data]
Statement = { type="text", description="The decision in one or two sentences." }
Rationale = { type="text", description="Why, including the alternatives and any disagreement.", optional=true }
```
