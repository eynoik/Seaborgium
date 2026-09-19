package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.compat.MineColoniesRenderPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * epicfightxminecolonies patches every citizen/visitor as an Epic Fight living entity. Rendering all
 * farmers, couriers, builders, visitors, etc. through SkinnedMesh is unnecessary and extremely
 * expensive in large colonies. Keep the EF renderer only for MineColonies jobs whose IJob#isGuard()
 * returns true. Raiders/mercenaries are separate entity classes and are deliberately untouched.
 */
@Mixin(value = LivingEntityPatch.class, remap = false)
public abstract class MineColoniesCitizenRenderGateMixin {
    @Inject(
            method = "overrideRender()Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void eftafix$useVanillaRendererForNonCombatCitizens(CallbackInfoReturnable<Boolean> cir) {
        Object original = ((LivingEntityPatch<?>)(Object)this).getOriginal();
        Boolean useEpic = MineColoniesRenderPolicy.shouldUseEpicRenderer(original);

        if (Boolean.FALSE.equals(useEpic)) {
            cir.setReturnValue(false);
        }
    }
}
