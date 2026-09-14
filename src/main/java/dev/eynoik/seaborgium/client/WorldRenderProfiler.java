package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight render-thread telemetry for terrain section draw passes.
 *
 * Spark can show the aggregate cost of LevelRenderer.renderSectionLayer, but it
 * cannot distinguish the RenderType argument. This profiler records that split
 * so later optimizations can target the actual expensive pass instead of
 * blindly degrading solid, cutout or translucent terrain.
 */
public final class WorldRenderProfiler {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final Map<String, MutableTiming> TIMINGS = new HashMap<>();
    private static final Map<RenderType, String> NAME_CACHE = new IdentityHashMap<>();

    private static long windowStarted = System.nanoTime();
    private static Snapshot latest = Snapshot.EMPTY;

    private WorldRenderProfiler() {
    }

    public static long begin(RenderType renderType) {
        if (!shouldCollect()) {
            return 0L;
        }
        return System.nanoTime();
    }

    public static void end(RenderType renderType, long startedAt) {
        if (startedAt == 0L) {
            return;
        }

        long elapsed = System.nanoTime() - startedAt;
        String name = nameOf(renderType);
        MutableTiming timing = TIMINGS.computeIfAbsent(name, ignored -> new MutableTiming());
        timing.calls++;
        timing.totalNanos += elapsed;
        timing.maxNanos = Math.max(timing.maxNanos, elapsed);
        rotateIfNeeded(System.nanoTime());
    }

    public static Snapshot snapshot() {
        if (!shouldCollect()) {
            return Snapshot.EMPTY;
        }
        rotateIfNeeded(System.nanoTime());
        return latest;
    }

    private static boolean shouldCollect() {
        return SeaborgiumConfig.WORLD_RENDER_TELEMETRY.get()
                && (SeaborgiumConfig.DEBUG_TELEMETRY.get()
                || TimedProfileSession.isActive()
                || AbBenchmarkSession.isActive());
    }

    private static String nameOf(RenderType renderType) {
        String cached = NAME_CACHE.get(renderType);
        if (cached != null) {
            return cached;
        }

        String name = computeName(renderType);
        NAME_CACHE.put(renderType, name);
        return name;
    }

    private static String computeName(RenderType renderType) {
        if (renderType == RenderType.solid()) {
            return "solid";
        }
        if (renderType == RenderType.cutoutMipped()) {
            return "cutoutMipped";
        }
        if (renderType == RenderType.cutout()) {
            return "cutout";
        }
        if (renderType == RenderType.translucent()) {
            return "translucent";
        }
        if (renderType == RenderType.tripwire()) {
            return "tripwire";
        }

        // Veil/Iris can pass wrapped/dynamic RenderType instances instead of the
        // vanilla singletons above. Their toString starts with e.g.
        // "RenderType[solid:CompositeState[...]". Extract only the stable short
        // name so the HUD never prints the complete render-state description.
        String text = renderType.toString();
        int start = text.indexOf('[');
        start = start >= 0 ? start + 1 : 0;

        int colon = text.indexOf(':', start);
        int comma = text.indexOf(',', start);
        int bracket = text.indexOf(']', start);
        int end = text.length();
        if (colon >= start) {
            end = Math.min(end, colon);
        }
        if (comma >= start) {
            end = Math.min(end, comma);
        }
        if (bracket >= start) {
            end = Math.min(end, bracket);
        }

        String candidate = text.substring(start, end).trim();
        if (candidate.isEmpty()) {
            candidate = renderType.getClass().getSimpleName();
        }
        if (candidate.length() > 32) {
            candidate = candidate.substring(0, 29) + "...";
        }
        return candidate;
    }

    private static void rotateIfNeeded(long now) {
        long duration = now - windowStarted;
        if (duration < WINDOW_NANOS) {
            return;
        }

        List<LayerTiming> timings = new ArrayList<>(TIMINGS.size());
        for (Map.Entry<String, MutableTiming> entry : TIMINGS.entrySet()) {
            MutableTiming timing = entry.getValue();
            timings.add(new LayerTiming(entry.getKey(), timing.calls, timing.totalNanos, timing.maxNanos));
        }
        timings.sort(Comparator.comparingLong(LayerTiming::totalNanos).reversed());

        latest = new Snapshot(duration, List.copyOf(timings));
        TIMINGS.clear();
        // Prevent an unbounded cache if a renderer creates temporary RenderType
        // objects. This still means toString is paid at most once per type/window.
        NAME_CACHE.clear();
        windowStarted = now;
    }

    private static final class MutableTiming {
        private int calls;
        private long totalNanos;
        private long maxNanos;
    }

    public record LayerTiming(String name, int calls, long totalNanos, long maxNanos) {
        public double totalMillis() {
            return totalNanos / 1_000_000.0;
        }

        public double averageMillis() {
            return calls == 0 ? 0.0 : totalNanos / (calls * 1_000_000.0);
        }

        public double maxMillis() {
            return maxNanos / 1_000_000.0;
        }
    }

    public record Snapshot(long durationNanos, List<LayerTiming> layers) {
        private static final Snapshot EMPTY = new Snapshot(0L, List.of());
    }
}
