package dev.eynoik.seaborgium.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class TooltipMemoizer {
    private static final Map<CacheKey, List<Component>> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, List<Component>> eldest) {
            return size() > dev.eynoik.seaborgium.SeaborgiumConfig.UI_TOOLTIP_CACHE_ENTRIES.get();
        }
    };

    private TooltipMemoizer() {
    }

    public static List<Component> get(ItemStack stack, Player player, TooltipFlag flag) {
        if (!canCache(stack, player, flag)) {
            return null;
        }

        CacheKey key = key(stack, player, flag);
        synchronized (CACHE) {
            List<Component> cached = CACHE.get(key);
            return cached == null ? null : new ArrayList<>(cached);
        }
    }

    public static void put(ItemStack stack, Player player, TooltipFlag flag, List<Component> lines) {
        if (!canCache(stack, player, flag) || lines == null) {
            return;
        }

        CacheKey key = key(stack, player, flag);
        synchronized (CACHE) {
            CACHE.put(key, List.copyOf(lines));
        }
    }

    private static boolean canCache(ItemStack stack, Player player, TooltipFlag flag) {
        return dev.eynoik.seaborgium.SeaborgiumConfig.UI_TOOLTIP_MEMOIZATION.get()
                && stack != null
                && !stack.isEmpty()
                && player != null
                && player.level().isClientSide()
                && flag != null;
    }

    private static CacheKey key(ItemStack stack, Player player, TooltipFlag flag) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;

        return new CacheKey(
                player.getUUID(),
                player.tickCount,
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

    private record CacheKey(
            UUID playerId,
            int clientTick,
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
}
