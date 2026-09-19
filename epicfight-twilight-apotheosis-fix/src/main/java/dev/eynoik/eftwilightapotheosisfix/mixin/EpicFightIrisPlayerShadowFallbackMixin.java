package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.perf.IrisShadowPassDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player patches override LivingEntityPatch#overrideRender(), so the base shadow fast-path alone
 * does not cover players. AbstractClientPlayerPatch is the shared implementation used by client
 * player patches; LocalPlayerPatch delegates to it after its first-person guard.
 */
@Pseudo
@Mixin(
        targets = "yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch",
        priority = 1200,
        remap = false
)
public abstract class EpicFightIrisPlayerShadowFallbackMixin {
    @Inject(
            method = "overrideRender()Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void eftafix$useVanillaPlayerRendererForIrisShadowPass(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (IrisShadowPassDetector.isShadowPass()) {
            cir.setReturnValue(false);
        }
    }
}
