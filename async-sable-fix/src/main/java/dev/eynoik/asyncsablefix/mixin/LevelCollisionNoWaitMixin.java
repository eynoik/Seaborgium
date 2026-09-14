package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.CompatUtil;
import dev.eynoik.asyncsablefix.LoadedChunkLookup;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.Level", priority = 900)
public abstract class LevelCollisionNoWaitMixin {
    @Inject(method = "getChunkForCollisions(II)Lnet/minecraft/world/level/BlockGetter;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void asyncsablefix$nonBlockingCollisionChunk(int chunkX, int chunkZ, CallbackInfoReturnable<BlockGetter> cir) {
        if (!CompatUtil.isAsyncTickThread()) return;
        Level level = (Level) (Object) this;
        Object loaded = LoadedChunkLookup.getLoadedChunk(level.getChunkSource(), chunkX, chunkZ);
        cir.setReturnValue((BlockGetter) loaded);
    }
}
