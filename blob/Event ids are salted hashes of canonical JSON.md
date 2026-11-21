---
identity__uuid: 5b36677a-327f-4247-b54b-937acd8d5655
identity__type: particular
identity__created_at: 2026-11-21
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Events are the basic unit of khora]]"
properties__decided_on: 2026-11-21
properties__status: "active"
properties__kind: "design"
properties__quote: "salt is fine, go with JSON"
properties__author: agent:claude-opus-5-5
---
## Statement

An event's id is the SHA-256 hash of its canonical JSON (RFC 8785), written `sha256:<hex>`. The hashed content includes a random salt, the event's causes and its recorded time. The same content does not get the same id twice.

## Rationale

A plain content hash lets anyone who sees an id test a guess about its content, and keeps shredded content testable (ADR-0YB decision 10). With a salt, a guess cannot be tested; the salt is later kept and shredded with the content's key, leaving an id that reveals nothing. Tampering is still detected.

JSON is readable, which helps when comparing log stores in K2. Values are limited to strings, integers and ids to avoid the number pitfalls of canonical JSON. Deterministic CBOR and a hand-written binary encoding were the alternatives.

This changes ADR-0YB decision 5: the same content no longer always gets the same id.