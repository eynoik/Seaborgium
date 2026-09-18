package dev.eynoik.seaborgium.client;

import com.mojang.logging.LogUtils;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TooltipMemoizer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<CacheKey, List<Component>> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, List<Component>> eldest) {
            return size() > SeaborgiumConfig.UI_TOOLTIP_CACHE_ENTRIES.get();
        }
    };

    private static final Map<StableKey, PrefetchedTooltip> PREFETCH = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<StableKey, PrefetchedTooltip> eldest) {
            return size() > SeaborgiumConfig.UI_TOOLTIP_CACHE_ENTRIES.get();
        }
    };

    private static final Set<StableKey> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Set<String> ASYNC_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> WORKER_BYPASS = ThreadLocal.withInitial(() -> false);

    private TooltipMemoizer() {
    }

    public static boolean isWorkerBypass() {
        return WORKER_BYPASS.get();
    }

    public static List<Component> get(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag) {
        if (isWorkerBypass() || !canCache(stack, player, flag)) {
            return null;
        }

        CacheKey key = key(stack, player, flag);
        synchronized (CACHE) {
            List<Component> cached = CACHE.get(key);
            if (cached != null) {
                return new ArrayList<>(cached);
            }
        }

        if (!SeaborgiumConfig.ASYNC_TOOLTIP_PREFETCH.get()
                || ASYNC_BLACKLIST.contains(stack.getItem().getClass().getName())) {
            return null;
        }

        StableKey stable = stableKey(stack, player, flag);
        PrefetchedTooltip prefetched;
        synchronized (PREFETCH) {
            prefetched = PREFETCH.get(stable);
        }

        if (prefetched == null) {
            return null;
        }

        int age = Math.max(0, player.tickCount - prefetched.generatedTick());
        if (age > SeaborgiumConfig.ASYNC_TOOLTIP_MAX_STALE_TICKS.get()) {
            return null;
        }

        scheduleRefresh(stack, context, player, flag, stable, player.tickCount);
        List<Component> copy = new ArrayList<>(prefetched.lines());
        synchronized (CACHE) {
            CACHE.put(key, List.copyOf(copy));
        }
        return copy;
    }

    public static void put(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag, List<Component> lines) {
        if (isWorkerBypass() || !canCache(stack, player, flag) || lines == null) {
            return;
        }

        CacheKey key = key(stack, player, flag);
        List<Component> snapshot = List.copyOf(lines);
        synchronized (CACHE) {
            CACHE.put(key, snapshot);
        }

        if (SeaborgiumConfig.ASYNC_TOOLTIP_PREFETCH.get()
                && !ASYNC_BLACKLIST.contains(stack.getItem().getClass().getName())) {
            StableKey stable = stableKey(stack, player, flag);
            synchronized (PREFETCH) {
                PREFETCH.put(stable, new PrefetchedTooltip(player.tickCount, snapshot));
            }
        }
    }

    private static void scheduleRefresh(
            ItemStack stack,
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            StableKey stable,
            int tick
    ) {
        if (!IN_FLIGHT.add(stable)) {
            return;
        }

        ItemStack copy = stack.copy();
        SeaborgiumJobSystem.executor().execute(() -> {
            WORKER_BYPASS.set(true);
            try {
                List<Component> fresh = copy.getTooltipLines(context, player, flag);
                synchronized (PREFETCH) {
                    PREFETCH.put(stable, new PrefetchedTooltip(tick, List.copyOf(fresh)));
                }
            } catch (Throwable throwable) {
                String className = stack.getItem().getClass().getName();
                if (ASYNC_BLACKLIST.add(className)) {
                    LOGGER.warn("Seaborgium disabled async tooltip precompute for {} after a worker failure", className, throwable);
                }
            } finally {
                WORKER_BYPASS.remove();
                IN_FLIGHT.remove(stable);
            }
        });
    }

    private static boolean canCache(ItemStack stack, Player player, TooltipFlag flag) {
        return SeaborgiumConfig.UI_TOOLTIP_MEMOIZATION.get()
                && stack != null
                && !stack.isEmpty()
                && player != null
                && player.level().isClientSide()
                && flag != null;
    }

    private static CacheKey key(ItemStack stack, Player player, TooltipFlag flag) {
        StableKey stable = stableKey(stack, player, flag);
        return new CacheKey(stable, player.tickCount);
    }

    private static StableKey stableKey(ItemStack stack, Player player, TooltipFlag flag) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;

        return new StableKey(
                player.getUUID(),
                stack.getItem(),
                stack.getCount(),
                ItemStack.hashItemAndComponents(stack),
                flag.isAdvanced(),
                flag.isCreative(),
                Screen.hasShiftDown(),
                Screen.hasControlDown(),
                Screen.hasAltDown(),
                screen == null ? null : screen.getClass()
        );
    }

    private record CacheKey(StableKey stable, int clientTick) {
    }

    private record StableKey(
            UUID playerId,
            Item item,
            int count,
            int stackHash,
            boolean advanced,
            boolean creative,
            boolean shift,
            boolean control,
            boolean alt,
            Class<?> screenClass
    ) {
    }

    private record PrefetchedTooltip(int generatedTick, List<Component> lines) {
    }
}
