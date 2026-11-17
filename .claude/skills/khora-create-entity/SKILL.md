---
name: khora-create-entity
description: Create a new fully-populated entity in the Khora Obsidian vault — frontmatter shape, is_a, optional properties, additional relationships, and body — in one invocation. Use when the user wants to create any vault entity (a new type/universal, a process, a letter, a memory, an instance). Routes through the obsidian CLI; conventions live in .claude/rules/khora-vault.md and .claude/rules/khora-crud.md (both already loaded).
---

# khora/create-entity

One invocation produces one finished, schema-conformant entity.

## Invariants the result must satisfy

- `relationships__is_a` is a YAML list pointing at the requested target universal (versioned wikilink)
- `identity__uuid` and `identity__created_at` are real values
- Any requested `properties__*`, additional `relationships__*`, and body content are present
- Every `relationships__<key>` and `properties__<key>` used is schema-declared in some ancestor along the is_a chain

## Inputs

Required: `name`, `is_a` (wikilink to the target universal).

Optional: `folder` (default `blob/`), `properties`, `relationships`, `body`.

## Approach

Write the full file content yourself, per the "Creating an entity" pattern in `khora-crud.md` — no templates, no Templater. Generate `identity__uuid` with `bash .claude/scripts/khora-uuid` and `identity__created_at` with `date +%F`. Set `properties__author` (see "Authorship" in `khora-crud.md`). Put the content in a temp file and create it with `obsidian vault=<vault> create path="blob/<name>.md" content="$(cat <file>)"`. Then read it back and run `khora-validate-entity` on it.

A new **type** (universal) is `identity__type: universal`, named `<Name> v1.0.0`, `is_a [[BaseEntity v1.0.0]]` or another universal, and its TOML body declares a `Description` plus any `[Metadata.Relationships]` / `[Metadata.Properties]` / `[Data]` its instances need. Instances of it are `identity__type: particular`.

If a required input is missing, ask the person before proceeding. If a requested relationship or property key isn't declared in any ancestor universal's schema, explain it in plain words as an ontology change and ask for consent rather than writing silently.
