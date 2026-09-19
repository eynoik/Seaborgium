package dev.eynoik.eftwilightapotheosisfix.mixin;

import dev.eynoik.eftwilightapotheosisfix.compat.MineColoniesRenderPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * epicfightxminecolonies patches every citizen/visitor as an Epic Fight living entity. Rendering all
 * farmers, couriers, builders, visitors, etc. through SkinnedMesh is unnecessary and extremely
 * expensive in large colonies. Keep the EF renderer only for MineColonies jobs whose IJob#isGuard()
 * returns true. Raiders/mercenaries are separate entity classes and are deliberately untouched.
 *
 * Important: this mixin deliberately targets LivingEntityPatch by string and resolves getOriginal()
 * reflectively. Epic Fight inherits getOriginal() from EntityPatch<T extends Entity>; linking it
 * through an unbounded compile-only generic stub produces the wrong JVM return descriptor (Object)
 * and caused the 0.2.0 client NoSuchMethodError.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch", remap = false)
public abstract class MineColoniesCitizenRenderGateMixin {
    @Inject(
            method = "overrideRender()Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void eftafix$useVanillaRendererForNonCombatCitizens(CallbackInfoReturnable<Boolean> cir) {
        Object original = MineColoniesRenderPolicy.getOriginalFromPatch(this);
        Boolean useEpic = MineColoniesRenderPolicy.shouldUseEpicRenderer(original);

        if (Boolean.FALSE.equals(useEpic)) {
            cir.setReturnValue(false);
        }
    }
}
