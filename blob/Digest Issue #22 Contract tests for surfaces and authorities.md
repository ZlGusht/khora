---
identity__uuid: e89198f4-e928-4609-ad98-ab13f736c49a
identity__type: particular
identity__created_at: 2026-11-26
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #22 Contract tests for surfaces and authorities]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

Surfaces and authorities have contract tests: scenarios written as data (given steps, a when, a then, with step names instead of ids), played through the real kernel, and each checked against general rules (four for authorities, five for surfaces). `Projects` passes ten scenarios, the reference surface `Echo` six; issue #22's done-when is met. Not done: security tests, several surfaces per scenario, JSON scenarios.

## Findings

- Two of the #36 review's findings are now tests: a surface's own emissions are not causally ordered (on catch-up and on live delivery), and a surface whose `interests()` throws fails the submit. Both are skipped with their reason; fixing the kernel makes them fail as "known gap appears fixed".
- A contract has to show its rules can fail: each rule has a deliberately bad implementation it catches. Several rules would otherwise pass with nothing to judge, so they abort instead.
- Expected emissions must say which event they answer, or a surface answering the wrong event passes.
- Reading at the head of the log hides a kernel that ignores the frontier; a read at an earlier frontier is needed.

## Frictions

- The format first attached the surface only as the "when", so live delivery went untested; scenarios can now attach it among their given steps.
- Two subagent runs were cut by network errors and had to be resumed or rerun.

## Considerations

- The contracts sit on #36 and test its kernel: merge #36 first, then this; the two skipped gaps are for #36's fixes.
- K9's general authority, B1 and B2 should each ship a contract test with their scenarios.
- The test format is the plan's *Suggestion* made concrete ([[Contract tests are scenarios checked against general rules]], [[Contract scenarios are records with symbolic references]]); the plan's Testing section and stage 0's open test-format decision can point here.
- Proposed issue: security rules for the contracts, with P5 ([[Security tests wait for access checks]]).