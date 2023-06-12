# 32. Use OSTree with TreeHub to enable Delta Updates

Date: 2023-04-03

## Status

Accepted

## Context

Reducing the amount of data to be sent is very important to reduce response and completion times and by that the observed quality of car drivers and fleet administrators.

## Decision

Not used in our prototype.

## Consequences

The git-like structure of OSTree enables to only download files that have changed since the last commit. This greatly reduces the amount of data to be sent. For that a metadata file is used that contains the commit identifier. The car subsequently downloads the respective files to perform the update.
