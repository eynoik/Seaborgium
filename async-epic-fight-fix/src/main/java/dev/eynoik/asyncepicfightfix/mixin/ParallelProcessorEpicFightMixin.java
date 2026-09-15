package dev.eynoik.asyncepicfightfix.mixin;

import dev.eynoik.asyncepicfightfix.EpicFightAsyncCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.axalotl.async.common.ParallelProcessor", remap = false)
public abstract class ParallelProcessorEpicFightMixin {
    @Inject(method = "shouldTickSynchronously", at = @At("HEAD"), cancellable = true, require = 1)
    private static void asyncepicfightfix$serializePatchedEntities(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EpicFightAsyncCompat.shouldForceSynchronous(entity)) {
            cir.setReturnValue(true);
        }
    }
}
