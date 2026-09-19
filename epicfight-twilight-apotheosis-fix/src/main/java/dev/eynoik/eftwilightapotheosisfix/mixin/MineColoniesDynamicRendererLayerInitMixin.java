package dev.eynoik.eftwilightapotheosisfix.mixin;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.asset.AssetAccessor;

/**
 * DynamicMeshPatchRenderer's own comment relies on PatchedLivingEntityRenderer#initLayerLast() to
 * wrap vanilla layers that it does not patch itself (especially HumanoidArmorLayer). The compat's
 * registration path constructs the renderer but does not call initLayerLast().
 */
@Pseudo
@Mixin(targets = "com.sapphic.efxmco.client.DynamicMeshPatchRenderer", remap = false)
public abstract class MineColoniesDynamicRendererLayerInitMixin {
    private static final AtomicBoolean EFTAFIX_WARNING_PRINTED = new AtomicBoolean();

    @Inject(
            method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/world/entity/EntityType;Ljava/util/function/Function;Lyesman/epicfight/api/asset/AssetAccessor;)V",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void eftafix$initializeFallbackVanillaLayers(
            EntityRendererProvider.Context context,
            EntityType<?> entityType,
            Function<?, ?> meshProvider,
            AssetAccessor<?> fallbackMesh,
            CallbackInfo ci
    ) {
        try {
            Method initLayerLast = this.getClass().getMethod(
                    "initLayerLast",
                    EntityRendererProvider.Context.class,
                    EntityType.class
            );
            initLayerLast.invoke(this, context, entityType);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (EFTAFIX_WARNING_PRINTED.compareAndSet(false, true)) {
                System.err.println("[EFTwilightApotheosisFix] Could not initialize epicfightxminecolonies fallback layers. " + ex);
            }
        }
    }
}
