package dev.eynoik.asyncsablefix.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * PneumaticCraft 8.2.23 keeps target-tracking state in ordinary static maps.
 *
 * With vanilla single-threaded entity ticking that is fine. Async can fire
 * LivingChangeTargetEvent concurrently from several entity workers, which lets
 * PneumaticArmorHandler mutate the same Int2IntOpenHashMap from multiple
 * threads. The reproduced failure corrupts the map during rehash() and throws
 * ArrayIndexOutOfBoundsException from an Async worker; after enough corruption
 * the normal server thread can later die in the same put()/rehash() path.
 *
 * Keep entity ticks asynchronous and only make the two shared tracking
 * structures thread-safe:
 *  - targetingTracker uses fastutil's synchronized wrapper;
 *  - targetWarnings uses concurrent outer and inner maps, because the original
 *    computeIfAbsent lambda creates ordinary HashMaps.
 */
@Pseudo
@Mixin(targets = "me.desht.pneumaticcraft.common.event.PneumaticArmorHandler", remap = false)
public abstract class PneumaticArmorHandlerThreadSafetyMixin {
    @Shadow @Final @Mutable
    private static Int2IntMap targetingTracker;

    @Shadow @Final @Mutable
    private static Map<UUID, Map<String, Integer>> targetWarnings;

    @Inject(method = "<clinit>", at = @At("RETURN"), remap = false, require = 1)
    private static void asyncguard$installThreadSafeTargetState(CallbackInfo ci) {
        targetingTracker = Int2IntMaps.synchronize(targetingTracker);

        AsyncGuardTargetWarningsMap safeWarnings = new AsyncGuardTargetWarningsMap();
        targetWarnings.forEach((playerId, warnings) ->
                safeWarnings.put(playerId, asyncguard$toConcurrentWarnings(warnings)));
        targetWarnings = safeWarnings;

        System.out.println("[AsyncGuard] PneumaticCraft target tracking wrapped with thread-safe maps.");
    }

    @Unique
    private static Map<String, Integer> asyncguard$toConcurrentWarnings(Map<String, Integer> source) {
        if (source instanceof ConcurrentMap<?, ?>) {
            return source;
        }
        return new ConcurrentHashMap<>(source);
    }

    @Unique
    private static final class AsyncGuardTargetWarningsMap
            extends ConcurrentHashMap<UUID, Map<String, Integer>> {
        @Override
        public Map<String, Integer> computeIfAbsent(
                UUID key,
                Function<? super UUID, ? extends Map<String, Integer>> mappingFunction
        ) {
            return super.computeIfAbsent(key, playerId -> {
                Map<String, Integer> created = mappingFunction.apply(playerId);
                return created == null ? null : asyncguard$toConcurrentWarnings(created);
            });
        }
    }
}
