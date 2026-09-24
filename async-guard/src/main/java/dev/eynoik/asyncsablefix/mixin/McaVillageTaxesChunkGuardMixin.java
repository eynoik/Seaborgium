package dev.eynoik.asyncsablefix.mixin;

import dev.eynoik.asyncsablefix.LoadedChunkLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCA 7.7.36-beta.3 checks only the village-center chunk before iterating every
 * storage-building block. A storage position may therefore live in another
 * chunk that is not loaded. tryToPutIntoInventory() immediately calls
 * ServerLevel#getBlockState, which can synchronously enter ServerChunkCache /
 * C2ME chunk loading and watchdog the main server thread.
 *
 * Do a strict loaded-only lookup for the actual storage position before MCA
 * touches block state. If the FULL chunk is not already visible, skip that
 * position. VillageTaxesManager keeps the tax ItemStacks in storageBuffer, so
 * nothing is lost; MCA can deliver them during a later pass when the chunk is
 * loaded.
 */
@Pseudo
@Mixin(
    targets = "net.conczin.mca.server.world.data.villageComponents.VillageTaxesManager",
    remap = false
)
public abstract class McaVillageTaxesChunkGuardMixin {
    @Inject(
        method = "tryToPutIntoInventory",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private void asyncguard$skipUnloadedStorageChunk(
        ServerLevel world,
        BlockPos pos,
        CallbackInfo ci
    ) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (LoadedChunkLookup.getLoadedChunk(world.getChunkSource(), chunkX, chunkZ) == null) {
            ci.cancel();
        }
    }
}
