---
identity__uuid: 21be45ee-ae5a-491b-815e-2ef1ae7852b5
identity__type: universal
identity__created_at: 2026-06-24
identity__tags: []
aliases:
  - Weekly Loop
relationships__is_a:
  - "[[Cadence Loop v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
The **Weekly Loop** is the structural bridge between daily action and quarterly objectives, and the first cadence that is run as a conversation: an agent executes it via [[Execute Process v1.0.0]], assembling the week's seven dailies and interviewing the user. A weekly run is named by its week anchor (e.g. Y26Q2W26).

```toml
Description = """
The weekly integration & iteration cycle. Agent-run via Execute Process. It (1) compresses the past week's entropy into actionable memory, (2) calibrates the coming week against immediate reality (schedule + energy) and the quarterly objectives, and (3) iterates on the specific recurring weekly loops in focus. Closes at end of week. A weekly run is is_a [[Process Instance v1.0.0]] and [[Weekly Loop v1.0.0]], in_loop its quarter run, after_loop the previous week. Its plan and structured observations live in the run's body.
"""

Subprocesses = """
Each phase has a role and a goal. The phases are conversational — interview, do not request bullet lists.

### Lay down — the Strategic Confidant
Goal: a realistic week plan that fits the user's actual life (prevent tunnel vision — chasing goals while ignoring constraints).
1. Ingest the previous week's NextLoopDirective (after_loop) and the current quarterly objectives (in_loop).
2. Interview the terrain — do not assume the schedule: the hard landscape (calendar) and soft landscape (energy/mood); obstacles or social events that will drain or fuel.
3. Synthesize the quarterly directives with the reality check.
4. Define: a **Theme** (a short phrase for the week's vibe), the **weekly loops** to focus on, and critical **one-off actions** outside any loop.

### Operate — the Silent Partner
Facilitate execution through the daily loops. The week's dailies are the execution log; the agent does not intrude during the week.

### Observe — the Biographer / Therapist
Goal: extract high-signal reflection without administrative fatigue.
1. Review the ExecutionLog (the seven dailies in_loop this week) and the weekly fields below.
2. Interview adaptively: start broad ("how did the week feel vs. how we planned it?"), then drill down on Events ("what is worth pointing out?"), Thoughts/Insights ("what have you been thinking about / noticed about the world?"), Feelings ("how have you been feeling?"), Overloads/Sufferings ("when did you feel overwhelmed?"). Quick metric check (qualitative): did we hit the financial baseline? Process check: how did each focused weekly loop evolve — stick to the script or change it? Run the polarity (Representation↔Contact, Sealed↔Exposed) review.
3. Compile StructuredObservations into the run body: Events, Thoughts_Insights, Feelings, Overloads_Sufferings, Polarity_Review — each enriched with the conversation's nuance, not flattened to bullets.

### Process — the Navigator
Goal: close the loop and prime the next one.
1. Compare the observations against the quarterly trajectory.
2. Generate ONE key insight — the pattern requiring adjustment.
3. Generate the **NextLoopDirective**: not a to-do list but a strategic hand-off note to next week's Strategic Confidant (e.g. "burning out — next week prioritize recovery over intensity", or "momentum high — push for the breakthrough in domain X").
4. Write ProcessInsights, ProcessOutcome, and NextLoopDirective into the run body; the directive is read by next week via after_loop.
"""

ContextAssembly = """
The weekly recipe — this is where the daily→weekly rollup that previously was not happening becomes structural.

1. ExecutionLog: query the seven daily runs in_loop this week run (is_a [[Daily Loop v1.0.0]], identity__type = particular, in_loop = this week). Read their habit metrics and Observe fields — they ARE the week's execution log.
2. Prior directive: the previous week run (after_loop) — specifically its NextLoopDirective, which seeds this week's Laydown.
3. Quarterly frame: the quarter run this week is in_loop — its ExecutionPlan / objectives, so the week is calibrated to the quarter.
4. Optionally surface the focused weekly-loop process entities being iterated this week.
Trim to the dailies + the two adjacent-cadence anchors.
"""
```

<!-- khora:auto-related start -->
## Related
- [[Daily Loop v1.0.0]]
- [[Process Instance v1.0.0]]
- [[Weekly Loop v1.0.0]]
<!-- khora:auto-related end -->
