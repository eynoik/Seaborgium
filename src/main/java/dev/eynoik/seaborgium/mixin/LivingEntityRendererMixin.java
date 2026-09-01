package dev.eynoik.seaborgium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eynoik.seaborgium.client.LayerBudget;
import dev.eynoik.seaborgium.client.LayerProfiler;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void seaborgium$budgetLayer(
            RenderLayer layer,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        boolean shouldRender = LayerBudget.shouldRender(layer, entity, partialTick);
        LayerProfiler.recordDecision(shouldRender);
        if (shouldRender) {
            long sampleStart = LayerProfiler.beginSample();
            // The cast is required because RenderLayer erases T to Entity in the
            // target bytecode while the mapped Java declaration uses LivingEntity.
            try {
                layer.render(
                        poseStack,
                        bufferSource,
                        packedLight,
                        (LivingEntity) entity,
                        limbSwing,
                        limbSwingAmount,
                        partialTick,
                        ageInTicks,
                        netHeadYaw,
                        headPitch
                );
            } finally {
                LayerProfiler.endSample(layer.getClass(), sampleStart);
            }
        }
    }
}
