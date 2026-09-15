# Changelog

## 0.1.0 - 2026-09-15

- Added Async `ParallelProcessor.shouldTickSynchronously` compatibility guard for Epic Fight.
- Synchronizes only entities that currently expose an Epic Fight `EntityPatch`.
- Added narrow Weapons of Miracles runtime-tag fallback for non-patched living targets.
- Added narrow TwilightForestEFCompat persistent-data fallback for off-balance/ice-freeze targets.
- Kept ordinary non-patched entities asynchronous.
- Avoided direct compile/runtime links to optional addon classes by using a reflection-only bridge.
- Verified correct runtime-invisible `@Mixin` annotation retention.
- Passed mock classification tests for plain, Epic Fight-patched, WOM-marked and Twilight-marked entities.
