package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.perf.IrisShadowPassDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spark from the target pack showed Iris ShadowRenderer spending a large amount of the Render
 * Thread inside Epic Fight SkinnedMesh. Epic Fight's RenderEngine only replaces/cancels the vanilla
 * LivingEntityRenderer when LivingEntityPatch#overrideRender() returns true.
 *
 * During the Iris shadow-map pass only, return false. The normal vanilla renderer therefore draws
 * the entity into the shadow map, while the visible/main pass keeps full Epic Fight animation,
 * armor and combat rendering. This avoids doing CPU skinned Epic Fight rendering twice per frame.
 *
 * Higher priority than the MineColonies civilian gate so the shadow-map decision is made first.
 */
@Pseudo
@Mixin(
        targets = "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch",
        priority = 1200,
        remap = false
)
public abstract class EpicFightIrisShadowFallbackMixin {
    @Inject(
            method = "overrideRender()Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void eftafix$useVanillaRendererForIrisShadowPass(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (IrisShadowPassDetector.isShadowPass()) {
            cir.setReturnValue(false);
        }
    }
}
