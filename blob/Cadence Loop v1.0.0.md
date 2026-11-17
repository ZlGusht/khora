---
identity__uuid: bbb6e100-0f5a-4d9e-a0bc-46ecd6f8561c
identity__type: universal
identity__created_at: 2026-06-24
identity__tags: []
aliases:
  - Cadence Loop
relationships__is_a:
  - "[[Loop v2.0.0]]"
relationships__in_context_of:
  - "[[Looping v2]]"
  - "[[Khora]]"
---
A **Cadence Loop** is a [[Loop v2.0.0]] whose lifecycle is bounded by a fixed calendar period rather than by Reservoir capacity or a convergence Oracle. It opens at the start of its period and closes at the end of it — full stop. The four concrete cadences are [[Daily Loop v1.0.0]] ⊂ [[Weekly Loop v1.0.0]] ⊂ [[Quarterly Loop v1.0.0]] ⊂ [[Yearly Loop v1.0.0]], each nesting in the next.

This universal exists to hold what the four cadences share, so it is not restated four times: the temporal-close override and the `in_loop` / `after_loop` chaining vocabulary their runs use.

```toml
Description = """
The temporally-bounded reflection loop. Inherits Loop v2's Inception → Laydown → Operate → Observe → Process(v) skeleton, but OVERRIDES Loop v2's closing semantics: a Cadence Loop does not close on Reservoir state, capacity, or an Oracle — it closes when its calendar period ends (the day / week / quarter / year is over). The Reservoir, Dual Vector, and capacity machinery remain available as lightweight framing but are not load-bearing for closing.

A *run* of a cadence is the period itself, modelled per [[Process Instance v1.0.0]]'s dual-parent convention: the run is is_a BOTH [[Process Instance v1.0.0]] AND the specific cadence Loop (e.g. is_a [[Daily Loop v1.0.0]]). The run is named by its human-readable period anchor (e.g. 2026-06-24, Y26Q2W26, Y26Q2, Y26), carries status / started_at / ended_at from Process Instance, and holds its reflection content directly in its own body — there is no separate summary Letter and no separate anchor entity. One note per period.

Nesting and chaining (both inherited by every cadence's runs):
- in_loop  — the super-period run this run sits inside (a daily's week, a week's quarter, a quarter's year). A parent run reaches its children as the inbound in_loop backlinks.
- after_loop — the previous run of the SAME cadence (yesterday's daily, last week's weekly). This forms the loop chain along which a run's Process(v) NextLoopDirective reaches the next run's Laydown.

The daily→weekly→quarterly→yearly rollup is realized through each cadence's own ContextAssembly recipe (see the concrete cadences): a higher cadence pulls the runs in_loop it and metabolizes them.
"""

[Metadata.Relationships]
in_loop = { type = "connection", description = "The super-period run this run nests inside (day→week→quarter→year). The parent discovers its children via inbound in_loop backlinks.", optional = true }
after_loop = { type = "connection", description = "The previous run of the same cadence — forms the loop chain that carries NextLoopDirective forward.", optional = true }
```

<!-- khora:auto-related start -->
## Related
- [[Daily Loop v1.0.0]]
- [[Process Instance v1.0.0]]
<!-- khora:auto-related end -->
