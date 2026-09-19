# AsyncDeadlockFix 0.1.1

## 0.1.1 hotfix

0.1.0 targeted the correct deadlock but used the field name from a later Async branch:
`ParallelProcessor.executor`. The installed Async 0.2.0+alpha-1.21.1 actually exposes
`ParallelProcessor.tickPool`. The 0.1.0 injection therefore threw from
`setupThreadPool()` after the original pool had been created, terminating the Minecraft server
thread. The subsequent watchdog report contained no `Server thread` at all and all 16
`Async-Tick-Pool-Thread-*` workers were idle, which is consistent with that startup-thread failure.

0.1.1 resolves `tickPool` first and `executor` only as a forward-compatible fallback. Installation
errors now fail soft and leave Async's original pool alive instead of killing the server.

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
