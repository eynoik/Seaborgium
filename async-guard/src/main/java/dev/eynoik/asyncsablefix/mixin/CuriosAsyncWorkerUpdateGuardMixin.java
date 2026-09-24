package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.CompatUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Curios 9.5.1 recalculates dynamic slot size inside CurioStacksHandler#update.
 *
 * Relics can call Curios#getEquippedCurios from a NeoForge damage event fired
 * by an entity that Async is ticking off-thread. CurioStacksHandler#getStacks
 * then calls update() on that Async worker while the server thread can mutate
 * the same HashMultimap of slot modifiers, producing ConcurrentModificationException.
 *
 * On Async entity workers, keep the already-published stack handler state and
 * defer the mutable recalculation to Curios' normal server-thread update path.
 * The server thread and all non-Async callers retain Curios' original behavior.
 */
@Pseudo
@Mixin(
    targets = "top.theillusivec4.curios.common.inventory.CurioStacksHandler",
    remap = false
)
public abstract class CuriosAsyncWorkerUpdateGuardMixin {
    @Inject(
        method = "update",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private void asyncguard$deferMutableCuriosUpdateOffThread(CallbackInfo ci) {
        if (CompatUtil.isAsyncTickThread()) {
            ci.cancel();
        }
    }
}
