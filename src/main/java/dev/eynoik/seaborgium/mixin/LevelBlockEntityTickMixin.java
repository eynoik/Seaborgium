package dev.eynoik.seaborgium.mixin;

import dev.eynoik.seaborgium.client.AsyncCreateBlockEntityDispatcher;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelBlockEntityTickMixin {
    @Inject(method = "tickBlockEntities", at = @At("RETURN"))
    private void seaborgium$flushAsyncCreateBlockEntityTicks(CallbackInfo callbackInfo) {
        Level level = (Level) (Object) this;
        if (level.isClientSide()) {
            AsyncCreateBlockEntityDispatcher.flush();
        }
    }
}
