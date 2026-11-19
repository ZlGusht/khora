---
identity__uuid: 3850bcf6-cc51-49e8-98ff-6c78fc6a2085
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #21 Surface contract types]]"
relationships__relates_to:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
properties__priority: 3
properties__resolved: false
properties__type: thought
---
## Context

Task 1 of the Issue #21 run pinned the Maven download's SHA-256 in `src/kernel/.mvn/wrapper/maven-wrapper.properties`. The build then failed on a Linux machine without `unzip`.

## Content

`mvnw` downloads the `.tar.gz` instead of the `.zip` when `unzip` is missing, so one pinned checksum fits only one of the two archives. The pin was dropped; the download comes over HTTPS from Maven Central. Pinning again needs `unzip` on every machine, or a wrapper that checks both archives.