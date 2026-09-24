# Changelog

## 0.2.1 — Hundred Years Warfare + Curios/Relics Async races

- Added `HundredYearsWarPathingRegistryMixin` for Hundred Years Warfare 0.7.1r.
- Reproduced log path: `ConcurrentModificationException -> HashMap.computeIfAbsent -> PathingTaskManagerRegistry.getTaskManager -> ReturnToHomeGoal` on an Async entity worker.
- Serialize only the registry `computeIfAbsent` call; HYW soldiers and their AI remain asynchronously ticked.
- Added `CuriosAsyncWorkerUpdateGuardMixin` for Curios 9.5.1+1.21.1.
- Reproduced log path: `ConcurrentModificationException -> CurioStacksHandler.update -> getStacks -> Relics EntityUtils.findEquippedCurios`, originating from an Iron's Spells mob damage event on an Async worker.
- On Async tick workers only, `CurioStacksHandler.update()` is deferred instead of iterating/mutating Curios' shared slot-modifier state off-thread.
- Normal server-thread Curios updates are unchanged; the worker sees the already-published stack-handler state until the regular Curios update runs.
- Preserved all AsyncGuard 0.2.0 and older Sable, chunk/raycast, AttributeMap/Lithium, entity-family and PneumaticCraft protections.

## 0.2.0 — AsyncGuard rename + PneumaticCraft thread-safety

- Renamed the public project/JAR from AsyncSableFix to **AsyncGuard** because the mod now carries general Async compatibility protections.
- Moved the module directory from `async-sable-fix/` to `async-guard/`.
- Kept the technical NeoForge mod id `asyncsablefix` intentionally so old and new JARs cannot silently coexist and double-apply the same mixins.
- Added `PneumaticArmorHandlerThreadSafetyMixin` for PneumaticCraft 8.2.23.
- Reproduced failure: concurrent Async entity target changes corrupt PneumaticCraft's static fastutil target map, causing `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 257` in `Int2IntOpenHashMap.rehash()`.
- Wrapped PneumaticCraft `targetingTracker` with fastutil's synchronized map wrapper.
- Replaced the warning aggregation map with concurrent outer and per-player inner maps.
- Entity ticking stays asynchronous; the fix does not blacklist all PneumaticCraft mobs or disable Async globally.

## 0.1.7

- Reworked Sable collision compatibility after real vehicle testing exposed missing floor collision, broken seat/typewriter tracking, repeated `Enormous local sub-level collision bounds` spam and destructive disassembly symptoms on a stationary camper.
- Removed the 0.1.6 custom `4096.0` collision-volume guard.
- Removed the 0.1.6 hard `1024` `BlockPos.betweenClosed` scan cap.
- Restored Sable 2.0.5's original collision scan and original `500^3` huge-bounds guard.
- Serialized only `SubLevelEntityCollision.collide` with a reentrant critical-section lock.
- Restricted `SableLevelAcceleratorMixin` loaded-only AIR/empty/null behavior to Async tick workers.
- Preserved the 0.1.6.3 raycast/LOS watchdog fix, 0.1.6.2 AttributeMap race fix, 0.1.5 non-blocking collision chunk lookup, and existing synchronous safety rules for Create contraptions, MCA villagers and MineColonies citizens.
