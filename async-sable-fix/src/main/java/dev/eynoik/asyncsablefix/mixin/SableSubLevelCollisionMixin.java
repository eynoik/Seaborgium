package dev.eynoik.asyncsablefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SableSubLevelCollisionMixin {
    @ModifyConstant(method = "collide", constant = @Constant(doubleValue = 1.25E8D), remap = false, require = 1)
    private static double asyncsablefix$tighterCollisionVolumeGuard(double original) {
        return 4096.0D;
    }
}
