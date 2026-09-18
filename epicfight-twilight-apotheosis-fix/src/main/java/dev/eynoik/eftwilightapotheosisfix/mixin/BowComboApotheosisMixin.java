package dev.eynoik.eftwilightapotheosisfix.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.edwar.twilightmortisbows.BowComboEvents", remap = false)
public abstract class BowComboApotheosisMixin {
    private static final String APOTH_GENERATED = "apoth.generated";

    @Inject(
            method = "onArrowSpawn(Lnet/neoforged/neoforge/event/entity/EntityJoinLevelEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void eftafix$ignoreApotheosisGeneratedArrow(EntityJoinLevelEvent event, CallbackInfo ci) {
        Entity entity = event.getEntity();
        if (entity instanceof AbstractArrow && entity.getPersistentData().getBoolean(APOTH_GENERATED)) {
            ci.cancel();
        }
    }
}
