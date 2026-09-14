package dev.eynoik.asyncsablefix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Non-blocking loaded-chunk lookup for Async worker threads.
 *
 * This deliberately mirrors Async's own fast path:
 * ServerChunkCache#getVisibleChunkIfPresent -> ChunkHolder#getChunkIfPresent(FULL).
 * It never calls ChunkSource#getChunk/getChunkNow, so an absent chunk can never
 * fall through into Async's blocking main-thread handoff.
 */
public final class LoadedChunkLookup {
    private static volatile Class<?> sourceClass;
    private static volatile Method getVisibleChunkIfPresent;
    private static volatile Class<?> holderClass;
    private static volatile Method getChunkIfPresent;
    private static volatile Object fullStatus;
    private static volatile Class<?> imposterProtoChunkClass;
    private static volatile Method getWrapped;

    private LoadedChunkLookup() {}

    public static Object getLoadedChunk(Object chunkSource, int chunkX, int chunkZ) {
        if (chunkSource == null) return null;
        try {
            Method visible = visibleChunkMethod(chunkSource.getClass());
            if (visible == null) return null;
            long packedPos = ((long) chunkX & 0xffffffffL) | (((long) chunkZ & 0xffffffffL) << 32);
            Object holder = visible.invoke(chunkSource, packedPos);
            if (holder == null) return null;

            Method present = chunkIfPresentMethod(holder.getClass());
            Object full = fullStatus();
            if (present == null || full == null) return null;
            Object chunk = present.invoke(holder, full);
            if (chunk == null) return null;

            Class<?> imposter = imposterProtoChunkClass();
            if (imposter != null && imposter.isInstance(chunk)) {
                Method wrapped = wrappedMethod(imposter);
                return wrapped == null ? null : wrapped.invoke(chunk);
            }
            return chunk;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static Method visibleChunkMethod(Class<?> cls) {
        Method cached = getVisibleChunkIfPresent;
        if (cached != null && sourceClass != null && sourceClass.isAssignableFrom(cls)) return cached;
        synchronized (LoadedChunkLookup.class) {
            cached = getVisibleChunkIfPresent;
            if (cached != null && sourceClass != null && sourceClass.isAssignableFrom(cls)) return cached;
            Method found = findMethod(cls, "getVisibleChunkIfPresent", long.class);
            if (found != null) {
                found.setAccessible(true);
                sourceClass = cls;
                getVisibleChunkIfPresent = found;
            }
            return found;
        }
    }

    private static Method chunkIfPresentMethod(Class<?> cls) throws ClassNotFoundException {
        Method cached = getChunkIfPresent;
        if (cached != null && holderClass != null && holderClass.isAssignableFrom(cls)) return cached;
        synchronized (LoadedChunkLookup.class) {
            cached = getChunkIfPresent;
            if (cached != null && holderClass != null && holderClass.isAssignableFrom(cls)) return cached;
            Class<?> statusClass = Class.forName("net.minecraft.world.level.chunk.status.ChunkStatus", false, cls.getClassLoader());
            Method found = findMethod(cls, "getChunkIfPresent", statusClass);
            if (found != null) {
                found.setAccessible(true);
                holderClass = cls;
                getChunkIfPresent = found;
            }
            return found;
        }
    }

    private static Object fullStatus() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Object cached = fullStatus;
        if (cached != null) return cached;
        synchronized (LoadedChunkLookup.class) {
            cached = fullStatus;
            if (cached != null) return cached;
            Class<?> statusClass = Class.forName("net.minecraft.world.level.chunk.status.ChunkStatus");
            Field field = statusClass.getField("FULL");
            Object value = field.get(null);
            fullStatus = value;
            return value;
        }
    }

    private static Class<?> imposterProtoChunkClass() {
        Class<?> cached = imposterProtoChunkClass;
        if (cached != null) return cached;
        try {
            Class<?> value = Class.forName("net.minecraft.world.level.chunk.ImposterProtoChunk");
            imposterProtoChunkClass = value;
            return value;
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Method wrappedMethod(Class<?> imposterClass) {
        Method cached = getWrapped;
        if (cached != null) return cached;
        synchronized (LoadedChunkLookup.class) {
            cached = getWrapped;
            if (cached != null) return cached;
            Method found = findMethod(imposterClass, "getWrapped");
            if (found != null) {
                found.setAccessible(true);
                getWrapped = found;
            }
            return found;
        }
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... parameterTypes) {
        Class<?> current = cls;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        try {
            return cls.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
