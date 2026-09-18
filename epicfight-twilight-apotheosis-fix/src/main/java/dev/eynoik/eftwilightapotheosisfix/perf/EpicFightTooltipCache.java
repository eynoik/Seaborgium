package dev.eynoik.eftwilightapotheosisfix.perf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class EpicFightTooltipCache {
    private static final int MAX_ENTRIES = 256;

    private static final Map<CacheKey, List<Component>> CACHE = new LinkedHashMap<>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, List<Component>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private static final ThreadLocal<CacheKey> ACTIVE_KEY = new ThreadLocal<>();

    private EpicFightTooltipCache() {
    }

    public static List<Component> begin(ItemTooltipEvent event) {
        ACTIVE_KEY.remove();

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack == null || stack.isEmpty() || !player.level().isClientSide()) {
            return null;
        }

        CacheKey key = new CacheKey(
                player.getUUID(),
                player.tickCount,
                stack.getItem(),
                stack.getCount(),
                ItemStack.hashItemAndComponents(stack),
                event.getToolTip().hashCode()
        );

        synchronized (CACHE) {
            List<Component> cached = CACHE.get(key);
            if (cached != null) {
                return new ArrayList<>(cached);
            }
        }

        ACTIVE_KEY.set(key);
        return null;
    }

    public static void finish(ItemTooltipEvent event) {
        CacheKey key = ACTIVE_KEY.get();
        ACTIVE_KEY.remove();

        if (key == null) {
            return;
        }

        List<Component> snapshot = List.copyOf(event.getToolTip());
        synchronized (CACHE) {
            CACHE.put(key, snapshot);
        }
    }

    private record CacheKey(
            UUID playerId,
            int clientTick,
            Item item,
            int count,
            int stackHash,
            int baseTooltipHash
    ) {
    }
}
