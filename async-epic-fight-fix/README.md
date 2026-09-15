# Async Epic Fight Fix

Compatibility guard for Minecraft 1.21.1 / NeoForge 21.1.x that keeps Epic Fight 21.17.x and the tested addon set safe with Async 0.2.x without disabling Async globally.

## Why this exists

Async moves ordinary server entity ticks onto worker threads. Epic Fight runs its `EntityPatch` pre/post tick hooks inside the normal entity tick path, and addons can add additional living-entity tick state. Those paths were not written with arbitrary parallel entity ticking as a compatibility contract.

Version 0.1.0 injects only into `ParallelProcessor.shouldTickSynchronously` and forces main-thread ticking when:

- the entity actually owns an Epic Fight `EntityPatch`, or
- it carries a known active runtime marker from Weapons of Miracles, or
- it carries a known active persistent-data marker from TwilightForestEFCompat.

Ordinary unpatched entities are left to Async. Players and projectiles are already synchronous in Async itself.

## Tested / inspected addon set

The 0.1.0 compatibility policy was built against the exact supplied pack:

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
- Fail open if Epic Fight reflection is unavailable, so an optional-addon mismatch does not crash startup.
- Add future guards only for stack traces that prove a concrete unsafe path.

## Runtime marker coverage

Weapons of Miracles markers currently covered include anti-stunlock, timed katana slashes, lunar eclipse, solar ignition, blackout, ultimate invulnerability, health fix, serious focus, bow replacement and stronger-mob state.

TwilightForestEFCompat persistent state currently covered includes bokken off-balance and the ice-freeze lockout/damage/resolution keys found in the supplied JAR.

## Installation

Install alongside Async 0.2.x and Epic Fight 21.17.x on the server. Keep the normal Epic Fight/addon JARs unchanged.

## Status

0.1.0 has passed bytecode validation and a small mock classifier test. A full Minecraft runtime test on the real server pack is still required. If a watchdog/crash occurs, keep the complete crash report and `latest.log`; the next patch should target the demonstrated addon path rather than broadening synchronization blindly.
