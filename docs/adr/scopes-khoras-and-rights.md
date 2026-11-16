# ADR-0XZ: Scopes, khoras and rights

> **Status:** Proposed
> **Date:** 13.11.2026
> **Issue:** #6 Scopes
> **Related:** ADR-0YA (the model); ADR-0XY (surfaces); ADR-0YE (security model)
> **Number:** to be assigned when added to the repository

## Context

Data needs structure people can navigate (PRD P0 Scoping), and access must follow that structure. Structure, khoras and authority have to emerge from use rather than be set up front.

## Terms

| Term | Meaning |
| --- | --- |
| **Scope** | A surface whose shape is a membership: a meaningful subgraph. |
| **Facet** | A use of a scope. |
| **Khora** | A scope formed around a center. |
| **Field** | A khora's membership: what gathers around its center. |
| **Principal** | Something that holds rights: a person, organization, agent or service. |
| **Right** | Read, use, manage, grant, transfer or invoke, over a scope. |
| **Grant** | An entity passing rights from a holder to a principal. |

## Decisions

### Scopes

**1. A scope is a surface whose shape is a membership.** Membership can be:
- declared: members listed;
- defined: a query;
- structural: given by relations, such as a building and its parts;
- detected: found by an algorithm; membership is soft, and each run is recorded;
- computed by a behavior or process.

Adopting a detected scope pins it as declared or defined.

**2. Uses are facets, not kinds of scope.** One scope can serve several, for example:
- access: grants target it;
- navigation;
- object: it can collapse to one node;
- component: its boundary is its interface;
- context: what a query, process or agent runs within.

**3. Scopes overlap and nest.** They form a lattice, not a tree. A relation belongs to a scope when both its ends do, unless the scope lists relations explicitly.

**4. A scope's grants live in a scope nested inside it.** Holders of the grant right on the scope can read them by default.

### Khoras

**5. A khora is a scope formed around a center.**
- The center can be any entity that gathers others: a person, organization, agent, project, building, product, relationship or body of knowledge.
- Khoras can be partly set up and guided, but are never fully defined top down.
- Fields overlap and confer no rights.

### Rights

**6. No entity has an owner.** Who may do what is decided by rights alone.

**7. Rights originate in creation and pass by grant.** The creator holds all rights, and nobody can grant more than they hold. This is the object-capability model.

**8. Rights come as a bundle:**
- read;
- use: compute with it, derive from it, copy it out;
- manage: change it; for a surface or behavior, change its logic;
- grant: pass rights on and revoke them;
- transfer;
- invoke: use the operations a surface or behavior offers, without reading its logic.

**9. The grant used decides on whose behalf an action happens.** That principal holds the rights over what the action creates.

**10. Outside authority is a grant from another khora,** such as a municipality's right to read compliance data.

**11. External entities mirror their source's rights.** Grants can narrow them, never widen them.

**12. Copies exist only where parties genuinely see something differently.** Otherwise there is one entity with several holders.

### Access

**13. Everything is private by default.**
- Access needs a chain of grants from a holder, over a scope that contains the entity.
- Re-granting needs the grant right.
- There are no deny rules in the MVP.
- Authority is checked on actions only. Outcomes, effects and observations are valid by their cause (ADR-0YA decisions 4–5).

**14. A relation is visible only if both its ends are visible.**

**15. Detected scopes never carry grants.** Defined scopes carry them only when the grant says so explicitly.

**16. A write runs with the caller's rights intersected with the grants of the surface or behavior it goes through.** It never runs with the surface's or behavior's authority alone.

**17. Restrictions travel.**
- A derived value, and anything written from it, is readable only by those who may read all its inputs.
- Lineage comes from the declared surface graph.
- Aggregates over partly hidden members are marked partial.

**18. Holders of a scope set which writes need approval.** By default these are irreversible writes, external writes, and writes whose cause includes content from an untrusted origin.

**19. Holders of a scope can require reads in it to be kept.** Every read is an action (ADR-0YB decision 11); this sets which ones are kept durably.

**20. Access is enforced by the runtime and is explainable.**
- Read access is materialized as labels, a kernel surface, and pushed down as filters.
- A revocation reaches the labels before any later read is served.
- For any principal and right, the system can show the grant chain, or why none exists.

### Identity and distribution **(interop: review)**

**21. Identities need no coordination.**
- Immutable content is identified by its hash.
- Everything else is identified by issuer plus a local id. Principals use decentralized identifiers (DIDs).
- Real-world things carry their external ids: cadastral number, company registry number, LEI, ORCID.
- Uncertain matches become a same-as relation with a confidence, never a merge.

**22. Another deployment's log is an origin.**
- Deployments publish only what they choose to. Lookups go through a distributed index of pointers.
- Replicas are surfaces over a remote origin.
- Diverging replicas are flagged, never silently resolved.

## Costs and risks

- Revocation must cascade through delegated grants and labels before later reads. Stale labels are a security hole, not a performance issue.
- Defined access scopes widen silently as new data matches their query.
- Several holders of the manage right can make conflicting changes.
- Partial markers and governance scopes reveal that hidden things exist.
- Whoever runs a deployment's storage can read it, unless content is encrypted to the holders' keys.

## Alternatives considered

- **Fixed scope kinds:** one subgraph usually serves several uses.
- **An owner per entity:** shared projects and buildings would need copies and reconciliation.
- **A constitution per khora assigning authority:** top-down.
- **Global consensus replicating everything:** khora replicates by need.

## Open questions

| Question | Lean |
| --- | --- |
| How a khora's field is defined | Propagation from the center over relations, use and rights |
| Is the existence of hidden entities protected? | Open |
| Lineage per entity or per surface | Per surface by default; per entity where needed |
| Identity scheme: hash function, DID method | With interoperability |

## MVP

Decisions 1–20, with declared, defined and structural membership. Detected membership comes with the first spec that needs it. Decisions 21–22 come with interoperability.

## Done when (issue #6)

Access tests cover:
- private by default;
- re-granting;
- revocation before later reads;
- relation visibility;
- write authority as an intersection;
- restrictions on derived values and writes;
- partial aggregates;
- approval for untrusted causes;
- recorded reads;
- explaining access.
