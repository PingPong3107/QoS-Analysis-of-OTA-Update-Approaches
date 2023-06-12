# 28. ZChunk as delta algorithm

Date: 2023-04-01

## Status

Accepted

## Context

Reducing the overall data to be sent in an update is important for response, completion and installation times.

## Decision

Not used in our prototype.

## Consequences

ZChunk needs more memory as other approaches since it saves data in chunks fragmented over the whole memory. A metadata file is used which describes which chunks are needed for the update. Cars can then determine which chunks need to be downloaded for the update.
