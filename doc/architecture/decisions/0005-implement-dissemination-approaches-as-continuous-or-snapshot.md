# 5. Implement dissemination approaches as continuous or snapshot

Date: 2023-03-31

## Status

Accepted

## Context

For management reasons it makes sense that cars that are newly added to a fleet are immediately updated with software that should be deployed, when being part of that fleet. For that we use the concept of continuous jobs used in the AWS IoT library.

## Decision

For every update rollout it can be defined if it is a continuous or snapshot rollout. Using snapshot does not require any specific actions. Using continuous, the rollout needs to be registered in the database, such that every car that is added to the fleet after rollout start is automatically updated with the proper image.

## Consequences

Using continuous updates can reduce the effort an administrator needs to put into the fleet management, since those updates are automatically supervised by the system. When using snapshot an administrator would need to start a new rollout for the whole fleet.
