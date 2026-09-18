package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.client.TooltipMemoizer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipCacheMixin {
    private static final String TARGET =
            "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;";

    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true, require = 1)
    private void seaborgium$reuseTooltip(
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        if (TooltipMemoizer.isWorkerBypass()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        List<Component> cached = TooltipMemoizer.get(stack, context, player, flag);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = TARGET, at = @At("RETURN"), require = 1)
    private void seaborgium$rememberTooltip(
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        if (TooltipMemoizer.isWorkerBypass()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        TooltipMemoizer.put(stack, context, player, flag, cir.getReturnValue());
    }
}
