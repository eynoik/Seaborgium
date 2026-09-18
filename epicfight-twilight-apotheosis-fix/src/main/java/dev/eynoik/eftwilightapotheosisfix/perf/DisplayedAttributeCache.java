package dev.eynoik.eftwilightapotheosisfix.perf;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DisplayedAttributeCache {
    private static final int MAX_ENTRIES = 512;

    private static final Map<CacheKey, Double> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Double> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private DisplayedAttributeCache() {
    }

    public static Double get(ItemStack stack, Holder<Attribute> attribute, double baseValue) {
        CacheKey key = key(stack, attribute, baseValue);
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    public static void put(ItemStack stack, Holder<Attribute> attribute, double baseValue, double result) {
        CacheKey key = key(stack, attribute, baseValue);
        synchronized (CACHE) {
            CACHE.put(key, result);
        }
    }

    private static CacheKey key(ItemStack stack, Holder<Attribute> attribute, double baseValue) {
        Item item = stack == null || stack.isEmpty() ? null : stack.getItem();
        return new CacheKey(item, normalizedStackHash(stack), attribute, Double.doubleToLongBits(baseValue));
    }

    private static int normalizedStackHash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        if (!stack.isDamageableItem()) {
            return ItemStack.hashItemAndComponents(stack);
        }

        ItemStack normalized = stack.copy();
        normalized.setDamageValue(0);
        return ItemStack.hashItemAndComponents(normalized);
    }

    private record CacheKey(Item item, int stackHash, Holder<Attribute> attribute, long baseValueBits) {
    }
}
