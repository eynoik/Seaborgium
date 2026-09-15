package dev.eynoik.asyncepicfightfix.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Epic Fight 21.17.3.1 assumes MobEffectEvent.Expired always carries a non-null
 * MobEffectInstance. Async's effect-race guard can intentionally turn a vanished
 * effect into a null/expired path instead. Ignore only that invalid Epic Fight
 * callback so a player tick is not aborted with an NPE.
 */
@Mixin(targets = "yesman.epicfight.api.event.impl.VanillaEntityEventHooks", remap = false)
public abstract class EpicFightNullEffectMixin {
    @Inject(method = "onMobEffectExpired", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void asyncepicfightfix$skipNullExpiredEffect(
            MobEffectInstance mobEffectInstance,
            LivingEntity entity,
            CallbackInfo ci) {
        if (mobEffectInstance == null) {
            ci.cancel();
        }
    }
}
