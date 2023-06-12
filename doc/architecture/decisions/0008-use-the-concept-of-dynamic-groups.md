# 8. Use the concept of dynamic groups

Date: 2023-04-01

## Status

Accepted

## Context

In some instances it makes sense to define groups dynamically over constraints that define the cars / devices that should be part of the group. For instance, a car should have hardware xy to be in the group.

## Decision

Not implemented in our prototype.

## Consequences

Handling multiple fleets can become easier, especially if cars are meant to be switched between groups, this can be done by changing the pivotal variable, which leads the system to regroup the car. Also, rollouts can be secured regarding compatibility if constraints are used like a certain hardware type.
