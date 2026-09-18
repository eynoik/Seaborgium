package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncEntityPosePrep {
    private static final Map<Integer, Entry> CACHE = new ConcurrentHashMap<>();

    private AsyncEntityPosePrep() {
    }

    public static PreparedPose getOrRequest(LivingEntity entity) {
        if (!SeaborgiumConfig.ASYNC_ENTITY_POSE_PREP.get()) {
            return null;
        }

        int id = entity.getId();
        Entry entry = CACHE.computeIfAbsent(id, ignored -> new Entry());
        RawPose raw = capture(entity);

        PreparedPose ready = entry.ready;
        if (ready == null || ready.sourceTick() != raw.tick()) {
            entry.request(raw);
        }

        // One-tick-old immutable pose input is safe to consume for render preparation.
        // The renderer still owns and mutates the actual model on the render thread.
        return ready;
    }

    public static void forget(int entityId) {
        CACHE.remove(entityId);
    }

    private static RawPose capture(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        return new RawPose(
                entity.tickCount,
                entity.xOld,
                entity.yOld,
                entity.zOld,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                Math.max(box.getXsize(), box.getZsize()),
                box.getYsize(),
                entity.yBodyRotO,
                entity.yBodyRot,
                entity.yHeadRotO,
                entity.yHeadRot,
                entity.xRotO,
                entity.getXRot()
        );
    }

    private static PreparedPose prepare(RawPose raw) {
        return new PreparedPose(
                raw.tick(),
                raw.xOld(),
                raw.yOld(),
                raw.zOld(),
                raw.x() - raw.xOld(),
                raw.y() - raw.yOld(),
                raw.z() - raw.zOld(),
                raw.width(),
                raw.height(),
                raw.bodyYawOld(),
                Mth.wrapDegrees(raw.bodyYaw() - raw.bodyYawOld()),
                raw.headYawOld(),
                Mth.wrapDegrees(raw.headYaw() - raw.headYawOld()),
                raw.pitchOld(),
                Mth.wrapDegrees(raw.pitch() - raw.pitchOld())
        );
    }

    public record PreparedPose(
            int sourceTick,
            double xOld,
            double yOld,
            double zOld,
            double dx,
            double dy,
            double dz,
            double width,
            double height,
            float bodyYawOld,
            float bodyYawDelta,
            float headYawOld,
            float headYawDelta,
            float pitchOld,
            float pitchDelta
    ) {
        public double x(float partialTick) {
            return xOld + dx * partialTick;
        }

        public double y(float partialTick) {
            return yOld + dy * partialTick;
        }

        public double z(float partialTick) {
            return zOld + dz * partialTick;
        }

        public float bodyYaw(float partialTick) {
            return bodyYawOld + bodyYawDelta * partialTick;
        }

        public float headYaw(float partialTick) {
            return headYawOld + headYawDelta * partialTick;
        }

        public float pitch(float partialTick) {
            return pitchOld + pitchDelta * partialTick;
        }
    }

    private record RawPose(
            int tick,
            double xOld,
            double yOld,
            double zOld,
            double x,
            double y,
            double z,
            double width,
            double height,
            float bodyYawOld,
            float bodyYaw,
            float headYawOld,
            float headYaw,
            float pitchOld,
            float pitch
    ) {
    }

    private static final class Entry {
        private final AtomicBoolean inFlight = new AtomicBoolean();
        private volatile PreparedPose ready;
        private volatile int requestedTick = Integer.MIN_VALUE;

        private void request(RawPose raw) {
            if (requestedTick == raw.tick() || !inFlight.compareAndSet(false, true)) {
                return;
            }
            requestedTick = raw.tick();

            SeaborgiumJobSystem.executor().execute(() -> {
                try {
                    ready = prepare(raw);
                } finally {
                    inFlight.set(false);
                }
            });
        }
    }
}
