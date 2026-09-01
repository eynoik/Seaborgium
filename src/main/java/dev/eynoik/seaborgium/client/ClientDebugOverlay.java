package dev.eynoik.seaborgium.client;

import dev.eynoik.seaborgium.Seaborgium;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.Locale;

@EventBusSubscriber(modid = Seaborgium.MOD_ID, value = Dist.CLIENT)
public final class ClientDebugOverlay {
    private ClientDebugOverlay() {
    }

    @SubscribeEvent
    public static void appendDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!SeaborgiumConfig.DEBUG_TELEMETRY.get()) {
            return;
        }

        LayerProfiler.Snapshot snapshot = LayerProfiler.snapshot();
        event.getLeft().add("");
        event.getLeft().add(String.format(
                Locale.ROOT,
                "Seaborgium: %d/%d layers skipped (%.1f%%)",
                snapshot.skipped(),
                snapshot.evaluated(),
                snapshot.skippedPercent()
        ));

        for (LayerProfiler.LayerTiming timing : snapshot.expensiveLayers()) {
            event.getLeft().add(String.format(
                    Locale.ROOT,
                    "  %s: %.1f us avg, %.1f us max (%d samples)",
                    timing.name(),
                    timing.averageMicros(),
                    timing.maxMicros(),
                    timing.samples()
            ));
        }
    }
}
