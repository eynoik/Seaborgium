package dev.eynoik.asyncdeadlockfix;

import net.neoforged.fml.common.Mod;

@Mod(AsyncDeadlockFix.MOD_ID)
public final class AsyncDeadlockFix {
    public static final String MOD_ID = "asyncdeadlockfix";

    public AsyncDeadlockFix() {
        System.out.println("[AsyncDeadlockFix] Loaded: Async invokeAll/main-thread chunk pump guard enabled.");
    }
}
