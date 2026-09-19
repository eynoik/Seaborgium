package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.world.entity.LivingEntity;

/**
 * Compile-only shape used by external mixins. This class is not included in the produced mod JAR.
 * The real class is provided by Epic Fight at runtime.
 */
public abstract class LivingEntityPatch<T extends LivingEntity> {
    public abstract void setYRot(float yRot);
    public abstract T getOriginal();
}
