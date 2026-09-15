# Async Sable Fix 0.1.6

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.6

Fixes the server-thread stalls seen after 0.1.5 in Sable `SubLevelEntityCollision`, especially with synchronous MineColonies visitors/citizens.

- Replaces the per-block `Level#isLoaded` check in the `LevelAccelerator` guard with the existing loaded-only `LoadedChunkLookup`.
- Caches the guard result per chunk for the lifetime of each Sable `LevelAccelerator`, including negative results. A collision scan touching hundreds of blocks in one chunk now performs one loaded-chunk lookup instead of hundreds.
- Keeps the unloaded-chunk protection: missing chunks still return AIR/empty/null and Async writes into unloaded chunks are still refused.
- Keeps the 0.1.5 non-blocking collision-chunk lookup that fixed the Async worker -> main-thread chunk-load deadlock.
- Keeps the 4096.0 collision-volume guard from 0.1.4.
- Adds a hard 1024 integer-block scan cap around Sable's `BlockPos.betweenClosed` call. Pathological long/thin bounds are treated like Sable's existing enormous-bounds bailout instead of iterating for seconds.
- Create contraptions, MCA villagers and MineColonies citizens remain forced to synchronous entity ticking.

The main Seaborgium branch is not modified; this lives only on the `asyncsablefix-0.1.6` branch under `async-sable-fix/`.
