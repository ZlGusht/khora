---
identity__uuid: 98f11f79-48e1-4c6f-9205-242bc3398094
identity__type: universal
identity__created_at: 2026-11-16
identity__tags: []
aliases:
  - Person
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
A human the venture interacts with: the team, founders, investors, mentors, advisors, partners, helpers.

One entity per real person, as a particular (identity__type: particular) is_a [[Person v1.0.0]]. The filename is the person's full name as best known; nicknames, initials and transliterations go into `aliases` so search and agents resolve them. Always search for an existing Person under every known name before creating one. A person only mentioned in passing does not get an entity; name them in plain text in the Call body instead. Create the entity when they become someone we act on: contact, decide about, assign, or get introduced to.

History with a person is NOT stored on the Person. It is derived from the graph: the Calls they took part in, Decisions they made, Tasks assigned to them. The body holds only durable facts: who they are, how we know them, what they care about, how they can help.
"""

[Metadata.Relationships]
member_of = { type="list<connection>", description="Organizations this person belongs to or works for.", optional=true }
introduced_by = { type="connection", description="The Person who introduced us to this person.", optional=true }

[Metadata.Properties]
role = { type="text", description="Their role in plain words, e.g. 'Partner at LAUNCHub', 'Mentor (MTY)', 'Co-founder'.", optional=true }
link = { type="text", description="One canonical external profile URL, e.g. LinkedIn.", optional=true }
stage = { type="text", description="Where the relationship stands for the venture (CRM stage), set deliberately. Leave empty for the team and for people we are not pursuing.", values=["identified", "reached_out", "in_conversation", "engaged", "partner", "dormant"], optional=true }

[Data]
Notes = { type="text", description="Durable facts: background, interests, what they can help with, sensitivities.", optional=true }
```