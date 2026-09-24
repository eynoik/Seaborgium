# AsyncGuard 0.2.1

Compatibility and thread-safety guard pack for Minecraft 1.21.1 / NeoForge 21.1.x and Async 0.2.0 alpha.

AsyncGuard is the renamed successor to AsyncSableFix. The public project/JAR name is general because the mod protects several Async incompatibilities. The technical mod id remains `asyncsablefix` intentionally so an old AsyncSableFix/AsyncGuard JAR cannot silently load together with a newer one and double-apply mixins.

## 0.2.1 — Hundred Years Warfare + Curios/Relics races

### Hundred Years Warfare 0.7.1r

A real server log from 2026-09-23 showed:

`ConcurrentModificationException -> HashMap.computeIfAbsent -> PathingTaskManagerRegistry.getTaskManager -> ReturnToHomeGoal`

The call came from an Async entity worker while HYW soldiers were ticking in parallel. AsyncGuard now serializes only the registry's `computeIfAbsent` operation. HYW AI and entity ticks remain asynchronous.

### Curios 9.5.1 + Relics 0.12.8

A second real log showed:

`ConcurrentModificationException -> CurioStacksHandler.update -> getStacks -> Relics EntityUtils.findEquippedCurios`

The damage event originated from an Iron's Spells mob tick running on an Async worker. Curios 9.5.1's `getStacks()` calls `update()`, and that method iterates a mutable Guava `HashMultimap` of slot modifiers.

AsyncGuard now cancels only `CurioStacksHandler.update()` when the caller is an `Async-Tick-Pool-Thread-*` worker. The worker reads the already-published Curios stack state; Curios' normal server-thread update path remains untouched and performs the mutable recalculation.

## 0.2.0 — PneumaticCraft target-tracking race

PneumaticCraft 8.2.23 keeps global target-tracking state in ordinary mutable maps. Async target changes were observed corrupting its fastutil `Int2IntOpenHashMap`, causing `ArrayIndexOutOfBoundsException` in `rehash()`.

0.2.0 made those tracking structures thread-safe without disabling Async entity ticking.

## Existing protections retained

- Sable `SubLevelEntityCollision.collide` critical section serialization;
- Async-worker-only Sable LevelAccelerator loaded-only guards;
- Mowzie/entity-tracking LOS/raycast watchdog protection;
- loaded-only block/fluid reads inside Async raycast scope;
- non-blocking collision chunk lookup;
- Async/Lithium `AttributeMap` dirty-set synchronization;
- Create contraptions, MCA villagers and MineColonies citizens forced to synchronous ticking where already required;
- PneumaticCraft 8.2.23 target-tracking map protection.

## Upgrade

Remove the previous AsyncGuard/AsyncSableFix JAR and install only `asyncguard-0.2.1.jar`.

Work lives on branch `asyncguard-0.2.1` under `async-guard/`.
