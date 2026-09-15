package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.ClipGuard;
import dev.eynoik.asyncsablefix.CompatUtil;
import dev.eynoik.asyncsablefix.LoadedChunkLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Sable raycasts performed from Async workers from entering a blocking
 * ServerChunkCache#getChunk handoff. Loaded chunks are read directly. A missing
 * chunk is treated as a solid boundary (BEDROCK + empty fluid), which both keeps
 * line-of-sight conservative and stops pathological projected rays quickly.
 */
@Mixin(value = Level.class, priority = 900)
public abstract class LevelClipNoLoadMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true, require = 1)
    private void asyncsablefix$loadedOnlyClipBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!CompatUtil.isAsyncTickThread() || !ClipGuard.active()) return;

        Level self = (Level) (Object) this;
        Object loaded = LoadedChunkLookup.getLoadedChunk(self.getChunkSource(), pos.getX() >> 4, pos.getZ() >> 4);
        if (loaded instanceof BlockGetter getter) {
            cir.setReturnValue(getter.getBlockState(pos));
        } else {
            cir.setReturnValue(Blocks.BEDROCK.defaultBlockState());
        }
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true, require = 1)
    private void asyncsablefix$loadedOnlyClipFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (!CompatUtil.isAsyncTickThread() || !ClipGuard.active()) return;

        Level self = (Level) (Object) this;
        Object loaded = LoadedChunkLookup.getLoadedChunk(self.getChunkSource(), pos.getX() >> 4, pos.getZ() >> 4);
        if (loaded instanceof BlockGetter getter) {
            cir.setReturnValue(getter.getFluidState(pos));
        } else {
            cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }
}
