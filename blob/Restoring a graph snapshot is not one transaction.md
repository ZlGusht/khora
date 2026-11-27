---
identity__uuid: 2635d105-6c6c-4f8d-a11c-a4ff4bbb22a5
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #23 Graph datastore on Neo4j]]"
properties__type: change-later
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #23 Graph datastore on Neo4j]], from the review of the `datastore-neo4j` module.

## Content

`restore` clears the database in batches, then writes the whole snapshot in one transaction. A snapshot is held whole in memory and in one file, without a checksum. A clear cut short leaves a datastore that refuses to take events in; it is cleared and rebuilt, from a snapshot or from the start of the log.

When: snapshots outgrow memory or one transaction, or a datastore is rebuilt while it answers. Restoring into a second database and switching to it would make it atomic.