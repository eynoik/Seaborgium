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
    public static final ModConfigSpec.BooleanValue ASYNC_CREATE_BLOCK_ENTITIES;
    public static final ModConfigSpec.IntValue ASYNC_CREATE_BLOCK_ENTITY_THREADS;
    public static final ModConfigSpec.IntValue ASYNC_CREATE_BLOCK_ENTITY_MIN_BATCH;
    public static final ModConfigSpec.BooleanValue WORLD_RENDER_TELEMETRY;

    public static final ModConfigSpec.BooleanValue UI_TOOLTIP_MEMOIZATION;
    public static final ModConfigSpec.IntValue UI_TOOLTIP_CACHE_ENTRIES;

    public static final ModConfigSpec.IntValue MULTITHREAD_WORKERS;
    public static final ModConfigSpec.BooleanValue ASYNC_ENTITY_POSE_PREP;
    public static final ModConfigSpec.BooleanValue ASYNC_JEI_FILTER;
    public static final ModConfigSpec.BooleanValue ASYNC_TOOLTIP_PREFETCH;
    public static final ModConfigSpec.IntValue ASYNC_TOOLTIP_MAX_STALE_TICKS;

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

        builder.comment("Shared Seaborgium worker pool. Render/OpenGL calls stay on the render thread.")
                .push("multithreading");

        int defaultWorkers = Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors() - 2));
        MULTITHREAD_WORKERS = builder
                .comment("Shared worker threads for async preparation jobs. Ryzen 5 5600 default target is 6 workers.")
                .defineInRange("workers", defaultWorkers, 1, 16);

        ASYNC_ENTITY_POSE_PREP = builder
                .comment("Prepare immutable living-entity motion/rotation/size inputs on workers. Actual model mutation and draw calls remain on the render thread.")
                .define("entityPosePrep", true);

        ASYNC_JEI_FILTER = builder
                .comment("Build changed JEI ingredient search/filter/sort results on the shared worker pool. Falls back to normal JEI when its index is being rebuilt or compatibility fails.")
                .define("jeiFilter", true);

        ASYNC_TOOLTIP_PREFETCH = builder
                .comment("Use stale-while-revalidate tooltip snapshots: render the last safe result immediately and refresh the next result on a worker. Item classes that fail off-thread are runtime-blacklisted.")
                .define("tooltipPrefetch", true);

        ASYNC_TOOLTIP_MAX_STALE_TICKS = builder
                .comment("Maximum age of an async tooltip snapshot. 1 means at most about 50 ms at 20 TPS.")
                .defineInRange("tooltipMaxStaleTicks", 1, 0, 5);

        builder.pop();

        builder.comment("Optional client optimizations for Create Factory Panels. Create remains an optional dependency.")
                .push("create_factory_panels");

        FACTORY_PANEL_OPTIMIZATIONS = builder
                .comment("Use tighter Factory Panel render bounds based on the actual connection endpoints.")
                .define("enabled", true);

        builder.pop();

        builder.comment("Experimental client-side parallel ticking for Create SmartBlockEntity instances.")
                .push("create_block_entities");

        ASYNC_CREATE_BLOCK_ENTITIES = builder
                .comment("Batch Create SmartBlockEntity ticks and run independent chunk groups in parallel, with a barrier before leaving the block-entity tick phase.")
                .define("async", true);

        ASYNC_CREATE_BLOCK_ENTITY_THREADS = builder
                .comment("Maximum shared-pool workers used concurrently for Create block entity ticking.")
                .defineInRange("threads", 3, 1, 16);

        ASYNC_CREATE_BLOCK_ENTITY_MIN_BATCH = builder
                .comment("Below this number of queued Create block entities, execute the batch synchronously to avoid scheduling overhead.")
                .defineInRange("minBatch", 8, 1, 1024);

        builder.pop();

        builder.comment("Terrain/chunk rendering telemetry used to choose safe optimization targets.")
                .push("world_rendering");

        WORLD_RENDER_TELEMETRY = builder
                .comment("Split LevelRenderer.renderSectionLayer timing by solid/cutout/translucent render type when telemetry is active.")
                .define("telemetry", true);

        builder.pop();

        builder.comment("Client tooltip memoization for inventory and recipe-viewer UI.")
                .push("ui_tooltips");

        UI_TOOLTIP_MEMOIZATION = builder
                .comment("Cache ItemStack tooltip generation for repeated requests within the same client tick.")
                .define("memoize", true);

        UI_TOOLTIP_CACHE_ENTRIES = builder
                .comment("Maximum number of tooltip snapshots kept in each bounded cache.")
                .defineInRange("cacheEntries", 512, 32, 4096);

        builder.pop();

        SPEC = builder.build();
    }

    private SeaborgiumConfig() {
    }
}
