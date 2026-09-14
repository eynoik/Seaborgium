package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import dev.eynoik.seaborgium.client.CreateFactoryPanelOptimizer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity", remap = false)
public abstract class FactoryPanelBlockEntityMixin {
    @Inject(
            method = "createRenderBoundingBox",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void seaborgium$tighterFactoryPanelRenderBounds(CallbackInfoReturnable<AABB> callbackInfo) {
        if (!SeaborgiumConfig.FACTORY_PANEL_OPTIMIZATIONS.get()) {
            return;
        }

        BlockEntity blockEntity = (BlockEntity) (Object) this;
        AABB optimized = CreateFactoryPanelOptimizer.optimizedRenderBounds(blockEntity);
        if (optimized != null) {
            callbackInfo.setReturnValue(optimized);
        }
    }
}
