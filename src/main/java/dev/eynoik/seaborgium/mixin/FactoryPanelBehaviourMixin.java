package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour", remap = false)
public abstract class FactoryPanelBehaviourMixin {
    @Inject(
            method = "getRenderDistance",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void seaborgium$factoryPanelFilterItemLod(CallbackInfoReturnable<Float> callbackInfo) {
        if (!SeaborgiumConfig.FACTORY_PANEL_OPTIMIZATIONS.get()) {
            return;
        }
        callbackInfo.setReturnValue(SeaborgiumConfig.FACTORY_PANEL_FILTER_ITEM_DISTANCE.get().floatValue());
    }
}
