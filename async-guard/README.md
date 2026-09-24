# AsyncGuard 0.2.0

Compatibility and thread-safety guard pack for Minecraft 1.21.1 / NeoForge 21.1.x and Async 0.2.0 alpha.

AsyncGuard is the renamed successor to AsyncSableFix. The public project/JAR name is now general because the mod already protects more than Sable. The technical mod id remains `asyncsablefix` intentionally so an old AsyncSableFix JAR and AsyncGuard cannot silently load together and apply duplicate mixins.

## 0.2.0 — PneumaticCraft target-tracking race

A real server crash on 2026-09-24 exposed a second class of Async incompatibility unrelated to the existing chunk deadlock fixes.

PneumaticCraft 8.2.23 keeps global target-tracking state in:

- a fastutil `Int2IntOpenHashMap` used by `PneumaticArmorHandler.onMobTargetSet()`;
- nested ordinary `HashMap` instances used for entity-tracker warning aggregation.

Async can tick many mobs simultaneously. Their target changes can therefore fire `LivingChangeTargetEvent` from several `Async-Tick-Pool-Thread-*` workers at once. The same `Int2IntOpenHashMap` was observed throwing `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 257` inside `rehash()` on many Async workers, and later on the main server thread.

0.2.0 keeps those entity ticks asynchronous. It only replaces PneumaticCraft's shared tracking state after class initialization:

- `targetingTracker` is wrapped with fastutil's synchronized `Int2IntMap` wrapper;
- `targetWarnings` becomes a concurrent outer map whose per-player warning maps are also concurrent.

This removes concurrent structural mutation without synchronizing whole mobs, MineColonies, Hundred Years War, EnhancedAI, Mob Grinding Utils or the entire PneumaticCraft event bus.

## Existing protections retained from 0.1.7

- Sable `SubLevelEntityCollision.collide` critical section serialization;
- Async-worker-only Sable LevelAccelerator loaded-only guards;
- Mowzie/entity-tracking LOS/raycast watchdog protection;
- loaded-only block/fluid reads inside Async raycast scope;
- non-blocking collision chunk lookup;
- Async/Lithium `AttributeMap` dirty-set synchronization;
- Create contraptions, MCA villagers and MineColonies citizens forced to synchronous ticking where already required.

## Upgrade

Remove the old `asyncsablefix-0.1.7.jar` and install only the new `asyncguard-0.2.0.jar`.

Do not keep both. The retained legacy mod id is designed to make NeoForge reject that mistake instead of running duplicate compatibility mixins.

Work lives on branch `asyncguard-0.2.0` under `async-guard/`.
