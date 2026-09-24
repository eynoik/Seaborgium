# Changelog

## 0.2.2 — MCA village-tax unloaded-chunk watchdog

- Added `McaVillageTaxesChunkGuardMixin` for MCA `7.7.36-beta.3+1.21.1`.
- Reproduced watchdog path: `VillageTaxesManager.tryToPutIntoInventory -> Level.getBlockState -> Level.getChunk -> ServerChunkCache/C2ME`.
- MCA only verifies that the village-center chunk is loaded before iterating all recorded storage-building block positions.
- Before `tryToPutIntoInventory` reads block state, AsyncGuard now checks the exact target chunk through its strict loaded-only FULL-chunk lookup.
- If the target storage chunk is absent, that position is skipped instead of synchronously loading it on the main server thread.
- Tax items remain in MCA's `storageBuffer` and are therefore deferred, not discarded.
- Loaded storage chunks retain MCA's original block-state, chest and inventory behavior.
- Preserved all 0.2.1 and older HYW, Curios/Relics, PneumaticCraft, Sable, chunk/raycast, AttributeMap/Lithium and entity-family protections.

## 0.2.1 — Hundred Years Warfare + Curios/Relics Async races

- Added `HundredYearsWarPathingRegistryMixin` for Hundred Years Warfare 0.7.1r.
- Reproduced log path: `ConcurrentModificationException -> HashMap.computeIfAbsent -> PathingTaskManagerRegistry.getTaskManager -> ReturnToHomeGoal` on an Async entity worker.
- Serialize only the registry `computeIfAbsent` call; HYW soldiers and their AI remain asynchronously ticked.
- Added `CuriosAsyncWorkerUpdateGuardMixin` for Curios 9.5.1+1.21.1.
- Reproduced log path: `ConcurrentModificationException -> CurioStacksHandler.update -> getStacks -> Relics EntityUtils.findEquippedCurios`.
- On Async tick workers only, `CurioStacksHandler.update()` is deferred instead of iterating/mutating Curios' shared slot-modifier state off-thread.
- Normal server-thread Curios updates are unchanged.

## 0.2.0 — AsyncGuard rename + PneumaticCraft thread-safety

- Renamed the public project/JAR from AsyncSableFix to **AsyncGuard**.
- Added PneumaticCraft 8.2.23 target-tracking thread-safety.
- Preserved all 0.1.7 and earlier Sable, chunk/raycast, AttributeMap/Lithium and entity-family compatibility protections.

## 0.1.7

- Reworked Sable collision compatibility after real vehicle testing.
- Serialized only `SubLevelEntityCollision.collide` with a reentrant critical-section lock.
- Restricted `SableLevelAcceleratorMixin` loaded-only behavior to Async tick workers.
- Preserved earlier raycast/LOS, AttributeMap and non-blocking chunk protections.
