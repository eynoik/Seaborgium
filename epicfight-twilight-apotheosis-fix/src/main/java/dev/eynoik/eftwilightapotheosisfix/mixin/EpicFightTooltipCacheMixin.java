package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.perf.EpicFightTooltipCache;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine", remap = false)
public abstract class EpicFightTooltipCacheMixin {
    private static final String TARGET =
            "epicfight$itemTooltip(Lnet/neoforged/neoforge/event/entity/player/ItemTooltipEvent;)V";

    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void eftafix$reuseEpicFightTooltip(ItemTooltipEvent event, CallbackInfo ci) {
        List<Component> cached = EpicFightTooltipCache.begin(event);
        if (cached == null) {
            return;
        }

        event.getToolTip().clear();
        event.getToolTip().addAll(cached);
        ci.cancel();
    }

    @Inject(method = TARGET, at = @At("RETURN"), require = 1, remap = false)
    private void eftafix$rememberEpicFightTooltip(ItemTooltipEvent event, CallbackInfo ci) {
        EpicFightTooltipCache.finish(event);
    }
}
