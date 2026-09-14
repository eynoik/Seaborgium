package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.client.AsyncCreateBlockEntityDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker", remap = false)
public abstract class SmartBlockEntityTickerMixin {
    @Inject(
            method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    @SuppressWarnings("unchecked")
    private void seaborgium$queueClientCreateBlockEntityTick(Level level, BlockPos pos, BlockState state,
                                                              BlockEntity blockEntity, CallbackInfo callbackInfo) {
        if (!level.isClientSide() || AsyncCreateBlockEntityDispatcher.isWorkerExecution()) {
            return;
        }

        BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) (Object) this;
        if (AsyncCreateBlockEntityDispatcher.enqueue(ticker, level, pos, state, blockEntity)) {
            callbackInfo.cancel();
        }
    }
}
