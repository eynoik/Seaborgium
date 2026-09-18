package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.perf.DisplayedAttributeCache;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.edwar.twilightmortisbows.TwilightWeaponAttributes", remap = false)
public abstract class TwilightWeaponAttributeCacheMixin {
    private static final String TARGET =
            "getDisplayedAttributeValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;D)D";

    @Inject(
            method = TARGET,
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void eftafix$reuseDisplayedAttribute(
            ItemStack stack,
            Holder<Attribute> attribute,
            double baseValue,
            CallbackInfoReturnable<Double> cir
    ) {
        Double cached = DisplayedAttributeCache.get(stack, attribute, baseValue);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(
            method = TARGET,
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private static void eftafix$rememberDisplayedAttribute(
            ItemStack stack,
            Holder<Attribute> attribute,
            double baseValue,
            CallbackInfoReturnable<Double> cir
    ) {
        DisplayedAttributeCache.put(stack, attribute, baseValue, cir.getReturnValue());
    }
}
