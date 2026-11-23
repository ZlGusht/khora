---
identity__uuid: 927e536e-7198-430b-b5b5-6702bb59ef8d
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Runtime]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: reversed
properties__kind: design
properties__quote: Accept all
properties__author: agent:claude-opus-5-5
---
## Statement

The kernel targets Java 25, the current long-term-support release, and builds with Maven through its wrapper script, so nobody installs Maven.

## Rationale

Agent's proposal, accepted. Maven is declarative and the most documented, which suits a team new to Java. Gradle was the alternative: faster and more flexible, but its build scripts can become programs.