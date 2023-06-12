# 7. Use the concepts of static groups

Date: 2023-03-31

## Status

Accepted

## Context

Cars need to be grouped in fleets, one approach is to a unique ID for every car. A list of IDs represents a fleet or car group.

## Decision

A car group is saved as a list of IDs and a group ID. Also, other information about the group is provided. In our case current and previous installed image for that whole group. (Assuming only one consistent update state)

## Consequences

Car groups can be modeled now.
