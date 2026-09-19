package dev.eynoik.asyncdeadlockfix;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServerTaskPump {
    private static final AtomicBoolean LOOKUP_WARNING_PRINTED = new AtomicBoolean();
    private static volatile Method getServerMethod;
    private static volatile boolean lookupDone;

    private ServerTaskPump() {
    }

    static boolean pumpOnceIfServerThread() {
        MinecraftServer server = getServer();
        if (server == null || !server.isSameThread()) {
            return false;
        }

        boolean pumped = false;
        for (ServerLevel level : server.getAllLevels()) {
            try {
                pumped |= level.getChunkSource().pollTask();
            } catch (Throwable throwable) {
                // A single dimension must never break the wait loop.
                if (LOOKUP_WARNING_PRINTED.compareAndSet(false, true)) {
                    System.err.println("[AsyncDeadlockFix] Failed while pumping a chunk-source task: " + throwable);
                }
            }
        }
        return pumped;
    }

    private static MinecraftServer getServer() {
        try {
            Method method = getServerMethod;
            if (!lookupDone) {
                synchronized (ServerTaskPump.class) {
                    if (!lookupDone) {
                        Class<?> parallelProcessor = Class.forName(
                                "com.axalotl.async.common.ParallelProcessor",
                                false,
                                ServerTaskPump.class.getClassLoader()
                        );
                        method = parallelProcessor.getMethod("getServer");
                        method.setAccessible(true);
                        getServerMethod = method;
                        lookupDone = true;
                    } else {
                        method = getServerMethod;
                    }
                }
            }

            if (method == null) {
                return null;
            }
            Object server = method.invoke(null);
            return server instanceof MinecraftServer minecraftServer ? minecraftServer : null;
        } catch (Throwable throwable) {
            lookupDone = true;
            if (LOOKUP_WARNING_PRINTED.compareAndSet(false, true)) {
                System.err.println("[AsyncDeadlockFix] Could not resolve Async ParallelProcessor#getServer; invokeAll will fall back to normal waiting. " + throwable);
            }
            return null;
        }
    }
}
