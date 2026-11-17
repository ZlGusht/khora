---
identity__uuid: 742e4591-3c42-45eb-9860-1cfa72c51fd0
identity__type: universal
identity__created_at: 2026-11-17
identity__tags: []
aliases:
  - Team Weekly
relationships__is_a:
  - "[[Weekly Loop v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
The **Team Weekly** is the khora team's [[Weekly Loop v1.0.0]]: the same four phases, with the kit's checks added to Observe and Process. A run is named by its week anchor (e.g. `Y26Q4W47`), is_a both [[Process Instance v1.0.0]] and [[Team Weekly v1.0.0]], and sets `after_loop` to the previous week's run.

```toml
Description = """
The team's weekly rhythm. Agent-run via Execute Process, as a conversation with the team. It plans the week, then looks back on it: what was decided, what went stale, and whether acceptance is still a real check.
"""

Subprocesses = """
The phases are conversational: interview, do not request bullet lists.

### Lay down
1. Read the previous run's NextLoopDirective (after_loop).
2. Ask about the week's capacity: who is available, for how much, and what is fixed in the calendar.
3. Agree a Theme, the loops in focus, and the issues the team means to close.

### Operate
The week's work happens in loops, calls and pull requests. The agent does not intrude.

### Observe
1. Read `dashboards/stale.md` (run `bash .claude/scripts/khora-stale` first) and go through it with the team.
2. List open pull requests labelled `ingestion`, with their age.
3. Compute the acceptance rate since the last run: Decisions merged active, divided by Decisions proposed (merged active plus reversed in review). A rate near 100% with reviews taking almost no time is the fatigue signal; say so.
4. Sample three active Decisions at random and ask the team whether each still holds.
5. Review the week: the Calls held, the pull requests merged, the issues closed. Then interview as in Weekly Loop: events, insights, overloads.

### Process(v)
1. Close every `ingestion` pull request opened before this run's started_at, and deposit a Letter (type signal) on each Call it carried, saying its proposals expired.
2. Supersede any sampled Decision the team no longer holds, through a pull request.
3. Write ONE key insight and the NextLoopDirective into this run's body.
"""

ContextAssembly = """
1. The previous run (after_loop) and its NextLoopDirective.
2. The stale report, open ingestion pull requests (`gh pr list --label ingestion`), issues closed this week (`gh issue list --state closed --search "closed:>=<monday>"`).
3. Calls and Decisions with dates in this week; open loop runs.
"""
```
