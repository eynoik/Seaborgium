package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.client.AsyncJeiSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientFilter", remap = false)
public abstract class JeiIngredientFilterMixin {
    @Inject(method = "getElements()Ljava/util/List;", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void seaborgium$reuseAsyncFilter(CallbackInfoReturnable<List<?>> cir) {
        List<?> result = AsyncJeiSearch.readyOrSchedule(this);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "getElements()Ljava/util/List;", at = @At("RETURN"), require = 0, remap = false)
    private void seaborgium$seedAsyncFilter(CallbackInfoReturnable<List<?>> cir) {
        AsyncJeiSearch.seed(this, cir.getReturnValue());
    }
}
