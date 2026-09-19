package yesman.epicfight.world.capabilities.entitypatch;

/**
 * Compile-only shape used by external mixins. This class is not included in the produced mod JAR.
 * The real class is provided by Epic Fight at runtime.
 *
 * T intentionally has no Minecraft bound here because the isolated stub source set has no Minecraft
 * compile classpath. Generic erasure still matches Epic Fight's runtime getOriginal() descriptor.
 */
public abstract class LivingEntityPatch<T> {
    public abstract void setYRot(float yRot);
    public abstract T getOriginal();
}
