package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.client.WorldRenderProfiler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class LevelRendererMixin {
    @Unique
    private long seaborgium$sectionLayerStartedAt;

    @Inject(
            method = "renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void seaborgium$beginSectionLayerProfile(
            RenderType renderType,
            double cameraX,
            double cameraY,
            double cameraZ,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        seaborgium$sectionLayerStartedAt = WorldRenderProfiler.begin(renderType);
    }

    @Inject(
            method = "renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At("RETURN"),
            require = 0
    )
    private void seaborgium$endSectionLayerProfile(
            RenderType renderType,
            double cameraX,
            double cameraY,
            double cameraZ,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        WorldRenderProfiler.end(renderType, seaborgium$sectionLayerStartedAt);
        seaborgium$sectionLayerStartedAt = 0L;
    }
}
