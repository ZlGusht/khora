# ADR-0XZ: Scopes, khoras and rights

> **Status:** Proposed
> **Date:** 08.11.2026
> **Issue:** #6 Scopes
> **Related:** ADR-0XX Query model and propagation; ADR-0XY Entity sources, time and schema (amended by decision 38)
> **Number:** to be assigned when added to the repository

## Context

Everything in khora is data, and data on its own has no structure: entities and relations, up to trillions of them. The PRD makes scoping a P0 requirement: data must be structured, discoverable and navigable, and scopes must be visible and explainable. Scopes are also meant to carry access and security.

The sample specs ask for more:

- **Access:** per khora, seat, role and party (company brain); per principal, executors included (application development).
- **Structure:** components, modules, collectives; blast radius across boundaries; the parts of a building or a network.
- **Levels of description:** seeing a whole system aggregated, then decomposing parts of it into their actual structure, in networks where scale is not obvious.
- **Bottom-up detection:** modules, communities and components found by algorithms rather than declared.
- **Context:** what a query, process or agent runs within.

The specs also depend on a definition of a khora, which until now was open, and on who governs what when several parties share a project, a building or a relationship.

khora grows bottom-up. Structure, khoras and governance must emerge from what happens in the system, not be set up front by a central rule. This ADR fixes what a scope is, how scopes give levels of description, what a khora is, where rights come from and how access works, and what changes when khoras live on several deployments.

## Terms

| Term | Meaning |
| --- | --- |
| **Scope** | An entity whose content is a meaningful subgraph. |
| **Membership** | Which entities and relations a scope contains. A set-valued entity. |
| **Facet** | A use of a scope: access, navigation, object, component, context. |
| **Lens** | A hierarchy of scopes: one way of decomposing a graph into levels. |
| **Cut** | The set of scopes expanded in a view; everything else is shown collapsed. |
| **View** | A lens, a cut and a representation. |
| **Khora** | A scope formed around a center. |
| **Center** | The entity a khora forms around. |
| **Field** | A khora's membership: what is pulled toward its center. Soft; confers no authority. |
| **Principal** | Something that acts: a person, an organization, an AI agent, a process, a collective. |
| **Right** | One of read, use, manage, grant, transfer, over a scope. |
| **Holder** | A principal holding a right without needing a grant for it. |
| **Grant** | An entity passing rights from a holder (or grantee allowed to re-grant) to a principal over a scope. |
| **Agreement** | An entity by which principals create a collective and its decision rule. |
| **Frontier** | A snapshot across several logs: one position per log. |

```
   field of org khora            field of person khora
  ( · · ·  ● org  · · · )       ( · ·  ● person  · · )
  (   seat ▢    team ▢  )~~~~~~~(  notes ▢          )
  (        ┌────────────┼───────┼──────┐            )
  (        │ project scope (bounded,   │            )
  (        │ crosses both fields)      │            )
           └───────────────────────────┘
     ● center   ( ) field: soft, overlapping   ▢ ┌┐ scopes: bounded
     rights: created, then passed on by grants; no owner underneath
```

## Decisions

### Scopes

**1. A scope is an entity whose content is a meaningful subgraph.** It is data like anything else: it has an id and a history, can be related to other entities, queried, and be a member of other scopes.

**2. Membership can come from any entity source.** Membership is a set-valued entity, produced however entities are produced:

- **Declared:** members listed explicitly ("these 12 documents are the grant application").
- **Defined:** membership is a `Query`, e.g. project P and everything reachable from it through part-of, to fixpoint.
- **Structural:** given by relations in the model itself, e.g. a building and its parts.
- **Detected:** produced by a library algorithm (Leiden, Infomap, label propagation). Membership is soft (a degree), may overlap, and carries confidence and stability. Each run is a version. Adopting a detected scope pins it as declared or defined.
- **Produced by a process:** computed by an opaque process, known to the planner only by what it reads.

**3. Uses are facets, not kinds.** A scope carries any combination of:

- **Access:** grants target it.
- **Navigation:** a place to look; a view's unit.
- **Object:** the subgraph treated as one thing (decision 9).
- **Component:** the subgraph plus its boundary. Relations crossing the boundary are its interface, so coupling, cohesion and blast radius follow from it.
- **Context:** the extent a query, process or agent runs within.

There are no fixed scope kinds.

**4. Scopes overlap and nest.** An entity can be a member of many scopes, and a scope can be a member of another. Scopes form a lattice, not a tree; union, intersection and difference are set operations on membership.

**5. Relations are induced by default.** A relation belongs to a scope when both its ends do, unless the scope lists relations explicitly. Relators, being entities, have their own membership.

**6. Membership is pinned to time.** "Members of S as of frontier f", like every read (decision 38). Membership changes are actions in the log.

**7. A scope's governance is a scope inside it.** The grants and agreements that concern a scope are entities, and live in a nested scope of it. Governance is more structure, not a separate mechanism. By default the governance scope is readable by holders of the grant right over the parent scope.

### Levels of description and views

**8. Views are built from scopes, whatever represents them.** The canvas is one representation; summaries, reports and compressed context for agents are others. They share the mechanism below.

**9. A scope with the object facet has two states.**

- **Collapsed:** one node. Its attributes are aggregated from its members (counts, sums, worst status, lowest confidence), and its edges are the crossing relations bundled. Both are `Aggregate` with an algebra (ADR-0XX).
- **Expanded:** its members and their internal relations, each member possibly a collapsed scope itself.

Kinds may define their own collapsed representation: a building collapses differently from a team.

**10. A hierarchy of scopes is a lens.** The same graph has several lenses: governance, work breakdown, physical structure, detected communities. A lens chooses a nesting; where scopes overlap, the lens decides (one parent, or shown in several places). The scopes themselves are unchanged.

**11. A view is a lens, a cut and a representation.** The cut is the set of expanded scopes, so the level of detail varies by region rather than globally.

**12. Networks without obvious scale get lenses from hierarchical detection.** Multi-resolution community detection gives nested partitions, where a coarser resolution is a higher level. Detected lenses must be stable across levels and reruns; otherwise expanding a scope reshuffles everything around it and the structure cannot be learned.

**13. Lenses and their aggregates are materialized.** They are precomputed and refined on demand for the cut in view, as map tiles are.

### Khoras

**14. A khora is a scope formed around a center.** A scope in general is defined by its boundary; a khora is defined by its center. Its membership is its field: the entities pulled toward the center.

**15. The center can be any entity that gathers others.** A person, an organization, an AI agent, a project, a building, a product, a relationship, a body of knowledge, a modeled system. The center need not be a principal: a building's khora persists while the people holding rights over it change.

**16. Khoras form; they are not set up.** A khora forms when entities start gathering around a center. Of ten agents in a fleet, some may trigger their own khoras and some may not.

**17. Fields overlap and confer no authority.** An entity can be in several khoras' fields at once, e.g. a document shared by two companies. Being in a field, however strongly, never grants a right.

**18. A khora is not a deployment unit.** A deployment hosts many khoras; a khora can span several deployments and be computed on demand.

**19. A khora is not a schema boundary.** One khora can hold several upper ontologies, joined by a resolution layer (ADR-0XY).

**20. Khoras are cheap.** A khora is a scope with a center, so it costs what a scope costs.

**21. Seats are scopes.** A seat is a scope in the organization's khora. The occupant's own khora links to it by grant.

### Rights

**22. There is no ownership and no binding.** Entities are not bound to a khora and have no owner underneath. Each entity has one identity (decision 39). Who may do what is decided by rights alone, and what travels with a party is what it holds rights to, plus the copies it may take.

**23. Rights originate in creation and pass by grant.** Whoever creates an entity holds all rights over it. A holder passes rights on with a grant; nobody can give more than they have. Nothing central decides who may do what. This is the object-capability model.

**24. Rights come as a bundle.**

| Right | Allows |
| --- | --- |
| **Read** | Seeing the entity. |
| **Use** | Computing with it, deriving from it, copying it out. |
| **Manage** | Changing it, and its structure (membership, schema). |
| **Grant** | Passing rights on, and revoking them. |
| **Transfer** | Handing rights to another holder. |

Different parties can hold different rights over different scopes of the same thing: the owners' association manages a building's common structure, the utility its gas installation, the owner of an apartment everything about it.

**25. Every action records an actor and on whose behalf it acts.** The grant used decides on whose behalf: an agent acting under a grant from the organization's seat scope acts for the organization, so rights over what it creates go to the organization. An agent acting under its own rights acts for itself. The actor is always recorded for provenance (PROV-O `actedOnBehalfOf`).

**26. Shared rights come from agreements.** An agreement is an entity. It creates a collective principal and its decision rule (majority, unanimity, a lead with a veto), and the collective holds the rights the parties put in. Joint ownership is this, not a separate concept.

**27. Outside authority is a grant from another khora.** A municipality's legal right to read compliance data is a grant or claim from the municipality's khora, recognized by the parties. It is not a privilege of the system.

**28. External entities mirror their source's rights.** Rights over a synced file follow what the source system allows. Grants in khora can narrow them, never widen them beyond the source.

**29. Copies exist only for differing perspectives.** Where parties genuinely see something differently, each side is its own entity, linked to the one thing they concern (e.g. each party's version of a contract, linked to the relationship). Otherwise there is one shared entity with several holders.

### Access

**30. Effective access.** A principal P may exercise right r on entity e if a chain of grants leads from a holder of r over e to P, covers a scope containing e, and all its conditions hold.

**31. Everything is private by default.** No chain, no access. Holders exercise their rights without grants. There are no deny rules in the MVP.

**32. Re-delegation needs the grant right.** A grantee can pass on only what it was granted, and only if the grant includes the grant right.

**33. Relations need both ends.** A relation is visible only if both its ends are visible to the principal and the relation itself is covered.

**34. Detected scopes never carry grants.** An algorithm drawing a boundary does not change who can see what. Defined scopes carry grants only when the grant says so explicitly, since every entity that newly matches the query becomes visible.

**35. Execution runs under least privilege.** A process or agent acts with the intersection of what its invoker may do and what the process itself is granted.

**36. The planner enforces access.** `ctx.principal` is in the source contract (ADR-0XY). Read access is materialized as labels and pushed down to sources as filters, so an access check is never a traversal at read time. A source that cannot filter returns data to the planner, which filters before anything reaches the caller; such a source must be trusted with the data.

**37. Restrictions travel with derived data.** A result, aggregate or computed entity can be read only by principals who may read all of its inputs. A collapsed scope shows aggregates over its visible members, marked partial when members are hidden. Releasing a derived result beyond that (e.g. published anonymized statistics) needs the use right on the inputs and an explicit grant. The past is read with current grants; audit is a separate right.

### Time and distribution

**38. A snapshot is a frontier.** Logs are entities and can be joined, but actions in different logs that do not know of each other are concurrent and have no natural order. A snapshot is therefore a frontier, one position per log, and a result is partial when a log has not arrived up to the requested position. This amends ADR-0XY: `asOf` is a frontier, and record time is a position in one of several logs. **(interop: review)**

**39. Identities are global and need no coordination.**

- **Immutable content** (documents, versions, log entries) is identified by its hash. The same content has the same id everywhere, so deduplication is automatic.
- **Everything else** is identified by its issuer and a local id. Principals have decentralized identifiers (W3C DIDs); attested records are verifiable credentials.
- **Real-world things** (a building, a company, a person) carry their external identifiers: cadastral number, company registry number, LEI, ORCID.
- **Uncertain matches** produce a same-as relation with a confidence, never a silent merge.

**(interop: review)**

**40. Location is published selectively.** Deployments publish the claims they choose to (ADR-0XY), and lookups across deployments go through a distributed index of pointers, not content. Anything unpublished is found only through a grant. **(interop: review)**

**41. Replicas are one entity.** Copies of an entity on several deployments are one entity because they share an identity. When they diverge, the divergence is either a perspective (decision 29) or a conflict, and conflicts are flagged, not resolved silently. **(interop: review)**

### Orphans

**42. Unclaimed orphans decay.** An entity or cluster no live principal holds rights over is an orphan. Orphans that are not claimed decay by default. How orphans are prevented, found and claimed is open (see open questions).

## Caveats

On scopes and views:

- **Soft and crisp membership meet.** Detected scopes are soft; access is crisp. Decision 34 keeps them apart, but every lens built on detected scopes needs a membership threshold.
- **Defined access scopes widen silently.** New data matching the query becomes visible. Decision 34 makes this explicit, not safe.
- **Lens stability is hard.** Community detection is sensitive to small changes; stable hierarchies need seeded or incremental methods, and stability has to be measured.
- **Materialization costs.** Lenses, aggregates and access labels all have to be kept up to date as data changes.

On khoras:

- **Fields shift.** A field computed from relations and use changes as data changes. That is harmless while it confers no authority (decision 17), but anything built on it, such as context assembly or claiming orphans, inherits the instability.
- **A khora may be a pattern rather than a kernel concept.** If a khora is a scope with a center and a field, it may need no mechanics of its own. That changes what "interoperability between khoras" means in the PRD: it becomes federation of one entity space across deployments.

On rights and access:

- **Revocation has to cascade.** Revoking a grant must revoke everything delegated from it and reach the access labels before any read at a later frontier is served. Stale labels are a security hole, not a performance issue.
- **Several holders can conflict.** Creation and grants naturally give several principals the manage right over one entity; concurrent changes need merging or contradiction flags.
- **Derived restrictions need lineage.** Decision 37 requires knowing every derived entity's inputs. ADR-0XX carries only confidence and age by default, so either provenance becomes a default annotation for derived entities (cost) or restrictions are tracked per scope (coarser). The rule can also leave a derived entity with no possible reader, an orphan by construction.
- **Existence leaks.** Partial markers on aggregates, governance scopes and orphan detection reveal that hidden things exist.
- **Scopes are logical isolation.** Untrusted executors need physical isolation (sandboxes) underneath; scopes alone do not stop an escaped process.
- **There is a physical backstop.** Whoever runs a deployment's storage can read it. Better as an explicit, logged break-glass action than ignored. With data encrypted to holders' keys the backstop disappears, and orphan prevention becomes the only safeguard.

On orphans:

- **Decay is deletion.** It cannot be undone, and it collides with legal retention (contracts, financial records). Decay should be staged (archive, then delete) and respect retention.

On distribution:

- **Cross-deployment snapshots are weaker.** A frontier is consistent only up to what has arrived; results across deployments are often partial.
- **Identity matching is uncertain.** Same-as relations carry confidence; queries across them inherit it.

## Consequences

**Positive**

- One structure carries access, navigation, objects, components, context and governance; there are no scope kinds to choose between.
- Declared, defined, structural and detected structure live side by side, so structure can grow bottom-up and be adopted when it proves useful.
- Levels of description come from scopes and existing aggregation semantics; the canvas, reports and agent context use one mechanism.
- Khoras form around anything that gathers entities, overlap freely and cost nothing extra.
- Governance emerges from creation, grants and agreements, with no owner or constitution set up front, and handles parties sharing parts of one thing.
- Access is private by default, enforced in one place, and never needs a traversal at read time.
- Shared entities need no copies; deduplication across deployments follows from identity.

**Negative**

- Materialized lenses, aggregates and access labels have to be maintained.
- Lineage for derived restrictions, and cascading revocation, add cost to every write.
- Snapshots across deployments are frontiers and often partial.
- Orphans need mechanics that are not yet designed.
- The company brain spec's placement rule, ownership language, participant khoras and promotion of scopes to khoras no longer apply and must be rewritten.

## Alternatives considered

- **Fixed scope kinds:** replaced by facets; the same subgraph usually serves several uses.
- **Access as the base every scope is intersected with:** access is one facet; intersection happens at execution (decision 35).
- **Membership always as a query:** replaced by membership from any entity source.
- **A khora as the deployment stack:** rejected; a fleet of agents can have separate khoras on one instance, and one khora can span deployments.
- **A khora as a schema boundary:** rejected; several upper ontologies can live in one khora.
- **A khora as a top-level container:** rejected; khoras are defined by their center, and their fields overlap.
- **A khora centered only on a principal:** rejected; projects, buildings, products and relationships gather entities around themselves too.
- **Ownership and binding of every entity to one khora:** rejected; shared entities would need copies and reconciliation, and an owner per entity is a top-down assumption that shared projects and buildings break.
- **A constitution per khora assigning authority:** rejected as top-down; authority comes from creation, grants and agreements.
- **One global action log:** replaced by frontiers over joined logs.
- **Global consensus (blockchain-style replication of everything):** rejected for general data; khora replicates by need, not by default. Possibly useful later for a few global registries.
- **Seats as khoras, promoted later:** replaced by seats as scopes in the organization's khora.

## Open questions

| Question | Lean |
| --- | --- |
| How is a khora's field defined? | Propagation from the center over relations, use and rights, e.g. a personalized-PageRank preset of `Propagate`; open |
| Several holders of the manage right: merge, flag, or a rule? | Merge where the kind allows it, contradiction flags otherwise |
| How are orphans prevented, found and claimed? | Succession named by holders, falling back to grantor or creator; detection as a query over rights; claims by pull, contestable for a period; then decay |
| Is the existence of hidden entities itself protected? | Open |
| Lineage for derived restrictions: per entity or per scope? | Open; depends on the cost of default provenance |
| Deny rules and grant conditions (time, purpose) | Leave out of the MVP |
| Membership threshold for soft scopes in lenses | Per lens |
| Which lenses are materialized first | Governance and work breakdown (company brain), physical structure (built environment) |
| How to measure lens stability | Overlap between consecutive runs and adjacent levels |
| Identity scheme: hash function, DID method, issuer-local ids | Open; settle with interoperability |
| Claim publication and lookup across deployments | Distributed index of pointers; study Holochain and Solid |

## Out of scope (TODO)

- The interoperability protocol: claim publication, cross-deployment lookup, replication and conflict handling.
- Representations and renderers, including the canvas.
- The process engine.
- Physical isolation, sandboxing and encryption.

## Done when (issue #6)

- This ADR is accepted.
- The principal, grant and agreement model (decisions 22–29) is specified, private by default.
- Access contract tests cover: private by default, re-delegation, revocation before later reads, relation visibility, least privilege, derived restrictions, partial aggregates.
- ADR-0XY is amended for frontiers (decision 38).
- The company brain spec is updated: seats as scopes, rights instead of ownership and placement, and its open question on what defines a khora closed.
