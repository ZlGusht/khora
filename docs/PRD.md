# Product Requirements Document (PRD)

> **Document Status:** Draft
> **Last Updated:** 26.10.2026

---

## Executive Summary

**Product Name:** khora

**Problem Statement:**
The traditional software approach of scoped, top-down brittle system architecting is suffocating the emerging capabilities computation. Fundamental gaps like data discrepancies, stale control and interaction schemes, or fragile autonomous systems are being treated as symptoms instead of having their root cause addressed. 

**Proposed Solution:**
khora works by treating everything as the resource for computation - data. It grows out instead of being built, guaranteeing flexibility for dynamic times and sees scaling as a positive compounding, not a negative consequence to handle. It is a system that aims to store and manipulate meaning at its core. It does not solve the above problems, it simply does not cause them in the first place.

**Success Metrics:**  
- Sustainable, intuitive and seamless experience at scale
- Interoperability without coupling at minimal integration costs
- Emergent functionalities and features
- Scalable autonomous execution

**Launch Target:** Q4 of 26'

---

## Background & Context

### Why Now?
This project has been in the making for some time but now it has real user and usage-specific backing that makes it extremely useful to have. 

### User Research & Insights #TODO
- **Research Methods:** [Interviews, surveys, usage data, etc.]
- **Key Findings:**
  - [Finding 1]
  - [Finding 2]
  - [Finding 3]
- **User Quotes:** 
  > "[Relevant quote from user research]"

### Market Analysis
- **Market Size:** [TAM/SAM/SOM if relevant]
- **Competitive Landscape:** [Key competitors and how they address this problem]
- **Differentiation:** [What makes our approach unique/better]

---

## Goals & Success Criteria

### Business Goals
1. Incorporate project into other businesses
2. Establish a product market fit

### User Goals
1. Easily and intuitively build and interact with khoras.
2. Model and manipulate personal and organizational domains through khoras.
3. Be able to interoperate disparate domains.
4. Enable and enhance processes through computation.

### Success Metrics (Detailed)

| Metric | Baseline | Target | Timeframe | Measurement Method |
|--------|----------|--------|-----------|-------------------|
| [Metric 1] | [Current value] | [Goal value] | [When] | [How you'll measure] | 

### Non-Goals (Out of Scope)
- Does not handle domain systems, those are content and are subject to other work.
- Does not integrate LLMs as part of its core, those are consumers of the system, despite the possibility of the system containing them

---

## User Personas & Use Cases

### Primary Persona: John Doe
- **Role:** Knowledge worker
- **Goals:** Organize and execute their work
- **Pain Points:** They have to hop between different applications and have to transfer information between those. The less applications they use, the less flexible and functional those are.
- **Technical Proficiency:** Intermediate
- **Context:** Working with digital information personally and in an organization like information and work management, research, modeling (explaining) or run computational systems (autonomous systems, numerical analyses, simulations).

### Key Use Cases

#### Use Case 1: Running autonomous systems
**Actor:** John Doe
**Preconditions:** They have set up and mapped a domain in the respective khora
**Flow:**
1. User interacts with the system to define the task that must be achieved.
2. khora dispatches autonomous agents with internal and external access.
3. User observes the process or gets alerted when desired.
4. khora tracks and saves work done, as well as update the khora accordingly.

**Expected Outcome:** Agents are correctly steered, fully aware, and effective, their work is well coordinated, efficient, visible, and traceable.
**Edge Cases:** Agents don't get the correct context or miss relevant parts; task is not well understood or poorly defined.

#### Use Case 2: Organization
**Actor:** John Doe and co
**Preconditions:** Domain module installed. (Optional) Previous usage to populate relevant data.
**Flow:**
1. (Optional) User interacts with the system to observe the state. 
2. User does some work in or outside the khora in the domain.
3. khora analyzes and saves their work, either from a user's input or autonomously. The information is now available to the entirety of the khora.
4. User navigate and manage this information with or without the help of autonomous systems.

**Expected Outcome:** Information is properly stored and corresponds to the intended shape. 
**Edge Cases:** Data duplication in multiple shapes, undiscoverable data. Data is dead/not up to date. Data interaction is hard.

#### Use Case 3: Modeling
**Actor:** John Doe
**Preconditions:** (Optional) Domain model installed.
**Flow:**
1. User creates ontology if absent. They optionally explore and decide what they are going to model.
2. khora maps data to ontologies, once and/or on push and/or pull.
3. User interacts with, manipulates or manages models.
4. khora uses models to run processes and generate.

**Expected Outcome:** Models are easy to create, manage and access from systems. 
**Edge Cases:** Model does not get populated properly or gets outdated. Data is mapped wrong to ontologies. Models are hard to work with.

#### Use Case 4: System development and operation
**Actor:** John Doe
**Preconditions:** Model available.
**Flow:** 
1. User describes system based on model and develops it.
2. khora allows delegation to autonomous systems.
3. User interacts with and/or operates the system.

**Expected Outcome:** System is modeled, developed and operated inside the khora.
**Edge Cases:** System is not discoverable or not scoped correctly. System has side effects
--- 

## Product Requirements

### Functional Requirements

#### Must Have (P0) - MVP
1. **Scoping**
   - **Description:** Data is discoverable and navigable.
   - **User Story:** As a user, I want to have clarity and observability of my khora so that I can manage and operate the data inside it.
   - **Acceptance Criteria:**
     - [ ] Data is structurable and/or structured 
     - [ ] Data is scoped
     - [ ] Scopes can be created and are maintained as visible and explainable

2. **Interoperability**
   - **Description:** Data interoperability is always available without being limited by scoping within a single khora.
   - **User Story:** As a user, I want to have access to all information from my khora in any application or system I use so that I can make use not only of the complete data, but of its meaning and relationships as well.
   - **Acceptance Criteria:**
     - [ ] Data is available for use across systems
     - [ ] Data is flexible despite being scoped

3. **Interaction**
   - **Description:** Intuitive interaction with systems and visualization of meaning.
   - **User Story:** As a user, I want to intuitively interact with my khora without the need to understand and manipulate entities in order to establish a feeling of khora as solid. I should be able to quickly and easily get an idea of what is going on in the khora at any time.
   - **Acceptance Criteria:**
     - [ ] I can create, modify, and view data in my khora.
     - [ ] I am not forced to interact with pure entities, but can do that through representations.
     - [ ] Interactions are not based around entities, but around the user and their experience.
     - [ ] Data is alive, so it feels like I'm interacting with the real world and not an old snapshot of it.

4. **Provenance**
   - **Description:** Traceability and replicability of actions
   - **User Story:** As a user, I want to see how things have come to be so that I can make decisions based on that.
   - **Acceptance Criteria:**
     - [ ] Any action is saved in an interactive log.
     - [ ] It is visible who has done what.

5. **Computations**
   - **Description:** Data is available externally for computational runs.
   - **User Story:** As a user, I want to functional access to entities so that I can run my own computational procedures.
   - **Acceptance Criteria:**
     - [ ] Data is exosed functionally.

6. **Modeling**
   - **Description:** Fundamental structural ontologies.
   - **User Story:** As a user, I want to have building blocks so that I can build and model.
   - **Acceptance Criteria:**
     - [ ] Structural ontology kernel is available and complete.


#### Should Have (P1) - Post-Launch
1. **Collaboration and interoperability**
   - **Description:** khoras of different context should be able to work together as a single khora. Multiple principals should be able to collaborate within and between khoras.
   - **User Story:** As a user, I want to be able to work with different khoras available seamlessly so that the benefits can compound.
   - **Acceptance Criteria:**
     - [ ] My khora and I can interact with different khoras without losing its shape. I can't tell that there are multiple khoras underneath.
     - [ ] All functionalities of my khora are available on the summed khora.
2. **Self-containment**
   - **Description:** khora is itself data in the khora, the model is not separate from the modeled.
   - **User Story:** As a user, I want to develop and extend systems in the language of khora so that those are minimal and conform with the general shape.
   - **Acceptance Criteria:**
     - [ ] A minimal firmware is available to boot up the khora from data.
     - [ ] Any logic outside this firmware is modeled data.

#### Nice to Have (P2) - Future Consideration
1. **Physical world khoras**
   - **Description:** khora is in the digital world, but its capabilities extend naturally to the physical world.
   - **User Story:** As a user, I want to use and interact with the khora in the physical realm, so that its benefits are transferred to the physical world as well.
   - **Acceptance Criteria:**
     - [ ] My khoras include physical objects and attributes.
     - [ ] I can functionalities from the physical and digital khoras interchangeably.


### Non-Functional Requirements

#### Performance
- Usable and improve

#### Security & Privacy #TODO
- [Authentication requirements]
- [Data encryption standards]
- [Compliance requirements: GDPR, CCPA, etc.]
- [User data handling policies]

#### Scalability
- Entities within a single khora will vary, with possible trillions or quadrillions of entities, if those include big data for example.
- Any agent entity might have its own dedicated khora, which includes and is not limited to: humans, institutions and organizations, and agents. Interoperability means that thousands of khoras might be work together.
---

## User Experience & Design #TODO

### User Flow Diagram
[Link to or embed user flow diagram - Figma, Miro, etc.]

### Wireframes/Mockups
[Link to design files or embed key screens]

### Key Interactions
1. **[Interaction Name]**
   - **Trigger:** [What initiates this interaction]
   - **Behavior:** [What happens]
   - **Feedback:** [How system responds to user]

### Design Principles for This Product
- [Principle 1: e.g., "Minimize clicks to core action"]
- [Principle 2: e.g., "Progressive disclosure of complexity"]
- [Principle 3: e.g., "Clear error states and recovery paths"]

---

## Technical Specifications #TODO

### System Architecture
[High-level architecture overview - link to detailed technical design doc if available]

### Data Model
[Key entities and relationships - link to data schema if available]

### API Requirements

#### Endpoints Needed
| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `/api/[resource]` | GET | [Purpose] | [Params] | [Response structure] |
| `/api/[resource]` | POST | [Purpose] | [Body] | [Response structure] |

### Third-Party Integrations
- **[Service Name]:** [Purpose, authentication method, rate limits]
- **[Service Name]:** [Same structure]

### Technical Constraints
- [Known limitations or technical debt that impacts this product]
- [Platform/framework constraints]

### Analytics & Tracking

#### Events to Track
| Event Name | Trigger | Properties | Purpose |
|------------|---------|------------|---------|
| `[event_name]` | [When it fires] | [Data captured] | [Why we track it] |
| `[event_name]` | [When it fires] | [Data captured] | [Why we track it] |

---

## Dependencies & Risks

### Dependencies #TODO
| Dependency | Owner | Status | Impact if Delayed | Mitigation |
|------------|-------|--------|-------------------|------------|
| [Dependency 1] | [Team/Person] | [On Track/At Risk/Blocked] | [Impact] | [Plan B] |
| [Dependency 2] | [Team/Person] | [Status] | [Impact] | [Plan B] |

### Risks & Mitigation

#### High Risk
1. **Performance**
   - **Probability:** High
   - **Impact:** Medium
   - **Mitigation:** Caching, weighting methods, indexing
   - **Owner:** [Who's responsible]
1. **Volume**
   - **Probability:** High
   - **Impact:** Low
   - **Mitigation:** Tiered storage, weighting methods
   - **Owner:** [Who's responsible]

#### Medium Risk
1. **Systems might become intractable** 
   - **Probability:** Medium
   - **Impact:** High
   - **Mitigation:** Bottom-up approaches, heuristics
   - **Owner:** [Who's responsible]

### Open Questions
- [ ] Can cross database queries work efficiently?
- [ ] What top down regularities ontologies have can be exploited and how? Can those inform data design?

---

## Timeline & Milestones #TODO

### Development Phases

| Phase | Timeline | Key Deliverables | Owner |
|-------|----------|------------------|-------|
| **Discovery** | [Dates] | User research, competitive analysis | [PM] |
| **Design** | [Dates] | Wireframes, mockups, user testing | [Design] |
| **Development Sprint 1** | [Dates] | [Core features] | [Engineering] |
| **Development Sprint 2** | [Dates] | [Additional features] | [Engineering] |
| **QA & Testing** | [Dates] | Test plan execution, bug fixes | [QA] |
| **Beta Launch** | [Dates] | Limited release, feedback collection | [PM] |
| **Full Launch** | [Dates] | General availability | [PM] |
| **Post-Launch** | [Dates] | Monitoring, iteration | [PM] |

### Key Milestones
- **[Milestone 1]:** [Date] - [Description]
- **[Milestone 2]:** [Date] - [Description]
- **[Milestone 3]:** [Date] - [Description]

---

## Post-Launch Plan

### Success Criteria Check-in Schedule
- **Week 1:** Usability, domain usability, domain count, cross-domain value
- **Week 4:** Work done in vs outside
- **Week 12:** Scaling performance

### Iteration Plan
- **Feedback Collection:** LOOPs
- **Update Cadence:** On demand

### Learning Goals
- Validate scalability and interoperability assumptions
- Data in the system

---

## Appendix

### References
- [Link to user research]
- [Link to competitive analysis]
- [Link to technical design doc]
- [Link to design files]

### Changelog
| Date | Version | Changes | Author |
|------|---------|---------|--------|
| [Date] | 1.0 | Initial draft | [Name] |
| [Date] | 1.1 | [Description] | [Name] |

### Stakeholder Approvals
- [ ] Engineering Lead - [Name] - [Date]
- [ ] Design Lead - [Name] - [Date]
- [ ] Marketing Lead - [Name] - [Date]
- [ ] Executive Sponsor - [Name] - [Date]