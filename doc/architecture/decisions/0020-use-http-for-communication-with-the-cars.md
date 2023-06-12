# 20. Use HTTP for communication with the cars

Date: 2023-04-01

## Status

Accepted

## Context

Using an uncomplicated straight-forward approach to communicate with the devices can be the best choice. 

## Decision

We used HTTP for the Polling Intervals approach and also for image delivery in Optional. 
## Consequences

HTTP uses TCP which creates a small overhead. But it is a widely used protocol that can easily be integrated in every solution. For resource constrained networks, or ECUs something more lightweight can be more effective.
