# 4. Implement approach where cars frequently poll for new updates

Date: 2023-03-31

## Status

Accepted

## Context

To increase security by not offering any open ports at the car, it makes sense to use polling intervals to frequently check if new updates are available.

## Decision

Cars will poll in a predefined polling interval. For that the `newImageAvailable` endpoint with the car's ID is called frequently. If that returns true, the `getNewImage` endpoint is called with the car's ID and the image is received. 

## Consequences

Security off the update process is increased, also load can be controlled more easily, since the number of cars that are polling in a certain time interval can be controlled by the chosen polling interval.
