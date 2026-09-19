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
import java.util.concurrent.TimeUnit;

/**
 * Runtime bridge for Async 0.2.0+alpha-1.21.1.
 *
 * Important version detail:
 * Async 0.2.0 names its pool field "tickPool". Later branches renamed it to "executor".
 * 0.1.0 of this patch incorrectly assumed only the later field name and threw from
 * setupThreadPool(), killing the Minecraft server thread. 0.1.1 resolves tickPool first,
 * accepts executor only as a compatibility fallback, and fails soft instead of terminating
 * the server if an unknown Async build is encountered.
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

            Field poolField = findPoolField(owner);
            if (poolField == null) {
                System.err.println(
                        "[AsyncDeadlockFix] Unsupported Async build: neither ParallelProcessor.tickPool "
                                + "nor ParallelProcessor.executor exists. Leaving Async untouched."
                );
                return;
            }

            Object value = poolField.get(null);
            if (value instanceof PumpingThreadPoolExecutor) {
                return;
            }
            if (!(value instanceof ThreadPoolExecutor oldPool)) {
                System.err.println(
                        "[AsyncDeadlockFix] Async pool field " + poolField.getName()
                                + " is not a ThreadPoolExecutor (" + value + "); leaving Async untouched."
                );
                return;
            }

            PumpingThreadPoolExecutor replacement = new PumpingThreadPoolExecutor(
                    oldPool.getCorePoolSize(),
                    oldPool.getMaximumPoolSize(),
                    oldPool.getKeepAliveTime(TimeUnit.NANOSECONDS),
                    TimeUnit.NANOSECONDS,
                    new LinkedBlockingQueue<>(),
                    oldPool.getThreadFactory(),
                    oldPool.getRejectedExecutionHandler()
            );
            replacement.allowCoreThreadTimeOut(oldPool.allowsCoreThreadTimeOut());

            /*
             * setupThreadPool() has just returned. The original Async 0.2.0 implementation has
             * only created/prestarted the workers at this point; gameplay jobs are not submitted
             * yet. Shut those idle workers down, start equivalent workers, then atomically publish
             * the replacement through the same static field used everywhere else by Async.
             */
            oldPool.shutdownNow();
            replacement.prestartAllCoreThreads();
            poolField.set(null, replacement);

            System.out.println(
                    "[AsyncDeadlockFix] Replaced Async " + poolField.getName()
                            + " with pumping executor (" + replacement.getCorePoolSize() + " threads)."
            );
        } catch (Throwable throwable) {
            /*
             * Never kill the server because the compatibility patch failed to install.
             * 0.1.0 did exactly that by throwing here. The original Async pool remains usable;
             * worst case the original watchdog/deadlock can still reproduce and the log clearly
             * states why this patch was not active.
             */
            System.err.println(
                    "[AsyncDeadlockFix] Could not install pumping executor; leaving Async's original "
                            + "pool untouched. Cause: " + throwable
            );
            throwable.printStackTrace(System.err);
        }
    }

    private static Field findPoolField(Class<?> owner) {
        for (String name : new String[] {"tickPool", "executor"}) {
            try {
                Field field = owner.getField(name);
                if (ExecutorService.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
