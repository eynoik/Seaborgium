package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.ClipGuard;
import dev.eynoik.asyncsablefix.CompatUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks the complete Sable-overwritten BlockGetter#clip call while it runs on an
 * Async entity worker. Priority 900 applies after Sable's priority-1100 overwrite.
 */
@Mixin(value = BlockGetter.class, priority = 900)
public interface BlockGetterClipGuardMixin {
    @Inject(method = "clip", at = @At("HEAD"), require = 1)
    private void asyncsablefix$enterAsyncClip(ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        if (CompatUtil.isAsyncTickThread()) {
            ClipGuard.enter();
        }
    }

    @Inject(method = "clip", at = @At("RETURN"), require = 1)
    private void asyncsablefix$exitAsyncClip(ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        if (CompatUtil.isAsyncTickThread()) {
            ClipGuard.exit();
        }
    }
}
