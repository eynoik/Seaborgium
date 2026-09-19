# Async Sable Fix 0.1.7

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.7

0.1.7 changes the strategy for Sable sub-level collision. Async remains enabled for normal entity ticking; only Sable's collision critical section is serialized.

The previous 0.1.6.x collision workaround lowered Sable's original huge-bounds guard from `500^3` to `4096` and capped `BlockPos.betweenClosed` scans at 1024 blocks. That could turn a transient/racy bad bound into a missing collision, which matches observed items falling through a stationary camper. It also modified `LevelAccelerator` reads on the normal server thread, which was unsafe for Sable/Simulated assembly and disassembly.

0.1.7 therefore:

- removes the custom 4096 collision-volume guard;
- removes the custom 1024 integer-block scan cap;
- restores Sable's original collision behavior and original large-bounds guard;
- serializes `SubLevelEntityCollision.collide` across callers with a reentrant lock, preventing concurrent Async entity movement from overlapping inside Sable's shared mutable collision scratch path;
- keeps the rest of each entity tick asynchronous;
- restricts the loaded-only `LevelAccelerator` AIR/empty/null guard to Async worker threads only;
- leaves normal server-thread Sable assembly/disassembly block access untouched.

All unrelated protections from 0.1.6.3 remain:

- Async-worker Sable/vanilla `BlockGetter#clip` scope guard for the reproduced Mowzie/entity-tracking LOS watchdog;
- loaded-only `Level#getBlockState` / `getFluidState` handling inside that Async raycast scope, with missing chunks treated as a solid boundary;
- non-blocking `Level#getChunkForCollisions` behavior on Async workers;
- Async/Lithium `AttributeMap` dirty-set synchronization;
- Create contraptions, MCA villagers and MineColonies citizens forced to synchronous ticking by the existing compatibility rules.

The main Seaborgium branch remains untouched. Work lives on dedicated Async Sable Fix branches under `async-sable-fix/`.
