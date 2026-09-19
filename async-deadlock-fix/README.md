# AsyncDeadlockFix 0.1.0

Targeted fix for the server hang captured on 2026-09-19 with Async 0.2.0+alpha on NeoForge 1.21.1.

## Measured deadlock

The watchdog showed the server thread blocked in:

`ServerLevel.redirect$...$async$overwriteEntityTicking -> AbstractExecutorService.invokeAll -> FutureTask.get`

At the same time every `Async-Tick-Pool-Thread-*` was inside natural spawning and waiting in Async's
`ServerChunkCache#getChunk` path for chunk `[-204,-132]` at `minecraft:structure_starts`.
Those chunk requests were queued to the main-thread chunk processor, but the main server thread was parked
inside `invokeAll`, so neither side could make progress.

## Fix

This mod leaves Async spawning and entity ticking enabled. Immediately after Async creates its executor,
the executor is replaced with a configuration-equivalent `PumpingThreadPoolExecutor` using Async's own
thread factory. Only `invokeAll` called from the real Minecraft server thread changes behavior: while
waiting for Async futures it polls pending chunk-source tasks for every server level. Other executor behavior
and non-server-thread `invokeAll` calls retain normal JDK behavior.

This mirrors the direction taken by later Async 1.21.1 code, which removed the blocking `invokeAll` wait
and explicitly pumps chunk tasks while waiting for parallel batches.
