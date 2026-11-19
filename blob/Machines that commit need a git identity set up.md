---
identity__uuid: 1b7b0591-06f9-46c9-b4a0-d81303609e33
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #21 Surface contract types]]"
relationships__relates_to:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
properties__priority: 2
properties__resolved: false
properties__type: task
---
## Context

Commits in the Issue #21 run failed: the Linux machine had no git user.name or user.email, globally or in the repository. The repository's identity was then set to the author's GitHub private address.

## Content

The setup check in `.claude/rules/khora-setup.md` does not check the git identity. It could check `git config user.email` and suggest the GitHub private address, so commits don't publish a personal email.