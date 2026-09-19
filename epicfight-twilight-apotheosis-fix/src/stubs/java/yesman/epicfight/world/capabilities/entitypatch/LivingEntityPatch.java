package yesman.epicfight.world.capabilities.entitypatch;

/**
 * Compile-only shape used by legacy mixins. This class is not included in the produced mod JAR.
 *
 * Do NOT add getOriginal() here. Epic Fight inherits it from EntityPatch<T extends Entity>, so its
 * JVM descriptor returns net.minecraft.world.entity.Entity. An unbounded generic stub would erase
 * the return type to Object and can compile an invalid invokevirtual descriptor.
 */
public abstract class LivingEntityPatch<T> {
    public abstract void setYRot(float yRot);
}
