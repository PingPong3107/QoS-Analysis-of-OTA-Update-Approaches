# 3. Implement approach that uses broker to offer the Update Image

Date: 2023-03-31

## Status

Accepted

## Context

Offering the image directly may save some time compared to approaches, where notifications need to be sent out first.

## Decision

Images are directly published to a fanout exchange, that offers the image to the cars' message queues. 

## Consequences

Could lead to problems with large image sizes.
