package dev.eynoik.eftwilightapotheosisfix.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.model.SkinnedMesh;

/**
 * Epic Fight - First Person Model duplicates Epic Fight's armor texture lookup:
 *
 * ParseUtil.tryGetOr(
 *     () -> armorMesh.getRenderProperties().customTexturePath(),
 *     () -> ClientHooks.getArmorTexture(...)
 * )
 *
 * When a dynamically baked armor mesh has no RenderProperties, the intended fallback first creates
 * a full NullPointerException stack trace on the Render Thread. The main Epic Fight layer already
 * has the same stackless fast-path in this mod; this applies it to the optional first-person addon.
 *
 * The exact released 1.21.1 addon has three lambdas in renderLayer. The SkinnedMesh -> ResourceLocation
 * descriptor uniquely identifies the custom-texture supplier, but multiple synthetic suffixes are
 * listed fail-soft so minor recompiles do not turn this optional compat into a startup crash.
 */
@Pseudo
@Mixin(
        targets = "net.kenji.first_person_compat.client.layers.FirstPersonWearableItemLayer",
        remap = false
)
public abstract class FirstPersonWearableFallbackFastPathMixin {
    private static final RuntimeException EFTAFIX_FAST_FALLBACK =
            new FastFirstPersonArmorTextureFallback();

    @Inject(
            method = {
                    "lambda$renderLayer$0(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;",
                    "lambda$renderLayer$1(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;",
                    "lambda$renderLayer$2(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;",
                    "lambda$renderLayer$3(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;",
                    "lambda$renderLayer$4(Lyesman/epicfight/api/client/model/SkinnedMesh;)Lnet/minecraft/resources/ResourceLocation;"
            },
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private static void eftafix$avoidFirstPersonArmorNpeFallback(
            SkinnedMesh mesh,
            CallbackInfoReturnable<ResourceLocation> cir
    ) {
        if (mesh.getRenderProperties() == null) {
            throw EFTAFIX_FAST_FALLBACK;
        }
    }

    private static final class FastFirstPersonArmorTextureFallback
            extends RuntimeException {
        private FastFirstPersonArmorTextureFallback() {
            super(null, null, false, false);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
