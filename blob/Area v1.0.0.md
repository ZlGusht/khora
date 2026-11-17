---
identity__uuid: 29758447-0207-4de0-a0e8-ba176c526d8b
identity__type: universal
identity__created_at: 2026-11-17
identity__tags: []
aliases:
  - Area
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
A hub for one area of khora's design or of the work around it. Every entity in this vault is in_context_of at least one Area, so the graph has a spine: any Decision, Question, Call or loop run is one hop from its Area.

An Area has no fields of its own. Documents in docs/ name the Areas they belong to in an `areas` list in their own frontmatter, and the Areas dashboard and the stale report read that list.
"""
```
