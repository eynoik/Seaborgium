# Seaborgium

Seaborgium is a client-side optimization mod for Minecraft 1.21.1 on NeoForge.

Its job is deliberately narrow: reduce CPU work which would otherwise land on Minecraft's render/client thread without trying to move unsafe OpenGL or world state operations to arbitrary worker threads.

## Current alpha

### 0.1.0-alpha.13

Alpha.13 keeps the existing entity-layer, Factory Panel and async Create work and adds a client tooltip memoizer.

Minecraft/NeoForge tooltip generation is normally requested from the UI every render frame. JEI and normal inventory screens ultimately call `ItemStack#getTooltipLines`, which fires the whole NeoForge item-tooltip event chain. In a large pack this can include Epic Fight, Apotheosis and other expensive listeners.

Alpha.13 caches the finished tooltip for repeated requests during the **same client tick**.

The cache key includes:
- player and client tick,
- item, stack count and data components,
- advanced/creative tooltip mode,
- Shift/Ctrl/Alt state,
- current screen class.

The returned list is copied on cache hits so callers can safely alter their own list. The cache is bounded and configurable under `ui_tooltips`.

This is intentionally memoization rather than "multithreaded GUI rendering": OpenGL draws, font/glyph atlas work and many mod tooltip callbacks are not thread-safe. Parallelizing them generically would trade frametime spikes for races/crashes. Pure or pack-specific preparation can still be moved to workers later when profiling identifies a safe target.

### Existing optimizations retained

- screen-space layer budgeting for living entities;
- optional compact telemetry HUD;
- per-layer cost models and bounded profiling/benchmark commands;
- projected entity size cached for a frame;
- tighter Create Factory Panel render bounds;
- experimental client-side Create SmartBlockEntity batching across chunk groups with a synchronization barrier;
- terrain/world render telemetry.

## Configuration

The new tooltip section is enabled by default:

- `ui_tooltips.memoize = true`
- `ui_tooltips.cacheEntries = 512`

If a mod has a tooltip that intentionally changes multiple times inside a single 50 ms client tick, disable the option and report the item/mod so it can receive a narrower compatibility path.

## Planned work

1. Measure alpha.13 with a client Spark while hovering JEI/inventory items.
2. Add cache hit/miss and render-thread frametime telemetry if the profile shows enough benefit.
3. Continue profiling Factory Panels / world rendering / shader state churn.
4. Keep parallel work limited to code proven thread-safe.

## Build

Seaborgium targets Java 21, Minecraft 1.21.1 and NeoForge 21.1.248.

```bash
./gradlew build
```

The built JAR is written to `build/libs`.
