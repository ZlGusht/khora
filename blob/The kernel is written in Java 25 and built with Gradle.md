---
identity__uuid: 6d67278d-3bc5-44c3-a687-3b2c91c73493
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #19 Log store on NATS]]"
relationships__decided_by:
  - "[[Hristiyan Ganchov]]"
properties__decided_on: 2026-11-18
properties__status: active
properties__kind: design
properties__author: agent:claude-opus-5-5
---
## Statement

The kernel is written in Java 25, the current long-term-support release, and built with Gradle using the Kotlin DSL and a Java 25 toolchain.

## Rationale

Java was agreed with Simeon Petkov in the call that split the stage 1 work [paraphrased]. Java 25 over 21: the project starts from nothing, and records, sealed interfaces and pattern matching fit the event types. Gradle over Maven: the kernel is several modules, where Gradle is faster and shorter to configure.