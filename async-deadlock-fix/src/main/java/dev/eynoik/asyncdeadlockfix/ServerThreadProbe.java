package dev.eynoik.asyncdeadlockfix;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServerThreadProbe {
    private static volatile Method getServerMethod;
    private static volatile boolean lookupDone;
    private static final AtomicBoolean warningPrinted = new AtomicBoolean();

    private ServerThreadProbe() {
    }

    static boolean isServerThread() {
        MinecraftServer server = getServer();
        return server != null && server.isSameThread();
    }

    private static MinecraftServer getServer() {
        try {
            Method method = getServerMethod;
            if (!lookupDone) {
                synchronized (ServerThreadProbe.class) {
                    if (!lookupDone) {
                        Class<?> parallelProcessor = Class.forName(
                                "com.axalotl.async.common.ParallelProcessor",
                                false,
                                ServerThreadProbe.class.getClassLoader()
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
            Object value = method.invoke(null);
            return value instanceof MinecraftServer minecraftServer ? minecraftServer : null;
        } catch (Throwable throwable) {
            lookupDone = true;
            if (warningPrinted.compareAndSet(false, true)) {
                System.err.println("[AsyncDeadlockFix] Could not identify the Async server thread: " + throwable);
            }
            return null;
        }
    }
}
