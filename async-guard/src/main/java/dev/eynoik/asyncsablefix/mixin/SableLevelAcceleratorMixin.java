package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.CompatUtil;
import dev.eynoik.asyncsablefix.LoadedChunkLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ryanhcode.sable.util.LevelAccelerator", remap = false)
public abstract class SableLevelAcceleratorMixin {
    @Shadow(remap = false) @Final
    private Level level;

    @Unique
    private long asyncsablefix$guardChunkPos;

    @Unique
    private boolean asyncsablefix$guardChunkLoaded;

    @Unique
    private boolean asyncsablefix$guardInitialized;

    /**
     * Loaded-only guard for Async workers only.
     *
     * Sable's normal server-thread LevelAccelerator path is intentionally left
     * completely untouched. In particular, assembly/disassembly must be allowed to
     * use Sable's own chunk access instead of receiving synthetic AIR/null results.
     */
    @Unique
    private boolean asyncsablefix$denyUnloaded(BlockPos pos) {
        if (!CompatUtil.isAsyncTickThread() || level.isClientSide()) return false;

        final int chunkX = pos.getX() >> 4;
        final int chunkZ = pos.getZ() >> 4;
        final long packed = ((long) chunkX & 0xffffffffL) | (((long) chunkZ & 0xffffffffL) << 32);

        if (!asyncsablefix$guardInitialized || packed != asyncsablefix$guardChunkPos) {
            asyncsablefix$guardChunkLoaded = LoadedChunkLookup.getLoadedChunk(level.getChunkSource(), chunkX, chunkZ) != null;
            asyncsablefix$guardChunkPos = packed;
            asyncsablefix$guardInitialized = true;
        }

        return !asyncsablefix$guardChunkLoaded;
    }

    @Inject(method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void asyncsablefix$blockStateWithoutChunkGeneration(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (asyncsablefix$denyUnloaded(pos)) cir.setReturnValue(Blocks.AIR.defaultBlockState());
    }

    @Inject(method = "getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void asyncsablefix$fluidStateWithoutChunkGeneration(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (asyncsablefix$denyUnloaded(pos)) cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }

    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void asyncsablefix$blockEntityWithoutChunkGeneration(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if (asyncsablefix$denyUnloaded(pos)) cir.setReturnValue(null);
    }

    @Inject(method = "setBlockFast(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void asyncsablefix$skipAsyncWriteIntoUnloadedChunk(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (asyncsablefix$denyUnloaded(pos)) ci.cancel();
    }
}
