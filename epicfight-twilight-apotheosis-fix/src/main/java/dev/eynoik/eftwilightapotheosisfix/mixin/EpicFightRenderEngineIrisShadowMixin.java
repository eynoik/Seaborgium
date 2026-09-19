package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.perf.IrisShadowPassDetector;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Primary Iris-shadow optimization for Epic Fight 21.17.3.1.
 *
 * RenderEngine#epicfight$renderLivingPre is Epic Fight's RenderLivingEvent.Pre listener. In normal
 * rendering it replaces the vanilla renderer with PatchedLivingEntityRenderer / SkinnedMesh and can
 * cancel the vanilla event. During an Iris shadow pass none of Epic Fight's UI or visible-model
 * replacement work is required: vanilla entity geometry is sufficient to cast the shadow.
 *
 * Cancelling THIS listener (not the RenderLivingEvent itself) leaves the vanilla renderer untouched
 * and avoids capability lookup, patched renderer dispatch, layer rendering and entity-UI work.
 *
 * The overrideRender shadow mixins remain as a fail-soft secondary path for nearby Epic Fight builds
 * where this private listener name/signature might differ.
 */
@Pseudo
@Mixin(
        targets = "yesman.epicfight.client.events.engine.RenderEngine",
        priority = 1200,
        remap = false
)
public abstract class EpicFightRenderEngineIrisShadowMixin {
    @Inject(
            method = "epicfight$renderLivingPre(Lnet/neoforged/neoforge/client/event/RenderLivingEvent$Pre;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void eftafix$skipEpicFightRenderHookDuringIrisShadow(
            RenderLivingEvent.Pre<?, ?> event,
            CallbackInfo ci
    ) {
        if (IrisShadowPassDetector.isShadowPass()) {
            ci.cancel();
        }
    }
}
