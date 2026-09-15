package dev.eynoik.asyncsablefix;

/** Thread-local scope for block raycasts currently executing on an Async tick worker. */
public final class ClipGuard {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ClipGuard() {}

    public static void enter() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) DEPTH.remove();
        else DEPTH.set(depth);
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }
}
