package dev.eynoik.seaborgium;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(value = Seaborgium.MOD_ID, dist = Dist.CLIENT)
public final class Seaborgium {
    public static final String MOD_ID = "seaborgium";

    public Seaborgium(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SeaborgiumConfig.SPEC);
    }
}
