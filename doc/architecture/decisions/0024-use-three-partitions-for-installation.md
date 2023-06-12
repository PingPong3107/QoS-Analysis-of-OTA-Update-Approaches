# 24. Use three partitions for installation

Date: 2023-04-01

## Status

Accepted

## Context

To increase robustness and fault-tolerance even further it makes sense to use even a third partition as a rescue system on a different memory.

## Decision

Not used in our prototype, since we simulated the cars much simpler.

## Consequences

Even if the whole memory of the ECU breaks during an update (so both partitions) there is still a secondary memory unit that provides a rescue backup of the old version. Installation can take a bit longer, since this rescue version needs to be updated after every rollout.
