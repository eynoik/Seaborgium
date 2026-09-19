package dev.eynoik.asyncsablefix;

import net.neoforged.fml.common.Mod;

@Mod(AsyncSableFix.MOD_ID)
public final class AsyncSableFix {
    public static final String MOD_ID = "asyncsablefix";

    public AsyncSableFix() {
        System.out.println("[AsyncSableFix] Loaded 0.1.7: serialized Sable sub-level collision + Async-only chunk/raycast guards.");
    }
}
