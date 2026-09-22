package dev.eynoik.createlogisticsfix.mixin;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackagerBlockEntity.class)
abstract class PackagerBlockEntityMixin {
    @Shadow
    private InventorySummary availableItems;

    @Shadow
    public InvManipulationBehaviour targetInventory;

    @Inject(method = "getAvailableItems", at = @At("HEAD"), cancellable = true)
    private void clof$preserveCachedSummaryWhenTargetIsTemporarilyMissing(
        CallbackInfoReturnable<InventorySummary> cir
    ) {
        if (availableItems != null
            && targetInventory != null
            && targetInventory.getInventory() == null) {
            cir.setReturnValue(availableItems);
        }
    }
}
