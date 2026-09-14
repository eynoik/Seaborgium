# Async Sable Fix 0.1.5

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.5

Fixes the watchdog hang where an Async entity worker enters Sable collision code at an unloaded chunk boundary while the server thread is processing player-distance tickets (often exposed when another player joins).

0.1.4 used `ChunkSource#getChunkNow()` in `Level#getChunkForCollisions`. In Async 0.2.0 that call is only non-blocking when a visible `ChunkHolder` already exists. If it does not, vanilla falls through to `getChunk(..., FULL, false)`, which Async intercepts and waits on through the main-thread executor. During `callEntityTickBatch` this can deadlock with chunk-ticket processing / C2ME.

0.1.5 never calls `getChunkNow()` or `getChunk()` from an Async worker collision lookup. It mirrors Async's own fast path: `getVisibleChunkIfPresent(ChunkPos)` -> `ChunkHolder#getChunkIfPresent(FULL)`. If no loaded chunk is immediately available it returns `null`; there is no blocking fallback.

## Preserved 0.1.4 protections

- Create `AbstractContraptionEntity`, MCA `VillagerEntityMCA`, and MineColonies `AbstractEntityCitizen` remain forced to synchronous entity ticking.
- Sable `LevelAccelerator` still refuses reads/writes that would touch unloaded server chunks.
- Sable `SubLevelEntityCollision` pathological volume guard remains capped at `4096.0` instead of `1.25E8`.
- Async collision chunk lookup remains non-blocking; 0.1.5 replaces only the unsafe fallback behavior of the old implementation.

## Build

```bash
gradle build
```

The tested binary is also committed under `releases/asyncsablefix-0.1.5.jar`.
