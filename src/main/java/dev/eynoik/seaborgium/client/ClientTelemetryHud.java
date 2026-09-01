package dev.eynoik.seaborgium.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.eynoik.seaborgium.Seaborgium;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

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
    public static void renderTelemetry(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.options.hideGui || !SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
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
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = Math.max(font.width(summary), font.width(timingSummary));
        int x = graphics.guiWidth() - width - 6;
        int y = 6;

        graphics.fill(x - 3, y - 3, graphics.guiWidth() - 3, y + 19, 0x90000000);
        graphics.drawString(font, summary, x, y, 0xFFE0E0E0, true);
        graphics.drawString(font, timingSummary, x, y + 10, 0xFFAAAAAA, true);
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
