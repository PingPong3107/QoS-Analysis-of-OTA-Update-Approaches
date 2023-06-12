# 12. Use a checksum in update delivery

Date: 2023-04-01

## Status

Accepted

## Context

It must be ensured that the update file is not changed during transmission.

## Decision

Not implemented in the prototype.

## Consequences

Checksums ensure that the image is not changed during the transmission. The checksum needs to be checked at the ECU. Depending on the ECU this may be difficult. To increase security even check with an original checksum provided by the server, to make sure that it really came from the intended origin.
