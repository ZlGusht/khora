---
identity__uuid: ecf4f979-366e-4c38-b53d-34997264c816
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #25 Query as a data structure]]"
properties__type: change-later
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #25 Query as a data structure]], from the review of the `query` module.

## Content

A stored query is its canonical JSON in a string literal of `khora/query`. The JSON carries no version, so a later change to its shape cannot tell old queries from new ones. The envelope's time is a frontier only: ADR-0XX decision 2's `by: recorded | occurred` and `meaning: then | now`, and its annotations, are left out.

When: the first change to the query's JSON adds a version member. Time by recorded or occurred comes with K7; annotations with the first kind that carries confidence.