package dev.eynoik.eftwilightapotheosisfix.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(targets = "com.p1nero.epicfightbow.animations.ScanAttackAnimation", remap = false)
public abstract class P1neroScanAimMixin {
    @Inject(
            method = "getNearestScannedTarget(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;)Lnet/minecraft/world/entity/LivingEntity;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void eftafix$disableAutomaticTargetFallback(
            LivingEntityPatch<?> patch,
            CallbackInfoReturnable<LivingEntity> cir
    ) {
        cir.setReturnValue(null);
    }

    @Redirect(
            method = "attackTick(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/asset/AssetAccessor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;setYRot(F)V",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private void eftafix$letTwilightAimOwnYaw(LivingEntityPatch<?> patch, float yaw) {
        // Intentionally no-op: explicit Epic Fight lock-on still owns its normal rotation path.
    }
}
