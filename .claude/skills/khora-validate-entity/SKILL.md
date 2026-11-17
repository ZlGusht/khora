---
name: khora-validate-entity
description: Validate a single Khora vault entity against its full is_a chain and the schemas declared along it — is_a chain reaches BaseEntity, relationship keys are vocabulary-blessed, properties conform to parent schemas, frontmatter parses, no unresolved Templater placeholders. Pure read; no mutation. Use after create-entity or any update, or when asked to audit an entity.
---

# khora/validate-entity

Pure read. Thin orchestration around a deterministic validation script. Surfaces violations; does not fix them.

## Backing script

`.claude/scripts/khora-validate-entity.py` does the structural work — chain walk via `obsidian read`, schema union, key-set membership, placeholder scan. The script is the canonical implementation; this skill is the entrypoint and result interpreter.

## Approach

Invoke the script with the entity name:

```
bash .claude/scripts/khora-validate "<entity name or vault-relative path>"
```

Add `--format json` if you need machine-readable output for downstream skills. Exit code 0 = PASS; non-zero = FAIL or hard error.

Read the script's report. If the result is PASS, say so and stop. If FAIL, surface each invariant's violations in the conversation. Do not attempt to fix them — that's the caller's decision.

## Invariants the script checks

1. **is_a chain reaches `[[BaseEntity]]`** — no cycles, no broken parents. Both unversioned (`[[BaseEntity]]`, the alias) and legacy versioned (`[[BaseEntity v1.0.0]]`) terminators are accepted; new entities should use the unversioned form.
2. **Closed relationship vocabulary** (rule 4) — every `relationships__<key>` is declared in some ancestor's `[Metadata.Relationships]`.
3. **Property key membership** — every `properties__<key>` is declared in some ancestor's `[Metadata.Properties]`. `#TODO: extend invariant 3 to value-type / range / enum validation` (script has the same breadcrumb).
4. **Frontmatter YAML parses.**
5. **Body TOML parses** (where a ` ```toml ` fence is present).
6. **No unresolved Templater placeholders** — no literal `<% … %>` left.

Parent universals' schemas are trusted inputs. A malformed universal surfaces as a chain-walk or TOML-parse violation, not as something this skill repairs.

## Calling from other skills

This skill is the canonical post-change check. `khora/create-entity` should call it as the final step. Any future mutation skill should too. The script's `--format json` output is intended for that programmatic use.

## Out of scope

- Mutation. Pure read.
- Universal-schema validity (TOML well-formedness of universal bodies). `#TODO: khora-validate-universal if universal-side bugs become recurring.`
- Cross-entity reference integrity (every wikilink in body content points to an existing entity). `#TODO: khora-validate-references — different cost profile, scans whole vault.`
- Auto-repair.
