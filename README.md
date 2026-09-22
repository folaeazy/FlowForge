# FlowForge

FlowForge is a backend processing platform focused on building core infrastructure components from scratch.

The goal is to explore how systems handle:

* rate limiting
* asynchronous processing
* concurrency
* idempotency

## Current Progress

* Day 1: Starting with a Token Bucket Rate Limiter (pure Java)

## Planned Components

* Rate Limiter (Token Bucket) - Redis with Lua script
* Job Queue + Worker System
* Event-driven processing pipeline
* Idempotency handling
* Observability (logging + metrics)

This project is being built incrementally with a focus on production-level design and tradeoffs.

## New Addition & Operation

For  Url Generating Simulation Engine
* Pull from the shared pool (creates a duplicate — could be same identity duplicating itself, or a different identity hitting the same URL someone else already submitted)
* Generate a fresh, genuinely unique URL (never seen before in this run)
* Generate a malformed URL (missing protocol, invalid characters, empty string)
### Example: 80% fresh unique, 15% pulled from shared pool, 5% malformed — applied independently to every single request regardless of which identity category (unique/recurring/burst) is firing it.

