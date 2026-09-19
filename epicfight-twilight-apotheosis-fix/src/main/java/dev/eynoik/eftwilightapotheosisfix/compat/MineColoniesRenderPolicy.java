package dev.eynoik.eftwilightapotheosisfix.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps normal MineColonies civilians on the vanilla MineColonies renderer while preserving
 * Epic Fight rendering for actual guards. Raiders and mercenaries are separate entity types,
 * so this policy never catches them and they remain on Epic Fight.
 *
 * Reflection is intentional: MineColonies, epicfightxminecolonies and Epic Fight stay optional
 * compile-time dependencies for this patch. Runtime method lookups are cached per class.
 */
public final class MineColoniesRenderPolicy {
    private static final String CITIZEN_CLASS = "com.minecolonies.api.entity.citizen.AbstractEntityCitizen";

    private static volatile Class<?> citizenBase;
    private static volatile boolean citizenLookupDone;
    private static final AtomicBoolean reflectionWarningPrinted = new AtomicBoolean();
    private static final AtomicBoolean patchReflectionWarningPrinted = new AtomicBoolean();

    /**
     * Epic Fight 21.17.3.1 inherits getOriginal() from EntityPatch<T extends Entity>.
     * The real JVM descriptor therefore returns net.minecraft.world.entity.Entity, not Object.
     * Keeping this lookup reflective avoids baking an incorrect compile-only descriptor into our
     * mixin bytecode and also keeps Epic Fight out of the compile-time dependency graph.
     */
    private static final ClassValue<Optional<Method>> GET_PATCH_ORIGINAL = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            return findPublicMethod(type, "getOriginal");
        }
    };

    private static final ClassValue<Optional<Method>> GET_JOB_HANDLER = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            return findPublicMethod(type, "getCitizenJobHandler");
        }
    };

    private static final ClassValue<Optional<Method>> GET_COLONY_JOB = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            return findPublicMethod(type, "getColonyJob");
        }
    };

    private static final ClassValue<Optional<Method>> IS_GUARD = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            return findPublicMethod(type, "isGuard");
        }
    };

    private MineColoniesRenderPolicy() {
    }

    /**
     * Safely resolves Epic Fight's underlying entity without linking against the method descriptor.
     * Returns null on any API mismatch so the render gate fails open and Epic Fight keeps rendering.
     */
    public static Object getOriginalFromPatch(Object patch) {
        if (patch == null) {
            return null;
        }

        try {
            Method getOriginal = GET_PATCH_ORIGINAL.get(patch.getClass()).orElse(null);
            if (getOriginal == null) {
                return null;
            }
            return getOriginal.invoke(patch);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (patchReflectionWarningPrinted.compareAndSet(false, true)) {
                System.err.println("[EFTwilightApotheosisFix] Epic Fight getOriginal lookup failed once; preserving Epic Fight renderer. " + ex);
            }
            return null;
        }
    }

    /**
     * @return null when the entity is not a MineColonies citizen, true when Epic Fight rendering
     * should remain active, false when vanilla MineColonies rendering should be used.
     */
    public static Boolean shouldUseEpicRenderer(Object entity) {
        if (entity == null || !isMineColoniesCitizen(entity)) {
            return null;
        }

        try {
            Method getJobHandler = GET_JOB_HANDLER.get(entity.getClass()).orElse(null);
            if (getJobHandler == null) {
                return Boolean.TRUE;
            }

            Object handler = getJobHandler.invoke(entity);
            if (handler == null) {
                return Boolean.FALSE;
            }

            Method getColonyJob = GET_COLONY_JOB.get(handler.getClass()).orElse(null);
            if (getColonyJob == null) {
                return Boolean.TRUE;
            }

            Object job = getColonyJob.invoke(handler);
            if (job == null) {
                // Visitors and citizens without an assigned combat job do not need a skinned EF mesh.
                return Boolean.FALSE;
            }

            Method isGuard = IS_GUARD.get(job.getClass()).orElse(null);
            if (isGuard == null) {
                return Boolean.TRUE;
            }

            return Boolean.TRUE.equals(isGuard.invoke(job));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // Fail open: if MineColonies changes its API, preserve the original Epic Fight behavior.
            if (reflectionWarningPrinted.compareAndSet(false, true)) {
                System.err.println("[EFTwilightApotheosisFix] MineColonies guard detection failed once; preserving Epic Fight renderer. " + ex);
            }
            return Boolean.TRUE;
        }
    }

    private static boolean isMineColoniesCitizen(Object entity) {
        Class<?> base = citizenBase;
        if (!citizenLookupDone) {
            synchronized (MineColoniesRenderPolicy.class) {
                if (!citizenLookupDone) {
                    try {
                        base = Class.forName(CITIZEN_CLASS, false, entity.getClass().getClassLoader());
                        citizenBase = base;
                    } catch (ClassNotFoundException ignored) {
                        citizenBase = null;
                    } finally {
                        citizenLookupDone = true;
                    }
                } else {
                    base = citizenBase;
                }
            }
        }

        return base != null && base.isInstance(entity);
    }

    private static Optional<Method> findPublicMethod(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            method.setAccessible(true);
            return Optional.of(method);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
    }
}
