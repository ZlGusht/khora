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

Surfaces and authorities have contract tests: scenarios written as data (given steps, a when, a then, with step names instead of ids), played through the real kernel, and each checked against general rules (three for authorities, four for surfaces). `Projects` passes twelve scenarios and the reference surface `Echo` six, on catch-up and live delivery; issue #22's done-when is met. Not done: security tests, several surfaces per scenario, checking what a view shows.

## Findings

- The review of #36 became tests first: they flipped to "fixed" when #36 fixed its findings in 6845d75, and #36 now tests those itself.
- A rule must be shown to fail: each kept rule has a deliberately bad implementation it catches, and a rule with nothing to judge in a scenario aborts instead of passing.
- Expected emissions must say which event they answer, or a surface answering the wrong event passes.
- Reading only at the head of the log hides a kernel that ignores the frontier; a read at an earlier frontier is needed.

## Frictions

- The format first attached the surface only as the "when", so live delivery went untested.
- #36 changed while this was built; ten tests had to follow its stricter rules (a write must know the thing it writes about).
- The first version did more than the issue asked; two reviews trimmed it by about 220 lines.

## Ideas

- Write the contracts' scenarios as JSON, or as statements on a system's scope, once a system needs to ship its required behaviour as data (the plan's "later, as data").
- Let a scenario hold several surfaces, for systems whose surfaces react to each other.
- A contract for views (B1, B2): given events, the statements a view shows.

## Considerations

- The contracts sit on #36 and test its kernel: merge #36 first, then this (squash-merging fits: some commits undo earlier ones).
- K9's general authority, B1 and B2 should each ship a contract test with their scenarios.
- The test format is the plan's *Suggestion* made concrete ([[Contract tests are scenarios checked against general rules]], [[Contract scenarios are records with symbolic references]]); the plan's Testing section and stage 0's open test-format decision can point here.
- Proposed issue: security rules for the contracts, with P5 ([[Security tests wait for access checks]]).