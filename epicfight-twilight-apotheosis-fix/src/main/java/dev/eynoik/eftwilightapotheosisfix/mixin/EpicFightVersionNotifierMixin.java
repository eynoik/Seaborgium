package dev.eynoik.eftwilightapotheosisfix.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Epic Fight 21.17.3.1 classifies its four-part release version as a testing build and draws
 * "Epic Fight is testing version." plus the version string on the HUD/menu through VersionNotifier.
 *
 * This is purely cosmetic. Suppress the notifier without changing Epic Fight's version string,
 * feature flags, networking or compatibility checks.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.client.gui.VersionNotifier", remap = false)
public abstract class EpicFightVersionNotifierMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void eftafix$hideTestingVersionOverlay(
            GuiGraphics guiGraphics,
            boolean inWorld,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
