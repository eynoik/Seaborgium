package dev.eynoik.seaborgium.client;

import com.mojang.logging.LogUtils;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SeaborgiumJobSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    private static ExecutorService executor;
    private static int executorThreads;

    private SeaborgiumJobSystem() {
    }

    public static synchronized ExecutorService executor() {
        int wanted = Math.max(1, Math.min(
                SeaborgiumConfig.MULTITHREAD_WORKERS.get(),
                Math.max(1, Runtime.getRuntime().availableProcessors() - 2)
        ));

        if (executor == null || executor.isShutdown() || executorThreads != wanted) {
            if (executor != null) {
                executor.shutdown();
            }
            executorThreads = wanted;
            executor = Executors.newFixedThreadPool(wanted, new WorkerFactory());
            LOGGER.info("Seaborgium worker pool started with {} threads", wanted);
        }
        return executor;
    }

    public static int workerCount() {
        executor();
        return executorThreads;
    }

    private static final class WorkerFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Seaborgium-Worker-" + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        }
    }
}
