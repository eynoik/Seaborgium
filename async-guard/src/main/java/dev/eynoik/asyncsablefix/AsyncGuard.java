package dev.eynoik.asyncsablefix;

import net.neoforged.fml.common.Mod;

@Mod(AsyncGuard.MOD_ID)
public final class AsyncGuard {
    // Kept for upgrade safety: loading AsyncGuard together with an old AsyncSableFix/AsyncGuard JAR
    // must fail as a duplicate mod instead of applying the same mixins twice.
    public static final String MOD_ID = "asyncsablefix";

    public AsyncGuard() {
        System.out.println("[AsyncGuard] Loaded 0.2.1: Async compatibility/thread-safety guards active.");
    }
}
