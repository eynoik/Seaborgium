package dev.eynoik.eftwilightapotheosisfix.mixin;

import java.util.ArrayDeque;
import net.minecraft.client.model.HumanoidModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * epicfightxminecolonies bakes shared MineColonies HumanoidModel instances into cached SkinnedMeshes.
 * Epic Fight's transformer only emits a core part when that ModelPart is visible at bake time.
 * Armor/layer rendering can temporarily change those visibility flags, so a first bake made in that
 * state can permanently cache a mesh with a missing head/hat/body/limb.
 *
 * Force the seven vanilla humanoid core parts visible only for the duration of the bake, then restore
 * the exact previous flags.
 */
@Pseudo
@Mixin(targets = "com.sapphic.efxmco.client.mesh.DynamicMeshCache", remap = false)
public abstract class MineColoniesDynamicMeshVisibilityMixin {
    private static final ThreadLocal<ArrayDeque<VisibilityState>> EFTAFIX_VISIBILITY =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static final String BAKE =
            "bake(Lnet/minecraft/client/model/HumanoidModel;)Lyesman/epicfight/api/client/model/SkinnedMesh;";

    @Inject(method = BAKE, at = @At("HEAD"), require = 0, remap = false)
    private static void eftafix$forceCompleteHumanoidMesh(
            HumanoidModel<?> model,
            CallbackInfoReturnable<Object> cir
    ) {
        EFTAFIX_VISIBILITY.get().push(new VisibilityState(model));

        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
    }

    @Inject(method = BAKE, at = @At("RETURN"), require = 0, remap = false)
    private static void eftafix$restoreHumanoidVisibility(
            HumanoidModel<?> model,
            CallbackInfoReturnable<Object> cir
    ) {
        ArrayDeque<VisibilityState> stack = EFTAFIX_VISIBILITY.get();
        VisibilityState state = stack.poll();

        if (state != null) {
            state.restore(model);
        }

        if (stack.isEmpty()) {
            EFTAFIX_VISIBILITY.remove();
        }
    }

    private record VisibilityState(
            boolean head,
            boolean hat,
            boolean body,
            boolean leftArm,
            boolean rightArm,
            boolean leftLeg,
            boolean rightLeg
    ) {
        private VisibilityState(HumanoidModel<?> model) {
            this(
                    model.head.visible,
                    model.hat.visible,
                    model.body.visible,
                    model.leftArm.visible,
                    model.rightArm.visible,
                    model.leftLeg.visible,
                    model.rightLeg.visible
            );
        }

        private void restore(HumanoidModel<?> model) {
            model.head.visible = this.head;
            model.hat.visible = this.hat;
            model.body.visible = this.body;
            model.leftArm.visible = this.leftArm;
            model.rightArm.visible = this.rightArm;
            model.leftLeg.visible = this.leftLeg;
            model.rightLeg.visible = this.rightLeg;
        }
    }
}
