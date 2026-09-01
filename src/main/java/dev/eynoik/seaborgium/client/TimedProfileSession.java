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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Collects a bounded, user-triggered profiling run and writes a portable text report. */
public final class TimedProfileSession {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static Session active;

    private TimedProfileSession() {
    }

    public static boolean start(int seconds) {
        if (AbBenchmarkSession.isActive()) {
            message("Seaborgium: stop the A/B benchmark before starting a profile.", 0xFFFFAA00);
            return false;
        }
        if (active != null) {
            message("Seaborgium: a profile is already running (" + remainingSeconds() + " s left).", 0xFFFFAA00);
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            message("Seaborgium: join a world before starting a profile.", 0xFFFF5555);
            return false;
        }

        long now = System.nanoTime();
        active = new Session(seconds, now, now + seconds * 1_000_000_000L);
        message("Seaborgium: profiling started for " + seconds + " seconds. Play normally.", 0xFF55FF55);
        return true;
    }

    public static boolean stop() {
        if (active == null) {
            message("Seaborgium: no timed profile is running.", 0xFFFFAA00);
            return false;
        }
        finish("stopped by user");
        return true;
    }

    public static boolean isActive() {
        return active != null;
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
        if (session == null) {
            return;
        }
        LayerStats stats = session.layers.computeIfAbsent(layerClass, ignored -> new LayerStats());
        session.evaluated++;
        if (rendered) {
            session.rendered++;
            stats.rendered++;
        } else {
            session.skipped++;
            stats.skipped++;
        }
    }

    static void recordSample(Class<?> layerClass, long elapsedNanos) {
        Session session = active;
        if (session == null) {
            return;
        }
        LayerStats stats = session.layers.computeIfAbsent(layerClass, ignored -> new LayerStats());
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

        session.frames++;
        if (System.nanoTime() >= session.deadlineNanos) {
            finish("completed");
        }
    }

    private static void finish(String status) {
        Session session = active;
        if (session == null) {
            return;
        }
        active = null;

        long finishedAt = System.nanoTime();
        try {
            Path report = writeReport(session, finishedAt, status);
            message("Seaborgium: profile " + status + ". Report: " + report.toAbsolutePath(), 0xFF55FF55);
        } catch (IOException exception) {
            message("Seaborgium: profile ended, but the report could not be written: " + exception.getMessage(), 0xFFFF5555);
        }
    }

    private static Path writeReport(Session session, long finishedAt, String status) throws IOException {
        double durationSeconds = Math.max(0.001, (finishedAt - session.startedNanos) / 1_000_000_000.0);
        List<ReportRow> rows = new ArrayList<>(session.layers.size());
        double estimatedSavedNanos = 0.0;
        int modeledSkipped = 0;

        for (Map.Entry<Class<?>, LayerStats> entry : session.layers.entrySet()) {
            LayerStats stats = entry.getValue();
            double modelNanos = LayerProfiler.modeledAverageNanos(entry.getKey());
            double savedNanos = Double.isNaN(modelNanos) ? 0.0 : modelNanos * stats.skipped;
            if (!Double.isNaN(modelNanos)) {
                modeledSkipped += stats.skipped;
            }
            estimatedSavedNanos += savedNanos;
            rows.add(new ReportRow(entry.getKey(), stats, modelNanos, savedNanos));
        }
        rows.sort(Comparator.comparingDouble(ReportRow::estimatedSavedNanos)
                .thenComparingInt(row -> row.stats.skipped)
                .reversed());

        StringBuilder report = new StringBuilder(4096);
        report.append("Seaborgium timed profile\n")
                .append("========================\n")
                .append("Status: ").append(status).append('\n')
                .append(String.format(Locale.ROOT, "Requested duration: %d s%n", session.requestedSeconds))
                .append(String.format(Locale.ROOT, "Measured duration: %.3f s%n", durationSeconds))
                .append(String.format(Locale.ROOT, "Frames: %d%n", session.frames))
                .append(String.format(Locale.ROOT, "Average FPS: %.1f%n", session.frames / durationSeconds))
                .append('\n')
                .append("Layer budget\n")
                .append("------------\n")
                .append(String.format(Locale.ROOT, "Evaluated calls: %d%n", session.evaluated))
                .append(String.format(Locale.ROOT, "Rendered calls: %d%n", session.rendered))
                .append(String.format(Locale.ROOT, "Skipped calls: %d (%.1f%%)%n",
                        session.skipped, percent(session.skipped, session.evaluated)))
                .append(String.format(Locale.ROOT, "Estimated CPU saved: %.3f ms total%n", estimatedSavedNanos / 1_000_000.0))
                .append(String.format(Locale.ROOT, "Estimated CPU saved: %.4f ms/frame%n",
                        session.frames == 0 ? 0.0 : estimatedSavedNanos / session.frames / 1_000_000.0))
                .append(String.format(Locale.ROOT, "Modeled skipped calls: %d (%.1f%%)%n",
                        modeledSkipped, percent(modeledSkipped, session.skipped)))
                .append('\n')
                .append("Configuration\n")
                .append("-------------\n")
                .append("enabled: ").append(SeaborgiumConfig.ENABLED.get()).append('\n')
                .append("baseOnlyBelowPixels: ").append(SeaborgiumConfig.BASE_ONLY_BELOW_PIXELS.get()).append('\n')
                .append("essentialOnlyBelowPixels: ").append(SeaborgiumConfig.ESSENTIAL_ONLY_BELOW_PIXELS.get()).append('\n')
                .append("reducedBelowPixels: ").append(SeaborgiumConfig.REDUCED_BELOW_PIXELS.get()).append('\n')
                .append("alwaysRenderLayerKeywords: ").append(SeaborgiumConfig.ALWAYS_RENDER_LAYER_KEYWORDS.get()).append('\n')
                .append("cosmeticLayerKeywords: ").append(SeaborgiumConfig.COSMETIC_LAYER_KEYWORDS.get()).append('\n')
                .append('\n')
                .append("Per-layer results (sorted by estimated saved CPU time)\n")
                .append("------------------------------------------------------\n")
                .append("rendered | skipped | samples | avg_us | max_us | est_saved_ms | class\n");

        for (ReportRow row : rows) {
            double averageSampleMicros = row.stats.samples == 0
                    ? 0.0
                    : row.stats.sampledNanos / row.stats.samples / 1_000.0;
            report.append(String.format(Locale.ROOT, "%8d | %7d | %7d | %6.2f | %6.2f | %12.3f | %s%n",
                    row.stats.rendered,
                    row.stats.skipped,
                    row.stats.samples,
                    averageSampleMicros,
                    row.stats.maxSampleNanos / 1_000.0,
                    row.estimatedSavedNanos / 1_000_000.0,
                    row.layerClass.getName()));
        }

        report.append('\n')
                .append("Notes\n")
                .append("-----\n")
                .append("Timing samples are intentionally sparse (1 in 64 rendered layer calls) to keep profiler overhead low.\n")
                .append("Estimated savings use learned per-layer costs; unmodeled skipped calls are not counted as saved time.\n")
                .append("Average FPS is for this run only. For comparison, repeat the same scene and movement path.\n");

        Path directory = FMLPaths.GAMEDIR.get().resolve("seaborgium-reports");
        Files.createDirectories(directory);
        Path output = directory.resolve("seaborgium-profile-" + LocalDateTime.now().format(FILE_TIME) + ".txt");
        Files.writeString(output, report, StandardCharsets.UTF_8);
        return output;
    }

    private static double percent(int part, int whole) {
        return whole == 0 ? 0.0 : part * 100.0 / whole;
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
        private final Map<Class<?>, LayerStats> layers = new HashMap<>();
        private int evaluated;
        private int rendered;
        private int skipped;
        private int frames;

        private Session(int requestedSeconds, long startedNanos, long deadlineNanos) {
            this.requestedSeconds = requestedSeconds;
            this.startedNanos = startedNanos;
            this.deadlineNanos = deadlineNanos;
        }
    }

    private static final class LayerStats {
        private int rendered;
        private int skipped;
        private int samples;
        private long sampledNanos;
        private long maxSampleNanos;
    }

    private record ReportRow(Class<?> layerClass, LayerStats stats, double modeledAverageNanos,
                             double estimatedSavedNanos) {
    }
}
