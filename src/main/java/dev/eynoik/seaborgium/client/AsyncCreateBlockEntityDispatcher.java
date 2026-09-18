package dev.eynoik.seaborgium.client;

import com.mojang.logging.LogUtils;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public final class AsyncCreateBlockEntityDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long DEFAULT_TASK_COST_NS = 50_000L;
    private static final ThreadLocal<Boolean> WORKER_EXECUTION = ThreadLocal.withInitial(() -> false);
    private static final List<TickTask> PENDING = new ArrayList<>();
    private static final Set<String> RUNTIME_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final Map<String, AtomicLong> CLASS_COST_NS = new ConcurrentHashMap<>();

    private AsyncCreateBlockEntityDispatcher() {
    }

    public static boolean isWorkerExecution() {
        return WORKER_EXECUTION.get();
    }

    public static boolean enqueue(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos,
                                  BlockState state, BlockEntity blockEntity) {
        if (!SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITIES.get() || isWorkerExecution()) {
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
            long chunkKey = ChunkPos.asLong(task.pos().getX() >> 4, task.pos().getZ() >> 4);
            chunkGroups.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(task);
        }

        int workerCount = Math.min(
                Math.min(SeaborgiumConfig.ASYNC_CREATE_BLOCK_ENTITY_THREADS.get(), SeaborgiumJobSystem.workerCount()),
                chunkGroups.size()
        );
        if (workerCount <= 1) {
            runInline(tasks);
            return;
        }

        List<WeightedGroup> groups = new ArrayList<>(chunkGroups.size());
        for (List<TickTask> group : chunkGroups.values()) {
            long estimated = 0L;
            for (TickTask task : group) {
                estimated += estimatedCost(task.blockEntity().getClass().getName());
            }
            groups.add(new WeightedGroup(group, estimated));
        }
        groups.sort(Comparator.comparingLong(WeightedGroup::estimatedNs).reversed());

        List<WorkerBin> bins = new ArrayList<>(workerCount);
        for (int i = 0; i < workerCount; i++) {
            bins.add(new WorkerBin());
        }

        for (WeightedGroup group : groups) {
            WorkerBin lightest = bins.stream()
                    .min(Comparator.comparingLong(WorkerBin::estimatedNs))
                    .orElseThrow();
            lightest.groups.add(group);
            lightest.estimatedNs += group.estimatedNs();
        }

        CountDownLatch done = new CountDownLatch(workerCount);
        for (WorkerBin bin : bins) {
            SeaborgiumJobSystem.executor().execute(() -> {
                WORKER_EXECUTION.set(true);
                try {
                    for (WeightedGroup group : bin.groups) {
                        for (TickTask task : group.tasks()) {
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

    private static long estimatedCost(String className) {
        AtomicLong cost = CLASS_COST_NS.get(className);
        return cost == null ? DEFAULT_TASK_COST_NS : Math.max(1L, cost.get());
    }

    private static void updateCost(String className, long measuredNs) {
        CLASS_COST_NS.compute(className, (ignored, current) -> {
            if (current == null) {
                return new AtomicLong(Math.max(1L, measuredNs));
            }
            long previous = current.get();
            long ewma = (previous * 7L + Math.max(1L, measuredNs)) / 8L;
            current.set(ewma);
            return current;
        });
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
        long start = System.nanoTime();
        try {
            task.ticker().tick(task.level(), task.pos(), task.state(), task.blockEntity());
        } catch (Throwable throwable) {
            if (RUNTIME_BLACKLIST.add(className)) {
                LOGGER.error("Seaborgium disabled async Create block entity ticking for {} after a worker failure. Following ticks will use the render thread.", className, throwable);
            }
        } finally {
            updateCost(className, System.nanoTime() - start);
        }
    }

    private record TickTask(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos,
                            BlockState state, BlockEntity blockEntity) {
    }

    private record WeightedGroup(List<TickTask> tasks, long estimatedNs) {
    }

    private static final class WorkerBin {
        private final List<WeightedGroup> groups = new ArrayList<>();
        private long estimatedNs;

        private long estimatedNs() {
            return estimatedNs;
        }
    }
}
