package yesman.epicfight.world.capabilities.entitypatch;

/**
 * Compile-only shape used by the external mixin. This class is not included in the produced mod JAR.
 * The real class is provided by Epic Fight at runtime.
 */
public abstract class LivingEntityPatch<T> {
    public abstract void setYRot(float yRot);
}
