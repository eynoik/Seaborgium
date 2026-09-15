# Async Epic Fight Fix

Compatibility guard for Minecraft 1.21.1 / NeoForge 21.1.x that keeps Epic Fight 21.17.x and the tested addon set safe with Async 0.2.x without disabling Async globally.

## 0.1.1

In addition to the 0.1.0 entity-tick policy, 0.1.1 fixes the reproduced player-login/tick crash in Epic Fight 21.17.3.1 where `MobEffectEvent.Expired` can arrive with a null `MobEffectInstance` after Async's status-effect race guard handles an effect that vanished during ticking. Epic Fight's `VanillaEntityEventHooks.onMobEffectExpired` dereferenced that null value.

0.1.1 cancels only Epic Fight's invalid expired-effect callback when the instance is null. Other valid effect-expiry events and normal effect behavior are unchanged.

## Entity tick policy

Async moves ordinary server entity ticks onto worker threads. Epic Fight runs its `EntityPatch` pre/post tick hooks inside the normal entity tick path, and addons can add additional living-entity tick state. Version 0.1.0/0.1.1 forces main-thread ticking only when:

- the entity actually owns an Epic Fight `EntityPatch`, or
- it carries a known active runtime marker from Weapons of Miracles, or
- it carries a known active persistent-data marker from TwilightForestEFCompat.

Ordinary unpatched entities are left to Async. Players and projectiles are already synchronous in Async itself.

## Tested / inspected addon set

- Epic Fight 21.17.3.1
- EFIS Compat 3.1.0
- EF Weapons Compat 1.0.0
- Epic Fight Compat 1.1.0
- Epic Fight First Person 1.0 / EF 21.16.4 line
- Epic Fight x MineColonies 1.0.0
- P1nero Bow 21.16.1.0
- TwilightForestEFCompat 1.1.6-Fix
- Weapons of Miracles 2.0.178

The general Epic Fight attachment check also covers addon-provided mob patches without hard-linking this mod to each addon JAR.

## Design rules

- Do not synchronize every `LivingEntity`.
- Do not touch `ServerChunkCache`.
- Do not replace Async's scheduler.
- Keep compatibility guards narrow and tied to reproduced stack traces.

## Installation

Install alongside Async 0.2.x and Epic Fight 21.17.x. Replace 0.1.0 with 0.1.1; do not keep both versions in the mods folder.
