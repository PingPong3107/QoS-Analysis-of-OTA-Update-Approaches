# 27. Xdelta as delta algorithm

Date: 2023-04-01

## Status

Accepted

## Context

Reducing the overall data to be sent in an update is important for response, completion and installation times.

## Decision

Not used in our prototype.

## Consequences

Images are split in blocks and checksums are created for the blocks. If two checksums are equal, the corresponding block does not need to be sent. The amount of data to be sent gets reduced by the size of the redundant blocks. If the differences between previous and current image reach a certain threshold, delta images can even be larger than the actual image. One advantage is that this approach only needs sall amounts of memory on the server compared to other approaches.
