# 18. Use a monolithic architecture

Date: 2023-04-01

## Status

Accepted

## Context

To reduce the effort that is needed to maintain a complex system, a monolithic architecture can be helpful in smaller setups. For example having a framework that only serves a small company fleet. 

## Decision

Not used in our prototype.

## Consequences

When having larger fleets other architecture types might be more sensible, since monolithic architectures are generally not horizontal scalable. Also, there are downsides in agility, since changes in one component, might involve changes in other components too, since they are closely coupled.