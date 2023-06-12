# 23. Only use one partition for installation

Date: 2023-04-01

## Status

Accepted

## Context

Having only very limited memory capabilities on the ECU this approach might be the only choice.

## Decision

Not used in our prototype. Car are simulated much simpler.

## Consequences

The risk for errors is reduced since a self-contained firmware image is used. The problem is, if something goes wrong the ECU is broken.
