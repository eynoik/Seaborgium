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
    private long asyncsablefix$guardChunkPos = Long.MIN_VALUE;

    @Unique
    private boolean asyncsablefix$guardChunkLoaded;

    /**
     * Loaded-only guard cached per chunk for the lifetime of Sable's LevelAccelerator.
     * Never calls Level#isLoaded, ChunkSource#getChunkNow or ChunkSource#getChunk.
     */
    @Unique
    private boolean asyncsablefix$denyUnloaded(BlockPos pos) {
        if (level.isClientSide()) return false;

        final int chunkX = pos.getX() >> 4;
        final int chunkZ = pos.getZ() >> 4;
        final long packed = ((long) chunkX & 0xffffffffL) | (((long) chunkZ & 0xffffffffL) << 32);

        if (packed != asyncsablefix$guardChunkPos) {
            asyncsablefix$guardChunkLoaded = LoadedChunkLookup.getLoadedChunk(level.getChunkSource(), chunkX, chunkZ) != null;
            asyncsablefix$guardChunkPos = packed;
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
        if (CompatUtil.isAsyncTickThread() && asyncsablefix$denyUnloaded(pos)) ci.cancel();
    }
}
