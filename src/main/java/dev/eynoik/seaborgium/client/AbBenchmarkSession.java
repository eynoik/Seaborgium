package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Alternates the layer budget off/on and compares real client frame intervals. */
public final class AbBenchmarkSession {
    // ABBA cancels much of the linear scene drift while avoiding a permanent
    // odd/even-frame bias: OFF, ON, ON, OFF, then repeat.
    private static final boolean[] FRAME_PATTERN = {false, true, true, false};
    private static final long STATIC_BLOCK_NANOS = 10_000_000_000L;
    private static final long STATIC_WARMUP_NANOS = 2_000_000_000L;
    private static final long INVALID_FRAME_NANOS = 2_000_000_000L;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static Session active;

    private AbBenchmarkSession() {
    }

    public static boolean start(int seconds) {
        if (seconds % 20 != 0) {
            message("Seaborgium: static benchmark duration must be divisible by 20 seconds.", 0xFFFFAA00);
            return false;
        }
        return start(seconds, Mode.STATIC_BLOCKS);
    }

    public static boolean startPlay(int seconds) {
        return start(seconds, Mode.PLAY_ABBA);
    }

    private static boolean start(int seconds, Mode mode) {
        if (active != null) {
            message("Seaborgium: an A/B benchmark is already running (" + remainingSeconds() + " s left).", 0xFFFFAA00);
            return false;
        }
        if (TimedProfileSession.isActive()) {
            message("Seaborgium: stop the timed profile before starting an A/B benchmark.", 0xFFFFAA00);
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            message("Seaborgium: join a world before starting a benchmark.", 0xFFFF5555);
            return false;
        }

        long now = System.nanoTime();
        active = new Session(seconds, now, now + seconds * 1_000_000_000L, mode);
        LayerBudget.setBenchmarkEnabledOverride(false);
        if (mode == Mode.STATIC_BLOCKS) {
            message("Seaborgium: static A/B started. Keep the camera fixed; warmup frames are excluded.", 0xFF55FF55);
        } else {
            message("Seaborgium: gameplay A/B started (OFF-ON-ON-OFF).", 0xFF55FF55);
        }
        return true;
    }

    public static boolean stop() {
        if (active == null) {
            message("Seaborgium: no A/B benchmark is running.", 0xFFFFAA00);
            return false;
        }
        finish("stopped by user");
        return true;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static boolean isOptimizationEnabled() {
        return active != null && active.optimizationEnabled;
    }

    public static String statusLabel() {
        return active == null || active.mode == Mode.STATIC_BLOCKS ? "STATIC" : "PLAY";
    }

    public static int remainingSeconds() {
        if (active == null) {
            return 0;
        }
        long remaining = Math.max(0L, active.deadlineNanos - System.nanoTime());
        return (int) Math.ceil(remaining / 1_000_000_000.0);
    }

    static void recordDecision(Class<?> layerClass, boolean rendered) {
        Session session = active;
        if (session == null || !session.collecting) {
            return;
        }
        PhaseData phase = session.currentPhase();
        LayerStats stats = phase.layers.computeIfAbsent(layerClass, ignored -> new LayerStats());
        phase.evaluated++;
        if (rendered) {
            phase.rendered++;
            stats.rendered++;
        } else {
            phase.skipped++;
            stats.skipped++;
        }
    }

    static void recordSample(Class<?> layerClass, long elapsedNanos) {
        Session session = active;
        if (session == null || !session.collecting) {
            return;
        }
        LayerStats stats = session.currentPhase().layers.computeIfAbsent(layerClass, ignored -> new LayerStats());
        stats.samples++;
        stats.sampledNanos += elapsedNanos;
        stats.maxSampleNanos = Math.max(stats.maxSampleNanos, elapsedNanos);
    }

    static void recordEntityRender(Class<?> rendererClass) {
        Session session = active;
        if (session == null || !session.collecting) {
            return;
        }
        EntityStats stats = session.currentPhase().entityRenderers.computeIfAbsent(
                rendererClass,
                ignored -> new EntityStats()
        );
        stats.calls++;
    }

    static void recordEntitySample(Class<?> rendererClass, long elapsedNanos) {
        Session session = active;
        if (session == null || !session.collecting) {
            return;
        }
        EntityStats stats = session.currentPhase().entityRenderers.computeIfAbsent(
                rendererClass,
                ignored -> new EntityStats()
        );
        stats.samples++;
        stats.sampledNanos += elapsedNanos;
        stats.maxSampleNanos = Math.max(stats.maxSampleNanos, elapsedNanos);
    }

    static void recordFrame() {
        Session session = active;
        if (session == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            finish("world closed");
            return;
        }

        long now = System.nanoTime();
        if (session.lastFrameNanos != 0L) {
            long frameNanos = now - session.lastFrameNanos;
            if (session.collecting && frameNanos > 0L && frameNanos < INVALID_FRAME_NANOS) {
                session.currentPhase().frameTimes.add(frameNanos);
            } else if (session.collecting) {
                session.currentPhase().discardedFrames++;
            } else {
                session.currentPhase().warmupFrames++;
            }
        }
        session.lastFrameNanos = now;

        if (now >= session.deadlineNanos) {
            finish("completed");
            return;
        }

        if (session.mode == Mode.PLAY_ABBA) {
            session.patternIndex = (session.patternIndex + 1) % FRAME_PATTERN.length;
            setNextFrameMode(session, FRAME_PATTERN[session.patternIndex]);
        } else {
            if (now >= session.blockDeadlineNanos) {
                session.blockStartedNanos = now;
                session.blockDeadlineNanos = now + STATIC_BLOCK_NANOS;
                setNextFrameMode(session, !session.optimizationEnabled);
            }
            session.collecting = now - session.blockStartedNanos >= STATIC_WARMUP_NANOS;
        }
    }

    private static void setNextFrameMode(Session session, boolean enabled) {
        if (enabled != session.optimizationEnabled) {
            session.phaseSwitches++;
        }
        session.optimizationEnabled = enabled;
        LayerBudget.setBenchmarkEnabledOverride(enabled);
    }

    private static void finish(String status) {
        Session session = active;
        if (session == null) {
            return;
        }
        active = null;
        LayerBudget.setBenchmarkEnabledOverride(null);

        long finishedAt = System.nanoTime();
        try {
            Path report = writeReport(session, finishedAt, status);
            message("Seaborgium: A/B benchmark " + status + ". Report: " + report.toAbsolutePath(), 0xFF55FF55);
        } catch (IOException exception) {
            message("Seaborgium: benchmark ended, but the report could not be written: " + exception.getMessage(), 0xFFFF5555);
        }
    }

    private static Path writeReport(Session session, long finishedAt, String status) throws IOException {
        Metrics off = Metrics.calculate(session.off.frameTimes);
        Metrics on = Metrics.calculate(session.on.frameTimes);
        RendererEstimate rendererOff = RendererEstimate.calculate(session.off);
        RendererEstimate rendererOn = RendererEstimate.calculate(session.on);
        List<LayerRow> rows = buildLayerRows(session);
        double measuredLayerOffNanos = rows.stream().mapToDouble(LayerRow::estimatedOffNanos).sum();
        double measuredSeconds = Math.max(0.001, (finishedAt - session.startedNanos) / 1_000_000_000.0);

        StringBuilder report = new StringBuilder(8192);
        report.append("Seaborgium A/B benchmark\n")
                .append("========================\n")
                .append("Status: ").append(status).append('\n')
                .append(String.format(Locale.ROOT, "Requested duration: %d s%n", session.requestedSeconds))
                .append(String.format(Locale.ROOT, "Measured duration: %.3f s%n", measuredSeconds))
                .append("Mode: ").append(session.mode.reportName).append('\n')
                .append(session.mode == Mode.STATIC_BLOCKS
                        ? "Pattern: 10 s blocks with the first 2 s excluded as warmup\n"
                        : "Frame pattern: OFF, ON, ON, OFF (ABBA)\n")
                .append(String.format(Locale.ROOT, "Mode switches: %d%n", session.phaseSwitches))
                .append('\n')
                .append("Actual frame-time comparison\n")
                .append("----------------------------\n")
                .append("metric                 | budget OFF | budget ON | ON difference\n")
                .append(String.format(Locale.ROOT, "average FPS            | %10.2f | %9.2f | %+8.2f (%+.2f%%)%n",
                        off.averageFps, on.averageFps, on.averageFps - off.averageFps,
                        percentDifference(on.averageFps, off.averageFps)))
                .append(String.format(Locale.ROOT, "1%% low FPS             | %10.2f | %9.2f | %+8.2f (%+.2f%%)%n",
                        off.onePercentLowFps, on.onePercentLowFps, on.onePercentLowFps - off.onePercentLowFps,
                        percentDifference(on.onePercentLowFps, off.onePercentLowFps)))
                .append(String.format(Locale.ROOT, "average frame time ms  | %10.3f | %9.3f | %+8.3f%n",
                        off.averageMillis, on.averageMillis, on.averageMillis - off.averageMillis))
                .append(String.format(Locale.ROOT, "median frame time ms   | %10.3f | %9.3f | %+8.3f%n",
                        off.medianMillis, on.medianMillis, on.medianMillis - off.medianMillis))
                .append(String.format(Locale.ROOT, "p95 frame time ms      | %10.3f | %9.3f | %+8.3f%n",
                        off.p95Millis, on.p95Millis, on.p95Millis - off.p95Millis))
                .append(String.format(Locale.ROOT, "p99 frame time ms      | %10.3f | %9.3f | %+8.3f%n",
                        off.p99Millis, on.p99Millis, on.p99Millis - off.p99Millis))
                .append(String.format(Locale.ROOT, "measured frames        | %10d | %9d |%n", off.frames, on.frames))
                .append(String.format(Locale.ROOT, "discarded intervals    | %10d | %9d |%n",
                        session.off.discardedFrames, session.on.discardedFrames))
                .append(String.format(Locale.ROOT, "warmup intervals       | %10d | %9d |%n",
                        session.off.warmupFrames, session.on.warmupFrames))
                .append('\n')
                .append("Whole living-entity renderer (sampled)\n")
                .append("--------------------------------------\n")
                .append("metric                 | budget OFF | budget ON | ON difference\n")
                .append(String.format(Locale.ROOT, "render calls           | %10d | %9d |%n",
                        rendererOff.calls, rendererOn.calls))
                .append(String.format(Locale.ROOT, "timing samples         | %10d | %9d |%n",
                        rendererOff.samples, rendererOn.samples))
                .append(String.format(Locale.ROOT, "estimated ms/frame     | %10.3f | %9.3f | %+8.3f (%+.2f%%)%n",
                        rendererOff.estimatedMillisPerFrame(off.frames),
                        rendererOn.estimatedMillisPerFrame(on.frames),
                        rendererOn.estimatedMillisPerFrame(on.frames) - rendererOff.estimatedMillisPerFrame(off.frames),
                        percentDifference(rendererOn.averageNanos, rendererOff.averageNanos)))
                .append(String.format(Locale.ROOT, "average render us      | %10.2f | %9.2f | %+8.2f%n",
                        rendererOff.averageNanos / 1_000.0,
                        rendererOn.averageNanos / 1_000.0,
                        (rendererOn.averageNanos - rendererOff.averageNanos) / 1_000.0))
                .append(String.format(Locale.ROOT, "sampled layer share    | %9.2f%% |         - |%n",
                        rendererOff.estimatedTotalNanos == 0.0
                                ? 0.0
                                : measuredLayerOffNanos * 100.0 / rendererOff.estimatedTotalNanos))
                .append('\n')
                .append("Layer calls\n")
                .append("-----------\n")
                .append(String.format(Locale.ROOT, "Budget OFF: evaluated=%d rendered=%d skipped=%d%n",
                        session.off.evaluated, session.off.rendered, session.off.skipped))
                .append(String.format(Locale.ROOT, "Budget ON:  evaluated=%d rendered=%d skipped=%d (%.1f%%)%n",
                        session.on.evaluated, session.on.rendered, session.on.skipped,
                        percent(session.on.skipped, session.on.evaluated)))
                .append('\n')
                .append("Configuration used while budget was ON\n")
                .append("--------------------------------------\n")
                .append("baseOnlyBelowPixels: ").append(SeaborgiumConfig.BASE_ONLY_BELOW_PIXELS.get()).append('\n')
                .append("essentialOnlyBelowPixels: ").append(SeaborgiumConfig.ESSENTIAL_ONLY_BELOW_PIXELS.get()).append('\n')
                .append("reducedBelowPixels: ").append(SeaborgiumConfig.REDUCED_BELOW_PIXELS.get()).append('\n')
                .append('\n')
                .append("Top layer costs measured during budget-OFF phases\n")
                .append("------------------------------------------------\n")
                .append("off_calls | on_rendered | on_skipped | samples | avg_us | est_off_ms | class\n");

        for (LayerRow row : rows.subList(0, Math.min(40, rows.size()))) {
            report.append(String.format(Locale.ROOT, "%9d | %11d | %10d | %7d | %6.2f | %10.3f | %s%n",
                    row.offRendered,
                    row.onRendered,
                    row.onSkipped,
                    row.samples,
                    row.averageNanos / 1_000.0,
                    row.estimatedOffNanos / 1_000_000.0,
                    row.layerClass.getName()));
        }

        report.append('\n')
                .append("Top whole-renderer costs during budget-OFF measurement\n")
                .append("------------------------------------------------------\n")
                .append("off_calls | on_calls | off_samples | off_avg_us | on_avg_us | renderer\n");
        List<EntityRow> entityRows = buildEntityRows(session);
        for (EntityRow row : entityRows.subList(0, Math.min(30, entityRows.size()))) {
            report.append(String.format(Locale.ROOT, "%9d | %8d | %11d | %10.2f | %9.2f | %s%n",
                    row.off.calls,
                    row.on.calls,
                    row.off.samples,
                    row.off.averageNanos() / 1_000.0,
                    row.on.averageNanos() / 1_000.0,
                    row.rendererClass.getName()));
        }

        report.append('\n')
                .append("Notes\n")
                .append("-----\n")
                .append(session.mode == Mode.STATIC_BLOCKS
                        ? "Static mode uses long blocks so GPU queues settle; warmup intervals are excluded.\n"
                        : "Gameplay mode uses ABBA to reduce linear scene drift and odd/even-frame bias.\n")
                .append("Static mode requires a fixed camera; gameplay mode is useful only as a secondary test.\n")
                .append("FPS caps, VSync, chunk generation, menus and camera movement can hide or distort the result.\n")
                .append("The existing sampled layer profiler remains active during both phases (1 in 64 rendered calls).\n")
                .append("The original enabled config value is restored automatically when the benchmark ends.\n");

        Path directory = FMLPaths.GAMEDIR.get().resolve("seaborgium-reports");
        Files.createDirectories(directory);
        Path output = directory.resolve("seaborgium-benchmark-" + session.mode.fileName + "-"
                + LocalDateTime.now().format(FILE_TIME) + ".txt");
        Files.writeString(output, report, StandardCharsets.UTF_8);
        return output;
    }

    private static List<LayerRow> buildLayerRows(Session session) {
        List<LayerRow> rows = new ArrayList<>();
        for (Map.Entry<Class<?>, LayerStats> entry : session.off.layers.entrySet()) {
            Class<?> layerClass = entry.getKey();
            LayerStats off = entry.getValue();
            LayerStats on = session.on.layers.getOrDefault(layerClass, LayerStats.EMPTY);
            double averageNanos = off.samples == 0
                    ? LayerProfiler.modeledAverageNanos(layerClass)
                    : off.sampledNanos / (double) off.samples;
            if (Double.isNaN(averageNanos)) {
                averageNanos = 0.0;
            }
            rows.add(new LayerRow(
                    layerClass,
                    off.rendered,
                    on.rendered,
                    on.skipped,
                    off.samples,
                    averageNanos,
                    averageNanos * off.rendered
            ));
        }
        rows.sort(Comparator.comparingDouble(LayerRow::estimatedOffNanos).reversed());
        return rows;
    }

    private static List<EntityRow> buildEntityRows(Session session) {
        List<EntityRow> rows = new ArrayList<>();
        for (Map.Entry<Class<?>, EntityStats> entry : session.off.entityRenderers.entrySet()) {
            EntityStats on = session.on.entityRenderers.getOrDefault(entry.getKey(), EntityStats.EMPTY);
            rows.add(new EntityRow(entry.getKey(), entry.getValue(), on));
        }
        rows.sort(Comparator.comparingDouble((EntityRow row) -> row.off.estimatedTotalNanos()).reversed());
        return rows;
    }

    private static double percent(int part, int whole) {
        return whole == 0 ? 0.0 : part * 100.0 / whole;
    }

    private static double percentDifference(double value, double baseline) {
        return baseline == 0.0 ? 0.0 : (value / baseline - 1.0) * 100.0;
    }

    private static void message(String text, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(text).withColor(color), false);
        }
    }

    private static final class Session {
        private final int requestedSeconds;
        private final long startedNanos;
        private final long deadlineNanos;
        private final Mode mode;
        private long blockStartedNanos;
        private long blockDeadlineNanos;
        private long lastFrameNanos;
        private boolean optimizationEnabled;
        private boolean collecting;
        private int patternIndex;
        private int phaseSwitches;
        private final PhaseData off = new PhaseData();
        private final PhaseData on = new PhaseData();

        private Session(int requestedSeconds, long startedNanos, long deadlineNanos, Mode mode) {
            this.requestedSeconds = requestedSeconds;
            this.startedNanos = startedNanos;
            this.deadlineNanos = deadlineNanos;
            this.mode = mode;
            this.blockStartedNanos = startedNanos;
            this.blockDeadlineNanos = startedNanos + STATIC_BLOCK_NANOS;
            this.collecting = mode == Mode.PLAY_ABBA;
        }

        private PhaseData currentPhase() {
            return optimizationEnabled ? on : off;
        }
    }

    private static final class PhaseData {
        private final List<Long> frameTimes = new ArrayList<>();
        private final Map<Class<?>, LayerStats> layers = new HashMap<>();
        private final Map<Class<?>, EntityStats> entityRenderers = new HashMap<>();
        private int evaluated;
        private int rendered;
        private int skipped;
        private int discardedFrames;
        private int warmupFrames;
    }

    private static final class LayerStats {
        private static final LayerStats EMPTY = new LayerStats();
        private int rendered;
        private int skipped;
        private int samples;
        private long sampledNanos;
        private long maxSampleNanos;
    }

    private static final class EntityStats {
        private static final EntityStats EMPTY = new EntityStats();
        private int calls;
        private int samples;
        private long sampledNanos;
        private long maxSampleNanos;

        private double averageNanos() {
            return samples == 0 ? 0.0 : sampledNanos / (double) samples;
        }

        private double estimatedTotalNanos() {
            return averageNanos() * calls;
        }
    }

    private record LayerRow(Class<?> layerClass, int offRendered, int onRendered, int onSkipped,
                            int samples, double averageNanos, double estimatedOffNanos) {
    }

    private record EntityRow(Class<?> rendererClass, EntityStats off, EntityStats on) {
    }

    private record RendererEstimate(int calls, int samples, double averageNanos, double estimatedTotalNanos) {
        private static RendererEstimate calculate(PhaseData phase) {
            int calls = 0;
            int samples = 0;
            double estimatedTotal = 0.0;
            for (EntityStats stats : phase.entityRenderers.values()) {
                calls += stats.calls;
                samples += stats.samples;
                estimatedTotal += stats.estimatedTotalNanos();
            }
            double average = calls == 0 ? 0.0 : estimatedTotal / calls;
            return new RendererEstimate(calls, samples, average, estimatedTotal);
        }

        private double estimatedMillisPerFrame(int frames) {
            return frames == 0 ? 0.0 : estimatedTotalNanos / frames / 1_000_000.0;
        }
    }

    private enum Mode {
        STATIC_BLOCKS("static-blocks", "static"),
        PLAY_ABBA("gameplay-abba", "play");

        private final String reportName;
        private final String fileName;

        Mode(String reportName, String fileName) {
            this.reportName = reportName;
            this.fileName = fileName;
        }
    }

    private record Metrics(int frames, double averageFps, double onePercentLowFps, double averageMillis,
                           double medianMillis, double p95Millis, double p99Millis) {
        private static Metrics calculate(List<Long> frameTimes) {
            if (frameTimes.isEmpty()) {
                return new Metrics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }

            long[] sorted = new long[frameTimes.size()];
            long total = 0L;
            for (int index = 0; index < sorted.length; index++) {
                sorted[index] = frameTimes.get(index);
                total += sorted[index];
            }
            Arrays.sort(sorted);

            double averageNanos = total / (double) sorted.length;
            int worstCount = Math.max(1, (int) Math.ceil(sorted.length * 0.01));
            long worstTotal = 0L;
            for (int index = sorted.length - worstCount; index < sorted.length; index++) {
                worstTotal += sorted[index];
            }
            double worstAverageNanos = worstTotal / (double) worstCount;

            return new Metrics(
                    sorted.length,
                    1_000_000_000.0 / averageNanos,
                    1_000_000_000.0 / worstAverageNanos,
                    averageNanos / 1_000_000.0,
                    percentile(sorted, 0.50) / 1_000_000.0,
                    percentile(sorted, 0.95) / 1_000_000.0,
                    percentile(sorted, 0.99) / 1_000_000.0
            );
        }

        private static long percentile(long[] sorted, double percentile) {
            int index = (int) Math.ceil(percentile * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
        }
    }
}
