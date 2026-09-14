package dev.eynoik.asyncsablefix;

import net.neoforged.fml.common.Mod;

@Mod(AsyncSableFix.MOD_ID)
public final class AsyncSableFix {
    public static final String MOD_ID = "asyncsablefix";

    public AsyncSableFix() {
        System.out.println("[AsyncSableFix] Loaded: Sable async chunk-generation guard + unsafe entity sync guard enabled.");
    }
}
