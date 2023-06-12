# 19. Use a microservices architecture

Date: 2023-04-01

## Status

Accepted

## Context

For large setups scalable solutions are necessary.

## Decision

Used in our prototype. We have multiple services: MongoDB, MinIO, RabbitMQ and our main OTA service.

## Consequences

Components in a microservices architecture can be scaled independently, changes to the overall system like adding new functionality, can be applied more easily, since components are only coupled loosely.
