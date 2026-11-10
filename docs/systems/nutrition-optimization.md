## 1. Overview

- **System name:** Nutrition optimization
- **Purpose:** Helps user keep an interesting and nutritious diet that fuels their activity.
- **Scope:** Meal tracking and planning, activity tracking, recipe index, shopping list
- **Stakeholders:** Users are people who care about their nutrition and finances and want to own their own data 
- **Success criteria:** Users run their meal planning and grocery shopping actively through the system.

## 2. Context and boundaries

| Element | Description | Source / destination |
| --- | --- | --- |
| Inputs | Personal activity data, personal preferences, recipe stores  | activity and food models |
| Outputs | Grocery and nutrition plans | Plans |
| External interfaces | Store promotion and stock data, p | API/scrape |

The system boundary is: System only encompasses the present, looking at up-to date information and generating plans.

## 3. Requirements

| ID | Type | Requirement | Priority | Verification |
| --- | --- | --- | --- | --- |
| R1 | Functional | The system shall present users with custom optimized plans for shopping and nutrition. | Must | Plans are presented based on user specifications that include where to buy what and do what with it, based on general or specific nutritional needs |
| R2 | Functional | The system shall allow high level of personalization. | Should | Users should be able to specify any requirements and enforce these on the plans |

Each requirement is atomic, testable, and traceable to a stakeholder need.

## 4. Architecture

- **Components:** Store tracker, activity tracker, meal accessor, meal processor
- **Interactions:** Activity tracker tells meal processor what is the demand, it processor uses accessor to draft potential plans and upon confirmation creates the optimized grocery plan with the store tracker
- **Data:** Store, Product, Activity, Recipe, Meal

## 5. Constraints, assumptions and risks

- **Constraints:** Potential physical activity tracking must be done from outside.
- **Assumptions:** It is doable to collect and parse store product data