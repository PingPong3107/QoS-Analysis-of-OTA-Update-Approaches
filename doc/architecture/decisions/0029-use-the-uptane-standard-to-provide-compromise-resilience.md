# 29. Use the uptane standard to provide compromise resilience

Date: 2023-04-03

## Status

Accepted

## Context

When using mechanisms like TLS to protect the connection between server and cars, no security is provided if a set of keys or a server is compromised. For that Uptane helps in providing compromise resilience, even if a server or a subset of keys is compromised.

## Decision

Not included in our prototype (simplicity).

## Consequences

By providing metadata signing with a PKI and the use of at least two servers, where one provides on-demand signing and a second server that uses offline keys to sign metadata and images, and the use of full and partial verification on the device side, Uptane is able to provide compromise resilience. There are certain trade-offs between using full vs partial verification. Generally we think that a scheme like Uptane is not optional in automotive industry.
