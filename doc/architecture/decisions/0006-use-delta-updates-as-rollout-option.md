# 6. Use Delta Updates as rollout option

Date: 2023-03-31

## Status

Accepted

## Context

Reducing the amount of data to be sent is an important issue, since it reduces update completion times as well as response times when cars try to pull a new image. 

## Decision

We emulate a delta update by waiting a certain predefined time. A random image is created with 1-DeltaSimilarity the size of the old image. This emulates the reduction in update data that can be achieved in the best case delta algorithm. In our simulations we used a delta simulation time of 0, since the time needed for the delta creation can just be added to the update completion time of the fleet, when only one delta image needs to be created for the whole fleet.

## Consequences

Delta updates reduce the overall amount of data to be sent. In our setup delta updates only work when the whole car fleet has the same update state before the rollout.
