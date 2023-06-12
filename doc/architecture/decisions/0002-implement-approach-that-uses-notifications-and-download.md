# 2. Implement approach that uses notifications and download

Date: 2023-03-29

## Status

Accepted

## Context

This approach is used in Edgehog, AWS IoT OTA library and Thingsboard. It is an approach that is typically used in IoT management. That is why it makes sense to implement it in our prototype.

## Decision

We decided to use a broker for the notifications to enable a quick and simple way of notifying the clients about a new update. Cars download the update image from our REST-API after receiving the notification. 

## Consequences

We have an approach that enables notification and download. Impact will be high load when all cars start the update automatically.
