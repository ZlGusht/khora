# ADR-0YE: Security model

> **Status:** Stub
> **Date:** 13.11.2026
> **Issue:** —
> **Related:** ADR-0XZ (rights and access); ADR-0YC (runtime); ADR-0YB (shredding)
> **Number:** to be assigned when added to the repository

## Context

Security is planned from the start but hardened when systems face the outside world. This ADR collects the security properties the architecture already commits to, and holds the place for what is not designed yet.

## Already decided elsewhere

- Private by default, rights by grant, object capabilities (ADR-0XZ decisions 6–8, 13).
- Writes run with the caller's rights intersected with the surface's or behavior's grants (ADR-0XZ decision 16).
- Restrictions travel with derived data and writes (ADR-0XZ decision 17).
- Approval for irreversible, external and untrusted-caused writes (ADR-0XZ decision 18).
- Kept reads and explainable access (ADR-0XZ decisions 19–20).
- Code gets only its declared capabilities (ADR-0YC decision 2).
- Every action is attributed to an actor and on whose behalf (ADR-0YB decision 3).
- Only actions need authority; effects are valid by their cause (ADR-0YA decisions 4–5).
- Encryption per entity or holder, deletion by key (ADR-0YB decision 10).

## Decisions

#TODO

## To design

- **Authentication:** how khora knows who is calling at the API and hub boundary. #TODO
- **Secrets:** credentials for adapters and agents never appear in context, logs or diffs. *Suggestion*: a secret is an entity nobody can read, only use through the adapter that holds it. #TODO
- **Trust boundaries:** kernel, runtime code, external origins, other deployments. #TODO
- **Abuse limits:** load from outside, beyond budgets. #TODO
- **Threat model:** once systems are exposed. #TODO
