package dev.eynoik.asyncdeadlockfix.mixin;

import dev.eynoik.asyncdeadlockfix.PumpingThreadPoolExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Replaces Async's freshly-created ThreadPoolExecutor immediately after setupThreadPool().
 *
 * This is intentionally done after Async creates the pool instead of overwriting Async entity/spawn
 * logic. We preserve its core/max size, thread factory, rejection handler and timeout settings.
 * The only semantic change is PumpingThreadPoolExecutor#invokeAll on the Minecraft server thread.
 */
@Pseudo
@Mixin(targets = "com.axalotl.async.common.ParallelProcessor", remap = false)
public abstract class ParallelProcessorExecutorMixin {
    @Inject(
            method = "setupThreadPool",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private static void asyncdeadlockfix$installPumpingExecutor(
            int parallelism,
            Class<?> asyncClass,
            CallbackInfo ci
    ) {
        try {
            Class<?> owner = Class.forName(
                    "com.axalotl.async.common.ParallelProcessor",
                    false,
                    asyncClass.getClassLoader()
            );
            Field executorField = owner.getField("executor");
            Object value = executorField.get(null);

            if (!(value instanceof ThreadPoolExecutor oldPool)
                    || value instanceof PumpingThreadPoolExecutor) {
                return;
            }

            PumpingThreadPoolExecutor replacement = new PumpingThreadPoolExecutor(
                    oldPool.getCorePoolSize(),
                    oldPool.getMaximumPoolSize(),
                    oldPool.getKeepAliveTime(java.util.concurrent.TimeUnit.NANOSECONDS),
                    java.util.concurrent.TimeUnit.NANOSECONDS,
                    new LinkedBlockingQueue<>(),
                    oldPool.getThreadFactory(),
                    oldPool.getRejectedExecutionHandler()
            );
            replacement.allowCoreThreadTimeOut(oldPool.allowsCoreThreadTimeOut());

            // setupThreadPool has only just returned; Async has not submitted gameplay work yet.
            oldPool.shutdownNow();
            replacement.prestartAllCoreThreads();
            executorField.set(null, replacement);

            System.out.println(
                    "[AsyncDeadlockFix] Replaced Async executor with pumping executor ("
                            + replacement.getCorePoolSize() + " threads)."
            );
        } catch (Throwable throwable) {
            throw new IllegalStateException(
                    "AsyncDeadlockFix could not replace Async's executor; refusing to run without the measured deadlock fix",
                    throwable
            );
        }
    }
}
