package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Makes a cheap screen-space estimate and decides whether a secondary entity
 * render layer is worth its cost. This class is render-thread only.
 */
public final class LayerBudget {
    private static final double MIN_DISTANCE_SQUARED = 0.25;
    private static final Map<Entity, Double> FRAME_PIXEL_AREAS = new IdentityHashMap<>();
    private static Boolean benchmarkEnabledOverride;
    private static final ClassValue<String> LOWERCASE_CLASS_NAMES = new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
            return type.getName().toLowerCase(Locale.ROOT);
        }
    };

    private LayerBudget() {
    }

    public static boolean shouldRender(RenderLayer<?, ?> layer, Entity entity, float partialTick) {
        boolean enabled = benchmarkEnabledOverride != null
                ? benchmarkEnabledOverride
                : SeaborgiumConfig.ENABLED.get();
        if (!enabled) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity == minecraft.getCameraEntity()) {
            return true;
        }

        // LivingEntityRenderer asks once per layer, often dozens of times for the
        // same entity. Projection is invariant during a frame, so calculate it once.
        double pixelArea = FRAME_PIXEL_AREAS.computeIfAbsent(
                entity,
                ignored -> estimatePixelArea(minecraft, entity, partialTick)
        );
        if (!Double.isFinite(pixelArea)) {
            return true;
        }

        if (pixelArea < SeaborgiumConfig.BASE_ONLY_BELOW_PIXELS.get()) {
            return false;
        }

        String layerName = LOWERCASE_CLASS_NAMES.get(layer.getClass());
        if (containsAny(layerName, SeaborgiumConfig.ALWAYS_RENDER_LAYER_KEYWORDS.get())) {
            return true;
        }

        if (pixelArea < SeaborgiumConfig.ESSENTIAL_ONLY_BELOW_PIXELS.get()) {
            return false;
        }

        return pixelArea >= SeaborgiumConfig.REDUCED_BELOW_PIXELS.get()
                || !containsAny(layerName, SeaborgiumConfig.COSMETIC_LAYER_KEYWORDS.get());
    }

    static void finishFrame() {
        FRAME_PIXEL_AREAS.clear();
    }

    static void setBenchmarkEnabledOverride(Boolean enabled) {
        benchmarkEnabledOverride = enabled;
        FRAME_PIXEL_AREAS.clear();
    }

    static double estimatePixelArea(Minecraft minecraft, Entity entity, float partialTick) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();

        AABB bounds = entity.getBoundingBox();
        double width = Math.max(bounds.getXsize(), bounds.getZsize());
        double height = bounds.getYsize();
        if (width <= 0.0 || height <= 0.0) {
            return 0.0;
        }

        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY()) + height * 0.5;
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        double distanceSquared = cameraPosition.distanceToSqr(entityX, entityY, entityZ);
        if (distanceSquared <= MIN_DISTANCE_SQUARED) {
            return Double.POSITIVE_INFINITY;
        }

        int viewportHeight = minecraft.getWindow().getHeight();
        double fovRadians = Math.toRadians(minecraft.options.fov().get());
        double focalLengthPixels = viewportHeight / (2.0 * Math.tan(fovRadians * 0.5));
        double pixelsPerBlock = focalLengthPixels / Math.sqrt(distanceSquared);

        return width * height * pixelsPerBlock * pixelsPerBlock;
    }

    private static boolean containsAny(String className, List<? extends String> keywords) {
        for (String keyword : keywords) {
            if (!keyword.isBlank() && className.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
