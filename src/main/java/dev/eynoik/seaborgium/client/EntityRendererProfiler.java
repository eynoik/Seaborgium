package dev.eynoik.seaborgium.client;

/** Low-overhead sampler for the complete LivingEntityRenderer invocation. */
public final class EntityRendererProfiler {
    private static final int SAMPLE_MASK = 31;
    private static long sampleSequence;

    private EntityRendererProfiler() {
    }

    public static long beginSample(Class<?> rendererClass) {
        if (!AbBenchmarkSession.isActive()) {
            return 0L;
        }
        AbBenchmarkSession.recordEntityRender(rendererClass);
        return (sampleSequence++ & SAMPLE_MASK) == 0L ? System.nanoTime() : 0L;
    }

    public static void endSample(Class<?> rendererClass, long startedAt) {
        if (startedAt == 0L) {
            return;
        }
        AbBenchmarkSession.recordEntitySample(rendererClass, System.nanoTime() - startedAt);
    }
}
