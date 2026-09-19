package dev.eynoik.asyncsablefix.mixin;

import java.util.concurrent.locks.ReentrantLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SableSubLevelCollisionMixin {
    /**
     * Sable 2.0.5 keeps mutable collision scratch state per Level. Async may execute
     * Entity#move for several entities at once, so overlapping calls to Sable's
     * collision routine can corrupt that shared state and produce absurd bounds,
     * missing floor collisions and broken sub-level tracking.
     *
     * Keep Async for the rest of the entity tick, but restore vanilla-style
     * serialization for this one Sable critical section. The lock is reentrant so
     * an accidental nested collision call on the same thread cannot self-deadlock.
     */
    @Unique
    private static final ReentrantLock ASYNCSABLEFIX_COLLISION_LOCK = new ReentrantLock();

    @Inject(method = "collide", at = @At("HEAD"), remap = false, require = 1)
    private static void asyncsablefix$enterSerializedCollision(CallbackInfoReturnable<?> cir) {
        ASYNCSABLEFIX_COLLISION_LOCK.lock();
    }

    @Inject(method = "collide", at = @At("RETURN"), remap = false, require = 1)
    private static void asyncsablefix$leaveSerializedCollision(CallbackInfoReturnable<?> cir) {
        ASYNCSABLEFIX_COLLISION_LOCK.unlock();
    }
}
