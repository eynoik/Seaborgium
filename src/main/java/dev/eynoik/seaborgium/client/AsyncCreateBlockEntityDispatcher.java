package dev.eynoik.seaborgium.client;

import com.mojang.logging.LogUtils;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batches Create SmartBlockEntity ticks during the normal client block-entity
 * phase and executes different chunks in parallel. Ticks inside one chunk keep
 * their vanilla order. A barrier at the end of Level.tickBlockEntities() makes
 * the optimization invisible to the following client tick/render phases.
 */
public final class AsyncCreateBlockEntityDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WORKERS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ThreadLocal<Boolean> WORKER_EXECUTION = ThreadLocal.withInitial(() -> false);
    private static final List<TickTask> PENDING = new ArrayList<>();
    private static final Set<String> RUNTIME_BLACKLIST = ConcurrentHashMap.newKeySet();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(MAX_WORKERS, new WorkerFactory());

    private AsyncCreateBlockEntityDispatcher() {
    }

    public static boolean isWorkerExecution() {
        return WORKER_EXECUTION.get();
    }

    public static boolean enqueue(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos,
                                  BlockState state, BlockEntity blockEntity) {
        if (!SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITIES.get()) {
            return false;
        }
        if (isWorkerExecution()) {
            return false;
        }
        if (RUNTIME_BLACKLIST.contains(blockEntity.getClass().getName())) {
            return false;
        }

        PENDING.add(new TickTask(ticker, level, pos.immutable(), state, blockEntity));
        return true;
    }

    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }

        List<TickTask> tasks = new ArrayList<>(PENDING);
        PENDING.clear();

        int minBatch = SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITY_MIN_BATCH.get();
        if (!SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITIES.get() || tasks.size() < minBatch) {
            runInline(tasks);
            return;
        }

        Map<Long, List<TickTask>> chunkGroups = new LinkedHashMap<>();
        for (TickTask task : tasks) {
            int chunkX = task.pos().getX() >> 4;
            int chunkZ = task.pos().getZ() >> 4;
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            chunkGroups.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(task);
        }

        List<List<TickTask>> groups = new ArrayList<>(chunkGroups.values());
        int configuredWorkers = Math.min(MAX_WORKERS, SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITY_THREADS.get());
        int workerCount = Math.min(configuredWorkers, groups.size());
        if (workerCount <= 1) {
            runInline(tasks);
            return;
        }

        AtomicInteger nextGroup = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(workerCount);
        for (int worker = 0; worker < workerCount; worker++) {
            EXECUTOR.execute(() -> {
                WORKER_EXECUTION.set(true);
                try {
                    int index;
                    while ((index = nextGroup.getAndIncrement()) < groups.size()) {
                        for (TickTask task : groups.get(index)) {
                            runTask(task);
                        }
                    }
                } finally {
                    WORKER_EXECUTION.remove();
                    done.countDown();
                }
            });
        }

        boolean interrupted = false;
        while (true) {
            try {
                done.await();
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runInline(List<TickTask> tasks) {
        boolean previous = WORKER_EXECUTION.get();
        WORKER_EXECUTION.set(true);
        try {
            for (TickTask task : tasks) {
                runTask(task);
            }
        } finally {
            if (previous) {
                WORKER_EXECUTION.set(true);
            } else {
                WORKER_EXECUTION.remove();
            }
        }
    }

    private static void runTask(TickTask task) {
        String className = task.blockEntity().getClass().getName();
        if (RUNTIME_BLACKLIST.contains(className) && !Thread.currentThread().getName().startsWith("Seaborgium-CreateBE-")) {
            task.ticker().tick(task.level(), task.pos(), task.state(), task.blockEntity());
            return;
        }

        try {
            task.ticker().tick(task.level(), task.pos(), task.state(), task.blockEntity());
        } catch (Throwable throwable) {
            if (RUNTIME_BLACKLIST.add(className)) {
                LOGGER.error("Seaborgium disabled async Create block entity ticking for {} after a worker failure. The failed client tick was skipped; following ticks will use the render thread.", className, throwable);
            }
        }
    }

    private record TickTask(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos,
                            BlockState state, BlockEntity blockEntity) {
    }

    private static final class WorkerFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Seaborgium-CreateBE-" + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        }
    }
}
