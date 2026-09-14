package dev.eynoik.seaborgium;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SeaborgiumConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_TELEMETRY;
    public static final ModConfigSpec.DoubleValue BASE_ONLY_BELOW_PIXELS;
    public static final ModConfigSpec.DoubleValue ESSENTIAL_ONLY_BELOW_PIXELS;
    public static final ModConfigSpec.DoubleValue REDUCED_BELOW_PIXELS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_RENDER_LAYER_KEYWORDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COSMETIC_LAYER_KEYWORDS;

    public static final ModConfigSpec.BooleanValue FACTORY_PANEL_OPTIMIZATIONS;
    public static final ModConfigSpec.DoubleValue FACTORY_PANEL_FILTER_ITEM_DISTANCE;
    public static final ModConfigSpec.BooleanValue WORLD_RENDER_TELEMETRY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Screen-space layer budget. Thresholds use estimated on-screen pixel area.")
                .push("layer_budget");

        ENABLED = builder
                .comment("Master switch. The base entity model is never removed.")
                .define("enabled", true);

        DEBUG_TELEMETRY = builder
                .comment("Collect lightweight layer counters and sampled timings for the F3 debug screen.")
                .define("debugTelemetry", true);

        BASE_ONLY_BELOW_PIXELS = builder
                .comment("Below this area, render only the base model.")
                .defineInRange("baseOnlyBelowPixels", 64.0, 0.0, 1_000_000.0);

        ESSENTIAL_ONLY_BELOW_PIXELS = builder
                .comment("Below this area, keep only essential equipment and identifying layers.")
                .defineInRange("essentialOnlyBelowPixels", 400.0, 0.0, 1_000_000.0);

        REDUCED_BELOW_PIXELS = builder
                .comment("Below this area, drop known cosmetic layers. Above it, render everything.")
                .defineInRange("reducedBelowPixels", 1_200.0, 0.0, 1_000_000.0);

        ALWAYS_RENDER_LAYER_KEYWORDS = builder
                .comment("Case-insensitive class-name fragments for layers which must survive the essential tier.")
                .defineListAllowEmpty("alwaysRenderLayerKeywords",
                        List.of("armor", "iteminhand", "helditem", "head", "eyes"),
                        () -> "layer",
                        value -> value instanceof String);

        COSMETIC_LAYER_KEYWORDS = builder
                .comment("Case-insensitive class-name fragments removed in the reduced tier.")
                .defineListAllowEmpty("cosmeticLayerKeywords",
                        List.of("cape", "elytra", "spinattack", "slimeouter", "deadmau5"),
                        () -> "layer",
                        value -> value instanceof String);

        builder.pop();

        builder.comment("Optional client optimizations for Create Factory Panels. Create remains an optional dependency.")
                .push("create_factory_panels");

        FACTORY_PANEL_OPTIMIZATIONS = builder
                .comment("Use tighter Factory Panel render bounds and a distance LOD for filter item icons.")
                .define("enabled", true);

        FACTORY_PANEL_FILTER_ITEM_DISTANCE = builder
                .comment("Maximum distance in blocks for Factory Panel filter item icons. Panel paths and bulbs are unaffected.")
                .defineInRange("filterItemRenderDistance", 40.0, 8.0, 64.0);

        builder.pop();

        builder.comment("Terrain/chunk rendering telemetry used to choose safe optimization targets.")
                .push("world_rendering");

        WORLD_RENDER_TELEMETRY = builder
                .comment("Split LevelRenderer.renderSectionLayer timing by solid/cutout/translucent render type when telemetry is active.")
                .define("telemetry", true);

        builder.pop();
        SPEC = builder.build();
    }

    private SeaborgiumConfig() {
    }
}
