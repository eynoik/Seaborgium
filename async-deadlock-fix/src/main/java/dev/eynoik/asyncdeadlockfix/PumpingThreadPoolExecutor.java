package dev.eynoik.asyncdeadlockfix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * ThreadPoolExecutor compatible with Async's existing executor, except invokeAll() does not park
 * the Minecraft server thread while Async workers are waiting for main-thread chunk work.
 *
 * The 0.2.0-alpha Async path does:
 *   Server thread -> executor.invokeAll(despawn tasks)
 * while async spawning can do:
 *   Async worker -> ServerChunkCache#getChunk -> mainThreadProcessor -> wait
 *
 * If every worker reaches the second path, vanilla AbstractExecutorService.invokeAll() parks the
 * server thread and no one services mainThreadProcessor. This executor preserves the same tasks and
 * pool, but pumps ServerChunkCache tasks while the server thread waits.
 */
public final class PumpingThreadPoolExecutor extends ThreadPoolExecutor {
    private static final long IDLE_PARK_NANOS = 100_000L;

    public PumpingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        Objects.requireNonNull(tasks);

        // Only alter the dangerous call when it is made by the actual Minecraft server thread.
        // Any other caller retains exact JDK ThreadPoolExecutor behavior.
        if (!isMinecraftServerThread()) {
            return super.invokeAll(tasks);
        }

        List<Future<T>> futures = submitAll(tasks);
        boolean completedNormally = false;
        try {
            waitForAll(futures, Long.MAX_VALUE);
            completedNormally = true;
            return futures;
        } finally {
            if (!completedNormally) {
                cancelAll(futures);
            }
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks,
            long timeout,
            TimeUnit unit
    ) throws InterruptedException {
        Objects.requireNonNull(tasks);
        Objects.requireNonNull(unit);

        if (!isMinecraftServerThread()) {
            return super.invokeAll(tasks, timeout, unit);
        }

        long timeoutNanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + timeoutNanos;
        List<Future<T>> futures = submitAll(tasks);
        boolean completedNormally = false;
        try {
            waitForAll(futures, deadline);
            completedNormally = true;
            return futures;
        } finally {
            if (!completedNormally || !allDone(futures)) {
                cancelUnfinished(futures);
            }
        }
    }

    private static boolean isMinecraftServerThread() {
        // This call also performs the authoritative MinecraftServer#isSameThread check.
        // pumpOnceIfServerThread() is allowed to execute one queued chunk task; doing that before
        // submission is harmless and avoids relying on a fragile thread-name check.
        return ServerThreadProbe.isServerThread();
    }

    private <T> List<Future<T>> submitAll(Collection<? extends Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> task : tasks) {
                futures.add(submit(Objects.requireNonNull(task)));
            }
            return futures;
        } catch (RuntimeException | Error throwable) {
            cancelAll(futures);
            throw throwable;
        }
    }

    private static void waitForAll(List<? extends Future<?>> futures, long deadline)
            throws InterruptedException {
        while (!allDone(futures)) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (System.nanoTime() >= deadline) {
                return;
            }

            boolean pumped = ServerTaskPump.pumpOnceIfServerThread();
            if (!pumped) {
                LockSupport.parkNanos(IDLE_PARK_NANOS);
            }
        }
    }

    private static boolean allDone(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    private static void cancelAll(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }

    private static void cancelUnfinished(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }
}
