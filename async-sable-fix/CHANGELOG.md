# Changelog

## 0.1.6.2

- Fixed a new Async/Lithium entity-attribute race observed in `ClientboundUpdateAttributesPacket` / `ReferenceOpenHashSet$SetIterator.next`.
- Added `AttributeMapThreadSafetyMixin`.
- Serializes only `AttributeMap` dirty-set bookkeeping; entity ticking remains asynchronous.
- `getAttributesToSync()` and `getAttributesToUpdate()` now return identity-preserving snapshots and clear the live dirty sets while holding the same per-AttributeMap lock.
- Preserved every 0.1.6.1 Sable/chunk/collision protection unchanged.

## 0.1.6.1

- Startup hotfix for the 0.1.6 binary.
- Corrected class-level Mixin annotation retention for the two classes rebuilt in 0.1.6 (`SableLevelAcceleratorMixin` and `SableSubLevelCollisionMixin`).
- No collision/cache behavior changes relative to 0.1.6.
- All 0.1.5 and earlier protections remain intact.

## 0.1.6

- Removed `Level#isLoaded` from the Sable LevelAccelerator hot path.
- Reused the 0.1.5 loaded-only ChunkHolder lookup and cached its result per chunk.
- Added negative per-chunk caching for unloaded chunks.
- Added a hard 1024-block cap for Sable `BlockPos.betweenClosed` collision scans.
- Preserved all 0.1.5 and earlier compatibility protections.

## 0.1.5

- Fixed Async/Sable server hang in `Level#getChunkForCollisions` when a collision lookup crosses into a chunk with no visible `ChunkHolder`.
- Replaced `ChunkSource#getChunkNow()` with a strict loaded-only lookup matching Async's `async$tryGetChunk` fast path.
- Explicitly returns no collision chunk when the holder/chunk is absent instead of entering Async's blocking `ServerChunkCache#getChunk` handoff.
- Preserved every 0.1.4 protection unchanged.
