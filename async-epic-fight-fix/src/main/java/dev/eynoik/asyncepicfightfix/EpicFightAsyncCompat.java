package dev.eynoik.asyncepicfightfix;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection-only bridge so the compat jar does not link directly against Epic Fight or optional addons.
 * Calls originate from Async's synchronous classification pass, before worker entity ticks are submitted.
 */
public final class EpicFightAsyncCompat {
    private static final String[] WOM_TAG_PREFIXES = {
        "anti_stunlock:",
        "timed_katana_slashes:",
        "lunar_eclipse:",
        "solar_ignited:",
        "WoM_blackout",
        "wom_ultimate_Invulnerable",
        "wom_health_fix:",
        "wom_serius_focus:",
        "wom-bow-replaced",
        "wom-stronger-mob"
    };

    private static final String[] TWILIGHT_DATA_KEYS = {
        "twilightmortisbows:bokken_off_balance",
        "twilightmortisbows:ice_freeze_lockout_until",
        "twilightmortisbows:ice_freeze_damage_until",
        "twilightmortisbows:ice_freeze_next_damage_at",
        "twilightmortisbows:ice_freeze_resolved"
    };

    private static volatile boolean initialized;
    private static volatile boolean epicFightAvailable;
    private static Method getEntityPatch;
    private static Class<?> entityPatchClass;
    private static Method entityGetTags;
    private static Method entityGetPersistentData;
    private static Method compoundContains;

    // Positive class cache avoids repeated reflective capability reads for the common patched-mob case.
    // Negative results are deliberately not cached because datapacks and optional compat mods can attach patches.
    private static final Set<Class<?>> ALWAYS_PATCHED_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Map<Class<?>, Boolean> ADDON_MARKER_CLASS_CACHE = new ConcurrentHashMap<>();

    private EpicFightAsyncCompat() {}

    public static boolean shouldForceSynchronous(Object entity) {
        if (entity == null) return false;
        ensureInitialized(entity.getClass().getClassLoader());
        if (!epicFightAvailable) return false;

        Class<?> entityClass = entity.getClass();
        if (ALWAYS_PATCHED_CLASSES.contains(entityClass)) return true;

        try {
            Object patch = getEntityPatch.invoke(null, entity, entityPatchClass);
            if (patch != null) {
                ALWAYS_PATCHED_CLASSES.add(entityClass);
                return true;
            }
        } catch (Throwable ignored) {
            // Fail open: preserve Async's own decision instead of crashing startup or tick classification.
        }

        // WOM and Twilight Forest compat both have per-LivingEntity tick handlers that can mutate targets
        // even when that target does not itself own an Epic Fight patch. Runtime markers keep the guard narrow.
        return hasAddonRuntimeMarker(entity, entityClass);
    }

    private static boolean hasAddonRuntimeMarker(Object entity, Class<?> entityClass) {
        Boolean noMarkerApi = ADDON_MARKER_CLASS_CACHE.get(entityClass);
        if (Boolean.TRUE.equals(noMarkerApi)) return false;

        try {
            Object tagsObj = entityGetTags.invoke(entity);
            if (tagsObj instanceof Iterable<?> tags) {
                for (Object tagObj : tags) {
                    if (!(tagObj instanceof String tag)) continue;
                    for (String prefix : WOM_TAG_PREFIXES) {
                        if (tag.startsWith(prefix)) return true;
                    }
                }
            }

            Object data = entityGetPersistentData.invoke(entity);
            if (data != null) {
                for (String key : TWILIGHT_DATA_KEYS) {
                    Object result = compoundContains.invoke(data, key);
                    if (Boolean.TRUE.equals(result)) return true;
                }
            }
        } catch (Throwable ignored) {
            // Some unusual Entity implementation could theoretically not expose NeoForge's persistent-data API.
            // Cache only that structural failure; ordinary negative marker results remain re-checkable next tick.
            ADDON_MARKER_CLASS_CACHE.put(entityClass, Boolean.TRUE);
        }
        return false;
    }

    private static void ensureInitialized(ClassLoader loader) {
        if (initialized) return;
        synchronized (EpicFightAsyncCompat.class) {
            if (initialized) return;
            try {
                Class<?> capabilities = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities", false, loader);
                entityPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.EntityPatch", false, loader);
                Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity", false, loader);
                Class<?> compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag", false, loader);
                getEntityPatch = capabilities.getMethod("getEntityPatch", entityClass, Class.class);
                entityGetTags = entityClass.getMethod("getTags");
                entityGetPersistentData = entityClass.getMethod("getPersistentData");
                compoundContains = compoundTagClass.getMethod("contains", String.class);
                epicFightAvailable = true;
            } catch (Throwable ignored) {
                epicFightAvailable = false;
            } finally {
                initialized = true;
            }
        }
    }
}
