# 17. Use RPC for communication between the services

Date: 2023-04-01

## Status

Accepted

## Context

When needing very fast response times RPC can make sense to achieve that, since it has less overhead than approaches like message queues.

## Decision

Not used in our prototype.

## Consequences

Good in low latency environments. When having high load it can get critical, since requests are not resent if they are not answered.