package dev.eynoik.eftwilightapotheosisfix.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.model.SkinnedMesh;

/**
 * Epic Fight 21.17.3.1 intentionally uses ParseUtil.tryGetOr around armor customTexturePath lookup.
 * Dynamically baked meshes commonly have null renderProperties, so the normal fallback path first
 * creates a NullPointerException and fills its stack trace. Spark showed that exception work on the
 * Render Thread.
 *
 * Throw a stackless singleton before the null dereference. ParseUtil still takes its existing
 * fallback supplier, preserving texture behavior while removing Throwable#fillInStackTrace cost.
 */
@Mixin(targets = "yesman.epicfight.client.renderer.patched.layer.WearableItemLayer", remap = false)
public abstract class EpicFightWearableFallbackFastPathMixin {
    private static final RuntimeException EFTAFIX_FAST_FALLBACK = new FastArmorTextureFallback();

    @Inject(
            method = "lambda$renderLayer$3(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            require = 1,
            remap = false
    )
    private static void eftafix$avoidExpensiveNullPointerFallback(
            SkinnedMesh mesh,
            CallbackInfoReturnable<ResourceLocation> cir
    ) {
        if (mesh.getRenderProperties() == null) {
            throw EFTAFIX_FAST_FALLBACK;
        }
    }

    private static final class FastArmorTextureFallback extends RuntimeException {
        private FastArmorTextureFallback() {
            super(null, null, false, false);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
