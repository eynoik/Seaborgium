package dev.eynoik.eftwilightapotheosisfix.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = "eftwilightapotheosisfix")
public final class ProjectileMarkerBridge {
    private static final String APOTH_GENERATED = "apoth.generated";
    private static final String TF_INTERNAL = "twilightmortisbows:internal_arrow";
    private static final String TF_DOUBLE_PHANTOM = "twilightmortisbows:double_arrow_phantom";

    private ProjectileMarkerBridge() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void eftafix$bridgeProjectileMarkers(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof AbstractArrow arrow)) {
            return;
        }

        CompoundTag data = arrow.getPersistentData();
        boolean apotheosisGenerated = data.getBoolean(APOTH_GENERATED);
        boolean twilightInternal = data.getBoolean(TF_INTERNAL) || data.getBoolean(TF_DOUBLE_PHANTOM);

        if (apotheosisGenerated && !data.getBoolean(TF_INTERNAL)) {
            data.putBoolean(TF_INTERNAL, true);
        }

        if (twilightInternal && !apotheosisGenerated) {
            data.putBoolean(APOTH_GENERATED, true);
        }
    }
}
