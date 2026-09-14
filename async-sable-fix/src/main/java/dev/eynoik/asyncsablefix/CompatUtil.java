package dev.eynoik.asyncsablefix;

public final class CompatUtil {
    private static final String ASYNC_THREAD_PREFIX = "Async-Tick-Pool-Thread-";

    private CompatUtil() {}

    public static boolean isAsyncTickThread() {
        return Thread.currentThread().getName().startsWith(ASYNC_THREAD_PREFIX);
    }

    public static boolean hasClassInHierarchy(Object object, String className) {
        if (object == null) return false;
        Class<?> type = object.getClass();
        while (type != null) {
            if (type.getName().equals(className)) return true;
            type = type.getSuperclass();
        }
        return false;
    }
}
