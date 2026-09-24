# Changelog

## 0.2.0 — AsyncGuard rename + PneumaticCraft thread-safety

- Renamed the public project/JAR from AsyncSableFix to **AsyncGuard** because the mod now carries general Async compatibility protections.
- Moved the module directory from `async-sable-fix/` to `async-guard/`.
- Kept the technical NeoForge mod id `asyncsablefix` intentionally so old and new JARs cannot silently coexist and double-apply the same mixins.
- Added `PneumaticArmorHandlerThreadSafetyMixin` for PneumaticCraft 8.2.23.
- Reproduced failure: concurrent Async entity target changes corrupt PneumaticCraft's static fastutil target map, causing `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 257` in `Int2IntOpenHashMap.rehash()`.
- Wrapped PneumaticCraft `targetingTracker` with fastutil's synchronized map wrapper.
- Replaced the warning aggregation map with concurrent outer and per-player inner maps so `computeIfAbsent`, `merge`, `forEach` and `clear` can safely overlap across Async workers and the main server thread.
- Entity ticking stays asynchronous; the fix does not blacklist all PneumaticCraft mobs or disable Async globally.
- Preserved all 0.1.7 and earlier Sable, chunk/raycast, AttributeMap/Lithium and entity-family compatibility protections.

## 0.1.7

- Reworked Sable collision compatibility after real vehicle testing exposed missing floor collision, broken seat/typewriter tracking, repeated `Enormous local sub-level collision bounds` spam and destructive disassembly symptoms on a stationary camper.
- Removed the 0.1.6 custom `4096.0` collision-volume guard.
- Removed the 0.1.6 hard `1024` `BlockPos.betweenClosed` scan cap.
- Restored Sable 2.0.5's original collision scan and original `500^3` huge-bounds guard.
- Serialized only `SubLevelEntityCollision.collide` with a reentrant critical-section lock. Async remains enabled for the rest of entity ticking.
- This prevents multiple Async entity movement calls from overlapping inside Sable's shared mutable collision scratch path, which can corrupt local bounds and sub-level tracking.
- Restricted `SableLevelAcceleratorMixin` loaded-only AIR/empty/null behavior to Async tick workers. Normal server-thread assembly/disassembly now uses unmodified Sable `LevelAccelerator` behavior.
- Preserved the 0.1.6.3 raycast/LOS watchdog fix, 0.1.6.2 AttributeMap race fix, 0.1.5 non-blocking collision chunk lookup, and existing synchronous safety rules for Create contraptions, MCA villagers and MineColonies citizens.

## 0.1.6.3

- Fixed the reproduced Async/Sable watchdog deadlock in entity tracking / line-of-sight.
- The failing path was `ChunkMap$TrackedEntity.updatePlayer` -> Mowzie boss tracking -> `LivingEntity.hasLineOfSight` -> Sable `BlockGetter#clip` -> `Level#getFluidState` -> Async blocking `ServerChunkCache#getChunk`.
- Added an Async-worker raycast scope guard around the Sable-overwritten `BlockGetter#clip` path.
- During that scope, `Level#getBlockState` / `getFluidState` read directly from already-loaded chunks only.
- Missing raycast chunks are treated as a solid boundary (`BEDROCK` + empty fluid), so LOS fails closed and the traversal stops instead of scheduling a synchronous chunk load while Async holds entity-tracker locks.
- Preserved every 0.1.6.2 and earlier protection unchanged.

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
