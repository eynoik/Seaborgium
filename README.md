# Seaborgium

Seaborgium is a client-side rendering optimization mod for Minecraft 1.21.1 on NeoForge.

Its job is deliberately narrow: do not spend a large part of a frame rendering secondary entity layers that occupy only a tiny number of pixels on screen. Sodium, EntityCulling and ImmediatelyFast optimize different parts of the renderer; Seaborgium is intended to complement them.

## Current alpha

The first implementation adds screen-space layer budgeting for living entities:

- estimates projected pixel area from entity bounds, camera distance, FOV and viewport height;
- always keeps the base entity model;
- keeps all layers on the camera entity;
- progressively removes cosmetic, non-essential and finally all secondary layers as projected size shrinks;
- exposes thresholds and layer class-name keywords in the NeoForge client config.
- adds an optional compact telemetry HUD with rendered/skipped counts and sampled timings for expensive layer classes. Bind its toggle under Controls -> Seaborgium.

This is an early alpha. Defaults are intentionally conservative and need profiling in real modpacks before a public release.

## Planned work

1. In-game statistics and a repeatable benchmark scene.
2. Per-layer CPU timing with a low-overhead sampling profiler.
3. Dynamic frame-budget pressure instead of static thresholds alone.
4. Shadow and glint budgeting.
5. Compatibility tests with Sodium, ImmediatelyFast, EntityCulling, Iris/Sable, GeckoLib, Create/Flywheel and Accelerated Rendering.

## Build

Seaborgium targets Java 21, Minecraft 1.21.1 and NeoForge 21.1.248.

```bash
./gradlew build
```

The built JAR is written to `build/libs`.
