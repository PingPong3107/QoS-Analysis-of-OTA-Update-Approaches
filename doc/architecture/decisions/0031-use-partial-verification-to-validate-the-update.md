# 31. Use partial verification to validate the update

Date: 2023-04-03

## Status

Accepted

## Context

When having less computational capabilities or an ECU with only small amounts of memory, one option is the use of partial verification in Uptane. For that, only target and snapshot metadata gets validated.

## Decision

Not used in our prototype.

## Consequences

Security is reduced in this approach, allowing more attack options. But it is applicable to a greater variety of different ECU types with high resource constraints. To provide more security a primary ECU should perform full verification and send the respective images and metadata to the secondaries, which perform at least partial verification. With that security is much higher with fewer attack options, like compromising the primary ECU.

