# AsyncGuard 0.2.2

Compatibility and thread-safety guard pack for Minecraft 1.21.1 / NeoForge 21.1.x and Async 0.2.0 alpha.

AsyncGuard is the renamed successor to AsyncSableFix. The technical mod id remains `asyncsablefix` intentionally so an old AsyncSableFix/AsyncGuard JAR cannot silently coexist with a newer one and double-apply mixins.

## 0.2.2 — MCA village-tax unloaded-chunk watchdog

A real server watchdog from 2026-09-23 on MCA `7.7.36-beta.3+1.21.1` showed:

`VillageTaxesManager.tryToPutIntoInventory -> Level.getBlockState -> Level.getChunk -> ServerChunkCache/C2ME`

MCA's `deliverTaxes()` checks only whether the chunk containing the village **center** is loaded. It then iterates every recorded block position belonging to storage buildings. A storage block can be in a different chunk.

Before MCA reads block state for a storage position, AsyncGuard now performs the same strict loaded-only FULL-chunk lookup already used by its other non-blocking chunk protections. If that exact chunk is not already loaded, the storage position is skipped.

No tax item is deleted: MCA's tax items remain in `village.storageBuffer` and can be delivered on a later pass when the storage chunk is loaded.

This does not force-load chunks and does not change normal chest/container insertion when the target chunk is already loaded.

Runtime test: keep the village-center chunk loaded while at least one MCA storage-building chunk is unloaded, trigger/enter the village so tax delivery runs, and verify that the server does not enter `ServerChunkCache.getChunk` for the unloaded storage position. After loading that storage chunk, the buffered tax items should still be deliverable.

## 0.2.1 — Hundred Years Warfare + Curios/Relics races

- HYW 0.7.1r: serialize only `PathingTaskManagerRegistry.computeIfAbsent`; HYW entity AI remains asynchronous.
- Curios 9.5.1 + Relics 0.12.8: skip `CurioStacksHandler.update()` only on Async tick workers, leaving mutable slot recalculation to the normal server thread.

## 0.2.0 — PneumaticCraft target-tracking race

PneumaticCraft 8.2.23 target-tracking maps are protected against concurrent Async target-change events.

## Existing protections retained

- Sable collision/raycast guards;
- non-blocking collision/chunk lookup;
- Async/Lithium `AttributeMap` dirty-set protection;
- sync safety rules for Create contraptions, MCA villagers and MineColonies citizens;
- PneumaticCraft target tracking;
- Hundred Years Warfare pathing registry;
- Curios/Relics off-thread slot-update guard.

## Upgrade

Remove the previous AsyncGuard/AsyncSableFix JAR and install only `asyncguard-0.2.2.jar`.

Work lives on branch `asyncguard-0.2.2` under `async-guard/`.
