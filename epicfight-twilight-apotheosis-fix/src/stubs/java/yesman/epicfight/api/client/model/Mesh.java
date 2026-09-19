package yesman.epicfight.api.client.model;

import net.minecraft.resources.ResourceLocation;

/**
 * Compile-only Epic Fight shape. Not packaged in the runtime JAR.
 */
public interface Mesh {
    final class RenderProperties {
        public ResourceLocation customTexturePath() {
            return null;
        }
    }
}
