package dev.eynoik.asyncsablefix.mixin;

import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protects AttributeMap dirty sets from concurrent Async entity ticks.
 *
 * Lithium replaces these sets with fastutil ReferenceOpenHashSet, which is not
 * safe when one Async worker marks an attribute dirty while the server thread
 * iterates the sync set to build ClientboundUpdateAttributesPacket.
 *
 * Normal entity ticking remains asynchronous. Only dirty-set bookkeeping and
 * snapshot handoff are serialized.
 */
@Mixin(targets = "net.minecraft.world.entity.ai.attributes.AttributeMap", priority = 900, remap = false)
public abstract class AttributeMapThreadSafetyMixin {
    @Shadow @Final
    private Set<AttributeInstance> attributesToSync;

    @Shadow @Final
    private Set<AttributeInstance> attributesToUpdate;

    @Unique
    private final Object asyncsablefix$attributeSyncLock = new Object();

    @Redirect(
        method = "onAttributeModified(Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", ordinal = 0),
        remap = false,
        require = 1
    )
    private boolean asyncsablefix$lockDirtyUpdateAdd(Set<AttributeInstance> set, Object value) {
        synchronized (this.asyncsablefix$attributeSyncLock) {
            return set.add((AttributeInstance) value);
        }
    }

    @Redirect(
        method = "onAttributeModified(Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", ordinal = 1),
        remap = false,
        require = 1
    )
    private boolean asyncsablefix$lockDirtySyncAdd(Set<AttributeInstance> set, Object value) {
        synchronized (this.asyncsablefix$attributeSyncLock) {
            return set.add((AttributeInstance) value);
        }
    }

    @Inject(
        method = "getAttributesToSync()Ljava/util/Set;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private void asyncsablefix$snapshotAttributesToSync(CallbackInfoReturnable<Set<AttributeInstance>> cir) {
        synchronized (this.asyncsablefix$attributeSyncLock) {
            cir.setReturnValue(this.asyncsablefix$snapshotAndClear(this.attributesToSync));
        }
    }

    @Inject(
        method = "getAttributesToUpdate()Ljava/util/Set;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private void asyncsablefix$snapshotAttributesToUpdate(CallbackInfoReturnable<Set<AttributeInstance>> cir) {
        synchronized (this.asyncsablefix$attributeSyncLock) {
            cir.setReturnValue(this.asyncsablefix$snapshotAndClear(this.attributesToUpdate));
        }
    }

    @Unique
    private Set<AttributeInstance> asyncsablefix$snapshotAndClear(Set<AttributeInstance> source) {
        Set<AttributeInstance> snapshot = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        snapshot.addAll(source);
        source.clear();
        return snapshot;
    }
}
