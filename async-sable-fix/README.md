# Async Sable Fix 0.1.6.3

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.6.3

Fixes a separate reproduced watchdog deadlock in entity tracking and line-of-sight. An Async natural-spawn worker could enter `ChunkMap$TrackedEntity.updatePlayer`, hold Async's tracker locks, then Mowzie's Mobs boss tracking called `LivingEntity.hasLineOfSight`. Sable's overwritten `BlockGetter#clip` read `Level#getFluidState` in an unloaded/projected chunk, which fell into Async's blocking `ServerChunkCache#getChunk` handoff. The server thread then blocked waiting for the same tracked-entity lock.

The new raycast guard:

- marks Sable/vanilla `BlockGetter#clip` only while it executes on an Async tick worker;
- inside that scope, `Level#getBlockState` and `getFluidState` use the existing loaded-only ChunkHolder lookup;
- loaded chunks are read directly without entering `ServerChunkCache#getChunk`;
- a missing chunk is treated as a solid boundary (`BEDROCK` + empty fluid), so line-of-sight fails closed and traversal stops instead of generating/loading the chunk;
- does not disable Async, Mowzie's Mobs, entity tracking, or normal main-thread raycasts.

## 0.1.6.2

Protects the separate Async/Lithium race in entity attribute synchronization. Lithium 0.15.4 replaces `AttributeMap` dirty sets with fastutil `ReferenceOpenHashSet`; concurrent Async entity work can mutate those sets while vanilla networking is iterating them to build `ClientboundUpdateAttributesPacket`.

`AttributeMapThreadSafetyMixin` keeps normal entity ticks asynchronous, serializes only dirty-set bookkeeping, returns snapshot sets, and clears the live dirty set under the same lock.

All earlier protections remain unchanged:

- no per-block `Level#isLoaded` calls in Sable `LevelAccelerator` collision scans;
- loaded-only, per-chunk cached guards including negative results;
- no blocking chunk-load fallback from Async collision workers;
- hard 1024 integer-block scan cap plus the earlier 4096.0 volume guard;
- Create contraptions, MCA villagers and MineColonies citizens remain forced to synchronous ticking.

The main Seaborgium branch remains untouched. Work lives on dedicated Async Sable Fix branches under `async-sable-fix/`.
