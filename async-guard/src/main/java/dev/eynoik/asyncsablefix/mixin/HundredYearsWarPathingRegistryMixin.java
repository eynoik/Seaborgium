package dev.eynoik.asyncsablefix.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hundred Years Warfare 0.7.1r keeps pathing-task managers in a normal HashMap.
 *
 * Async can tick several HYW entities in parallel. ReturnToHomeGoal calls
 * PathingTaskManagerRegistry#getTaskManager from those workers, and concurrent
 * computeIfAbsent calls were observed throwing ConcurrentModificationException.
 *
 * Serialize only the registry's computeIfAbsent operation. HYW entity AI stays
 * asynchronous; we do not force the whole BaseCombatEntity family onto the
 * server thread.
 */
@Pseudo
@Mixin(
    targets = "ydmsama.hundred_years_war.main.entity.utils.PathingTaskManagerRegistry",
    remap = false
)
public abstract class HundredYearsWarPathingRegistryMixin {
    @Redirect(
        method = "getTaskManager",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"
        ),
        remap = false,
        require = 0
    )
    private static Object asyncguard$serializeHashMapComputeIfAbsent(
        HashMap<Object, Object> map,
        Object key,
        Function<Object, Object> factory
    ) {
        synchronized (map) {
            return map.computeIfAbsent(key, factory);
        }
    }

    @Redirect(
        method = "getTaskManager",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"
        ),
        remap = false,
        require = 0
    )
    private static Object asyncguard$serializeMapComputeIfAbsent(
        Map<Object, Object> map,
        Object key,
        Function<Object, Object> factory
    ) {
        synchronized (map) {
            return map.computeIfAbsent(key, factory);
        }
    }
}
