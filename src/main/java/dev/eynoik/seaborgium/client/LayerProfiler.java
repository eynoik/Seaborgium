package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.SeaborgiumConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Render-thread-only telemetry. Timings are sampled instead of wrapping every
 * layer call in two nanoTime calls, so measuring Seaborgium does not become a
 * meaningful renderer cost by itself.
 */
public final class LayerProfiler {
    private static final int SAMPLE_MASK = 63;
    private static final long WINDOW_NANOS = 1_000_000_000L;

    private static final Map<Class<?>, MutableTiming> TIMINGS = new HashMap<>();
    private static final Map<Class<?>, CostModel> COST_MODELS = new HashMap<>();
    private static final Map<Class<?>, Integer> SKIPPED_BY_CLASS = new HashMap<>();
    private static long windowStarted = System.nanoTime();
    private static long sampleSequence;
    private static int evaluated;
    private static int rendered;
    private static int skipped;
    private static int frames;
    private static Snapshot latest = Snapshot.EMPTY;

    private LayerProfiler() {
    }

    public static void recordDecision(Class<?> layerClass, boolean wasRendered) {
        if (!SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
            return;
        }

        evaluated++;
        if (wasRendered) {
            rendered++;
        } else {
            skipped++;
            SKIPPED_BY_CLASS.merge(layerClass, 1, Integer::sum);
        }

        if ((evaluated & 255) == 0) {
            rotateIfNeeded(System.nanoTime());
        }
    }

    public static long beginSample() {
        if (!SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
            return 0L;
        }
        return (sampleSequence++ & SAMPLE_MASK) == 0L ? System.nanoTime() : 0L;
    }

    public static void endSample(Class<?> layerClass, long startedAt) {
        if (startedAt == 0L) {
            return;
        }

        long elapsed = System.nanoTime() - startedAt;
        MutableTiming timing = TIMINGS.computeIfAbsent(layerClass, ignored -> new MutableTiming());
        timing.samples++;
        timing.totalNanos += elapsed;
        timing.maxNanos = Math.max(timing.maxNanos, elapsed);
        COST_MODELS.computeIfAbsent(layerClass, ignored -> new CostModel()).observe(elapsed);
    }

    public static void recordFrame() {
        if (!SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
            return;
        }
        frames++;
        if ((frames & 63) == 0) {
            rotateIfNeeded(System.nanoTime());
        }
    }

    public static Snapshot snapshot() {
        if (!SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
            return Snapshot.EMPTY;
        }
        rotateIfNeeded(System.nanoTime());
        return latest;
    }

    private static void rotateIfNeeded(long now) {
        long duration = now - windowStarted;
        if (duration < WINDOW_NANOS) {
            return;
        }

        List<LayerTiming> timings = new ArrayList<>(TIMINGS.size());
        for (Map.Entry<Class<?>, MutableTiming> entry : TIMINGS.entrySet()) {
            MutableTiming timing = entry.getValue();
            timings.add(new LayerTiming(
                    entry.getKey().getSimpleName(),
                    timing.samples,
                    timing.totalNanos,
                    timing.maxNanos
            ));
        }
        timings.sort(Comparator.comparingLong(LayerTiming::sampledTotalNanos).reversed());

        double estimatedSavedNanos = 0.0;
        int modeledSkipped = 0;
        for (Map.Entry<Class<?>, Integer> entry : SKIPPED_BY_CLASS.entrySet()) {
            CostModel model = COST_MODELS.get(entry.getKey());
            if (model != null && model.observations > 0) {
                estimatedSavedNanos += model.averageNanos * entry.getValue();
                modeledSkipped += entry.getValue();
            }
        }

        latest = new Snapshot(
                evaluated,
                rendered,
                skipped,
                frames,
                duration,
                estimatedSavedNanos,
                modeledSkipped,
                List.copyOf(timings.subList(0, Math.min(3, timings.size())))
        );

        evaluated = 0;
        rendered = 0;
        skipped = 0;
        frames = 0;
        TIMINGS.clear();
        SKIPPED_BY_CLASS.clear();
        windowStarted = now;
    }

    private static final class MutableTiming {
        private int samples;
        private long totalNanos;
        private long maxNanos;
    }

    private static final class CostModel {
        private int observations;
        private double averageNanos;

        private void observe(long nanos) {
            observations++;
            if (observations == 1) {
                averageNanos = nanos;
            } else {
                // An exponential moving average adapts when a layer becomes more
                // expensive without throwing away knowledge during fully skipped windows.
                averageNanos += (nanos - averageNanos) * 0.125;
            }
        }
    }

    public record LayerTiming(String name, int samples, long sampledTotalNanos, long maxNanos) {
        public double averageMicros() {
            return samples == 0 ? 0.0 : sampledTotalNanos / (samples * 1_000.0);
        }

        public double maxMicros() {
            return maxNanos / 1_000.0;
        }
    }

    public record Snapshot(
            int evaluated,
            int rendered,
            int skipped,
            int frames,
            long durationNanos,
            double estimatedSavedNanos,
            int modeledSkipped,
            List<LayerTiming> expensiveLayers
    ) {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0L, 0.0, 0, List.of());

        public double skippedPercent() {
            return evaluated == 0 ? 0.0 : skipped * 100.0 / evaluated;
        }

        public double estimatedSavedMillisPerFrame() {
            return frames == 0 ? 0.0 : estimatedSavedNanos / frames / 1_000_000.0;
        }

        public double modeledPercent() {
            return skipped == 0 ? 0.0 : modeledSkipped * 100.0 / skipped;
        }
    }
}
