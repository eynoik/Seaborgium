package dev.eynoik.asyncsablefix.mixin;

import java.util.Collections;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SableSubLevelCollisionMixin {
    @Unique
    private static final long ASYNCSABLEFIX_MAX_COLLISION_BLOCKS = 1024L;

    @ModifyConstant(method = "collide", constant = @Constant(doubleValue = 1.25E8D), remap = false, require = 1)
    private static double asyncsablefix$tighterCollisionVolumeGuard(double original) {
        return 4096.0D;
    }

    /**
     * Sable can create long/thin integer bounds whose volume still passes the 4096.0 guard,
     * then iterate the same block range repeatedly for substeps and MTV passes. Refuse only
     * pathological scans; ordinary entity collision boxes are far below this limit.
     */
    @Redirect(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"
            ),
            remap = false,
            require = 1
    )
    private static Iterable<BlockPos> asyncsablefix$capCollisionBlockScan(BlockPos first, BlockPos second) {
        final long dx = Math.abs((long) second.getX() - first.getX()) + 1L;
        final long dy = Math.abs((long) second.getY() - first.getY()) + 1L;
        final long dz = Math.abs((long) second.getZ() - first.getZ()) + 1L;

        if (dx > ASYNCSABLEFIX_MAX_COLLISION_BLOCKS
                || dy > ASYNCSABLEFIX_MAX_COLLISION_BLOCKS
                || dz > ASYNCSABLEFIX_MAX_COLLISION_BLOCKS) {
            return Collections.emptyList();
        }

        final long xy = dx * dy;
        if (xy > ASYNCSABLEFIX_MAX_COLLISION_BLOCKS
                || (dz != 0L && xy > ASYNCSABLEFIX_MAX_COLLISION_BLOCKS / dz)) {
            return Collections.emptyList();
        }

        return BlockPos.betweenClosed(first, second);
    }
}
