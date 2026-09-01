package dev.eynoik.seaborgium;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SeaborgiumConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue BASE_ONLY_BELOW_PIXELS;
    public static final ModConfigSpec.DoubleValue ESSENTIAL_ONLY_BELOW_PIXELS;
    public static final ModConfigSpec.DoubleValue REDUCED_BELOW_PIXELS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_RENDER_LAYER_KEYWORDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COSMETIC_LAYER_KEYWORDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Screen-space layer budget. Thresholds use estimated on-screen pixel area.")
                .push("layer_budget");

        ENABLED = builder
                .comment("Master switch. The base entity model is never removed.")
                .define("enabled", true);

        BASE_ONLY_BELOW_PIXELS = builder
                .comment("Below this area, render only the base model.")
                .defineInRange("baseOnlyBelowPixels", 300.0, 0.0, 1_000_000.0);

        ESSENTIAL_ONLY_BELOW_PIXELS = builder
                .comment("Below this area, keep only essential equipment and identifying layers.")
                .defineInRange("essentialOnlyBelowPixels", 1_500.0, 0.0, 1_000_000.0);

        REDUCED_BELOW_PIXELS = builder
                .comment("Below this area, drop known cosmetic layers. Above it, render everything.")
                .defineInRange("reducedBelowPixels", 6_000.0, 0.0, 1_000_000.0);

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
        SPEC = builder.build();
    }

    private SeaborgiumConfig() {
    }
}
