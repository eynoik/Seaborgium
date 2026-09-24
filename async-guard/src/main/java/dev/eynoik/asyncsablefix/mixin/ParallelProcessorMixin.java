package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.CompatUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.axalotl.async.common.ParallelProcessor", remap = false)
public abstract class ParallelProcessorMixin {
    @Inject(method = "shouldTickSynchronously", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void asyncsablefix$forceUnsafeFamiliesSync(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CompatUtil.hasClassInHierarchy(entity, "com.simibubi.create.content.contraptions.AbstractContraptionEntity")
                || CompatUtil.hasClassInHierarchy(entity, "net.conczin.mca.entity.VillagerEntityMCA")
                || CompatUtil.hasClassInHierarchy(entity, "com.minecolonies.api.entity.citizen.AbstractEntityCitizen")) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
