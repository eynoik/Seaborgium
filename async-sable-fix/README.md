# Async Sable Fix 0.1.6.2

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.6.2

Adds protection for a separate Async/Lithium race in entity attribute synchronization. Lithium 0.15.4 replaces `AttributeMap` dirty sets with fastutil `ReferenceOpenHashSet`; concurrent Async entity work can mutate those sets while vanilla networking is iterating them to build `ClientboundUpdateAttributesPacket`.

The new `AttributeMapThreadSafetyMixin`:

- keeps normal entity ticks asynchronous;
- serializes only additions to `attributesToUpdate` / `attributesToSync`;
- returns identity-preserving snapshot sets from `getAttributesToSync()` and `getAttributesToUpdate()`;
- clears the live dirty set under the same lock so later vanilla `clear()` calls only touch the detached snapshot;
- avoids globally forcing all living entities onto the server thread.

All 0.1.6.1 protections remain unchanged:

- no per-block `Level#isLoaded` calls in Sable `LevelAccelerator` collision scans;
- loaded-only, per-chunk cached guards including negative results;
- no blocking chunk-load fallback from Async collision workers;
- hard 1024 integer-block scan cap plus the earlier 4096.0 volume guard;
- Create contraptions, MCA villagers and MineColonies citizens remain forced to synchronous ticking.

The main Seaborgium branch remains untouched. Work lives on dedicated Async Sable Fix branches under `async-sable-fix/`.
