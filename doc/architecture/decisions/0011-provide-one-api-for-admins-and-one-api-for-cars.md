# 11. Provide one API for admins and one API for cars

Date: 2023-04-01

## Status

Accepted

## Context

It makes sense to logically split the API into an admin and a device / car part. 

## Decision

We used that in the prototype. Admin API: ota-tester.com/admin/... Car API: ota-tester.com/car/...

## Consequences

The different URLs make it easier to distinguish between admin and car requests.
