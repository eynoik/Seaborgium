# Seaborgium

Seaborgium is a client-side optimization mod for Minecraft 1.21.1 on NeoForge.

## 0.1.0-alpha.14

Alpha.14 keeps every alpha.13 optimization and adds a shared worker pool plus four new multithreaded preparation paths.

### 1. Shared worker system

All new async preparation uses one configurable Seaborgium worker pool instead of creating unrelated executors.

Default:
- up to 6 workers,
- always leaves at least two logical processors outside the pool,
- client config: `multithreading.workers`.

OpenGL, model mutation and final draw calls remain on the render thread.

### 2. Living-entity pose input preparation

Living entities are snapshotted on the render thread and workers prepare immutable:
- motion deltas,
- body/head yaw deltas,
- pitch deltas,
- bounding dimensions.

The layer-budget projection path consumes completed snapshots from the previous/current tick where available.

This deliberately does **not** mutate `EntityModel` or `ModelPart` off-thread. Those model instances are shared by the renderer and are not generically thread-safe.

Config:
- `multithreading.entityPosePrep=true`

### 3. JEI async filtering/search preparation

When JEI invalidates its visible ingredient list after a text-filter change, Seaborgium can prepare the replacement list on the worker pool while the previous completed list remains visible for a few frames.

The compatibility path uses JEI's own private search/filter/sort implementation and only moves its execution off the render thread. It falls back to JEI's normal path when:
- the search index is being rebuilt,
- sort indexes are dirty,
- reflection compatibility does not match the installed JEI,
- an async request fails.

JEI already uses a parallel stream for the empty-filter path; alpha.14 mainly targets changed/non-empty searches which otherwise still contain synchronous preparation.

Config:
- `multithreading.jeiFilter=true`

### 4. Async tooltip stale-while-revalidate

Alpha.13 memoized repeated tooltip requests inside one client tick.

Alpha.14 additionally keeps the last completed tooltip briefly and returns it immediately while a copied `ItemStack` refreshes the next snapshot on a worker.

Properties:
- default max staleness: 1 client tick,
- Shift/Ctrl/Alt, screen class, item/count/components and tooltip mode are part of the key,
- worker refresh bypasses Seaborgium's own tooltip mixin to avoid recursion,
- item classes which throw during off-thread tooltip generation are runtime-blacklisted and fall back to the normal render-thread path.

Config:
- `multithreading.tooltipPrefetch=true`
- `multithreading.tooltipMaxStaleTicks=1`

This is experimental because third-party tooltip callbacks are not guaranteed to be thread-safe.

### 5. Weighted Create BlockEntity scheduler

The existing Create SmartBlockEntity async path now uses the shared worker pool and measures per-class tick cost with an EWMA.

Chunk groups are:
1. assigned estimated cost from the block entities they contain,
2. sorted heavy-first,
3. greedily distributed to the currently lightest worker bin.

Ticks inside one chunk preserve order. A barrier still completes before the block-entity phase ends. Classes that fail asynchronously are runtime-blacklisted and fall back to the render thread on following ticks.

Configs:
- `create_block_entities.async=true`
- `create_block_entities.threads=3`
- `create_block_entities.minBatch=8`

## Retained work

Alpha.14 retains:
- screen-space living-entity layer budgeting,
- layer and entity renderer telemetry,
- static/play benchmark commands,
- tighter Create Factory Panel render bounds,
- Create client block-entity parallel ticking,
- world render telemetry,
- same-tick ItemStack tooltip memoization.

## Testing

For this build test the modules separately if a regression appears:

1. JEI: type rapidly in the search field and switch filters/pages.
2. Tooltips: hover modded weapons/items, hold Shift/Ctrl/Alt and verify dynamic text.
3. Entities: move around MCA/Epic Fight/MineColonies entities and watch for animation/layer pop.
4. Create: test pumps, tanks, crafters, arms and Factory Panels.
5. Capture a client Spark with all modules enabled.

If a particular subsystem causes issues, disable only its config flag instead of removing Seaborgium.

## Build

Seaborgium targets Java 21, Minecraft 1.21.1 and NeoForge 21.1.248.

```bash
./gradlew build
```

The built JAR is written to `build/libs`.
