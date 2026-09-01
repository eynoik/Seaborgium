package dev.eynoik.seaborgium.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.eynoik.seaborgium.Seaborgium;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Seaborgium.MOD_ID, value = Dist.CLIENT)
public final class ClientTelemetryHud {
    private static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.seaborgium.toggle_telemetry",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seaborgium"
    );

    private static boolean visible;

    private ClientTelemetryHud() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        while (TOGGLE_HUD.consumeClick()) {
            visible = !visible;
        }
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("seaborgium")
                .then(Commands.literal("profile")
                        .executes(context -> TimedProfileSession.start(60) ? 1 : 0)
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 600))
                                .executes(context -> TimedProfileSession.start(
                                        IntegerArgumentType.getInteger(context, "seconds")) ? 1 : 0))
                        .then(Commands.literal("stop")
                                .executes(context -> TimedProfileSession.stop() ? 1 : 0)))
                .then(Commands.literal("benchmark")
                        .executes(context -> AbBenchmarkSession.start(60) ? 1 : 0)
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(40, 300))
                                .executes(context -> AbBenchmarkSession.start(
                                        IntegerArgumentType.getInteger(context, "seconds")) ? 1 : 0))
                        .then(Commands.literal("static")
                                .executes(context -> AbBenchmarkSession.start(60) ? 1 : 0)
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(40, 300))
                                        .executes(context -> AbBenchmarkSession.start(
                                                IntegerArgumentType.getInteger(context, "seconds")) ? 1 : 0)))
                        .then(Commands.literal("play")
                                .executes(context -> AbBenchmarkSession.startPlay(60) ? 1 : 0)
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 300))
                                        .executes(context -> AbBenchmarkSession.startPlay(
                                                IntegerArgumentType.getInteger(context, "seconds")) ? 1 : 0)))
                        .then(Commands.literal("stop")
                                .executes(context -> AbBenchmarkSession.stop() ? 1 : 0))));
    }

    @SubscribeEvent
    public static void renderTelemetry(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LayerProfiler.recordFrame();
        LayerBudget.finishFrame();
        boolean forcedTelemetry = TimedProfileSession.isActive() || AbBenchmarkSession.isActive();
        if (!visible || minecraft.options.hideGui
                || (!SeaborgiumConfig.DEBUG_TELEMETRY.get() && !forcedTelemetry)) {
            return;
        }

        LayerProfiler.Snapshot snapshot = LayerProfiler.snapshot();
        String summary = String.format(
                Locale.ROOT,
                "Seaborgium: -%d/%d layers (%.1f%%)",
                snapshot.skipped(),
                snapshot.evaluated(),
                snapshot.skippedPercent()
        );

        String timingSummary = formatTimingSummary(snapshot.expensiveLayers());
        String savedSummary = String.format(
                Locale.ROOT,
                "Estimated CPU saved: %.3f ms/frame (%.0f%% modeled)",
                snapshot.estimatedSavedMillisPerFrame(),
                snapshot.modeledPercent()
        );
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        String profileSummary;
        if (AbBenchmarkSession.isActive()) {
            profileSummary = "A/B benchmark (" + AbBenchmarkSession.statusLabel() + "): "
                    + AbBenchmarkSession.remainingSeconds() + " s remaining";
        } else if (TimedProfileSession.isActive()) {
            profileSummary = "Timed profile: " + TimedProfileSession.remainingSeconds() + " s remaining";
        } else {
            profileSummary = "";
        }
        int width = Math.max(font.width(summary), Math.max(font.width(savedSummary), font.width(timingSummary)));
        if (!profileSummary.isEmpty()) {
            width = Math.max(width, font.width(profileSummary));
        }
        int x = graphics.guiWidth() - width - 6;
        int y = 6;

        int bottom = profileSummary.isEmpty() ? y + 29 : y + 39;
        graphics.fill(x - 3, y - 3, graphics.guiWidth() - 3, bottom, 0x90000000);
        graphics.drawString(font, summary, x, y, 0xFFE0E0E0, true);
        graphics.drawString(font, savedSummary, x, y + 10, 0xFFB8D8B8, true);
        graphics.drawString(font, timingSummary, x, y + 20, 0xFFAAAAAA, true);
        if (!profileSummary.isEmpty()) {
            graphics.drawString(font, profileSummary, x, y + 30, 0xFFFFD070, true);
        }
    }

    private static String formatTimingSummary(List<LayerProfiler.LayerTiming> timings) {
        if (timings.isEmpty()) {
            return "Top layers: collecting samples...";
        }

        StringBuilder text = new StringBuilder("Top: ");
        int shown = Math.min(2, timings.size());
        for (int index = 0; index < shown; index++) {
            if (index > 0) {
                text.append(" | ");
            }
            LayerProfiler.LayerTiming timing = timings.get(index);
            text.append(timing.name())
                    .append(' ')
                    .append(String.format(Locale.ROOT, "%.1fus", timing.averageMicros()));
        }
        return text.toString();
    }

    @EventBusSubscriber(modid = Seaborgium.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_HUD);
        }
    }
}
