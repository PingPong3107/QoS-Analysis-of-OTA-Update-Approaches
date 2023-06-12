# 13. Use two partitions on the ECU

Date: 2023-04-01

## Status

Accepted

## Context

When only one partition is used, a full image update involves some risks. If power is cut during the update or anything else goes wrong, the ECU is broken. Replacement can be very expensive or even impossible.
## Decision

Not implemented in our prototype. Cars are modeled much simpler in our case.

## Consequences

Using two partitions, where only one is active, enables robust updates. During update the inactive partition is used to flash the new image. If everything went well the inactive partition is made active after reboot. In any other case the system can safely roll back to the previous version by using the old partition.
