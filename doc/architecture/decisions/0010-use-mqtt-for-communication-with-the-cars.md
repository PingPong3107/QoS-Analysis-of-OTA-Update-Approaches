# 10. Use MQTT for communication with the cars

Date: 2023-04-01

## Status

Accepted

## Context

MQTT is lightweight and well-supported, which makes it a good choice in many IoT tasks, also for OTA updates.

## Decision

Not used in our prototype (We use AMQP)

## Consequences

More lightweight than HTTP, which makes it more suitable for resource constrained ECU types. Downside: Not as widely supported as HTTP.
