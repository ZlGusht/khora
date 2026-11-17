# System Specification Template

## 1. Overview

- **System name:** \[name\]
- **Purpose:** \[one sentence: what the system does and why it exists\]
- **Scope:** \[what is in; what is explicitly out\]
- **Stakeholders:** \[who uses, owns, operates, or is affected by it\]
- **Success criteria:** \[measurable outcomes that mean the system works\]

## 2. Context and boundaries

| Element | Description | Source / destination |
| --- | --- | --- |
| Inputs | \[what enters the system\] | \[where from\] |
| Outputs | \[what the system produces\] | \[where to\] |
| External interfaces | \[other systems, users, environment\] | \[protocol / medium\] |

The system boundary is: \[where the system ends and the environment begins\].

## 3. Requirements

| ID | Type | Requirement | Priority | Verification |
| --- | --- | --- | --- | --- |
| R1 | Functional | The system shall \[behavior\]. | Must | \[test / inspection / analysis\] |
| R2 | Functional | The system shall \[behavior\]. | Should | \[method\] |
| R3 | Performance | \[metric\] within \[target + unit\]. | Must | \[method\] |
| R4 | Reliability | \[availability, failure tolerance\]. | Must | \[method\] |
| R5 | Security | \[access, data protection\]. | Must | \[method\] |

Each requirement is atomic, testable, and traceable to a stakeholder need.

## 4. Architecture

- **Components:** \[list each part and its single responsibility\]
- **Interactions:** \[what talks to what, and how; add a diagram here\]
- **Data:** \[key entities, where they live, who owns them\]
- **Behavior:** \[main states, modes, or flows, including failure handling\]

## 5. Constraints, assumptions and risks

- **Constraints:** \[budget, time, technology, regulation, physical limits\]
- **Assumptions:** \[what is taken as true but not guaranteed\]
- **Risks:** \[what could fail, its impact, and the mitigation\]

## 6. Verification

&#91;How the system is shown to meet each requirement: tests, reviews, simulations, acceptance criteria.\]

## 7. Open questions

- [ ] &#91;Unresolved decision, with owner and date\]
