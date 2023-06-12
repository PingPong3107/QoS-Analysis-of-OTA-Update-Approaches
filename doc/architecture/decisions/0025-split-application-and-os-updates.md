# 25. Split application and OS updates

Date: 2023-04-01

## Status

Accepted

## Context

Application and OS update should be split into parts to reduce downtime.

## Decision

Not used in our implementation: Simplicity of car simulation.

## Consequences

Downtime is reduced when only the OS gets updated at first. The OS update can be verified separately, and then the application gets updated.
