package dev.eynoik.seaborgium.client;

import com.mojang.logging.LogUtils;
import dev.eynoik.seaborgium.SeaborgiumConfig;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class AsyncJeiSearch {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Object, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicLong GENERATION = new AtomicLong();

    private static volatile Reflection reflection;
    private static volatile boolean unavailable;

    private AsyncJeiSearch() {
    }

    public static List<?> readyOrSchedule(Object ingredientFilter) {
        if (!SeaborgiumConfig.ASYNC_JEI_FILTER.get() || unavailable || ingredientFilter == null) {
            return null;
        }

        Reflection r = reflection(ingredientFilter.getClass());
        if (r == null) {
            return null;
        }

        try {
            if (r.searchIndexDirty.getBoolean(ingredientFilter) || r.sortIndexesDirty.getBoolean(ingredientFilter)) {
                return null;
            }

            Object cached = r.ingredientListCached.get(ingredientFilter);
            if (cached instanceof List<?>) {
                return null;
            }

            String filterText = String.valueOf(r.getFilterText.invoke(r.filterTextSource.get(ingredientFilter))).toLowerCase();
            State state = STATES.computeIfAbsent(ingredientFilter, ignored -> new State());

            synchronized (state) {
                if (filterText.equals(state.readyFilter) && state.ready != null) {
                    r.ingredientListCached.set(ingredientFilter, state.ready);
                    return new ArrayList<>(state.ready);
                }

                if (!filterText.equals(state.requestedFilter)) {
                    state.requestedFilter = filterText;
                    long generation = GENERATION.incrementAndGet();
                    state.requestGeneration = generation;
                    schedule(ingredientFilter, filterText, generation, state, r);
                }

                // Avoid blocking the render thread while a new search is being prepared.
                // The previous JEI result remains visible for a few frames.
                if (state.ready != null) {
                    return new ArrayList<>(state.ready);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
        }
        return null;
    }

    public static void seed(Object ingredientFilter, List<?> result) {
        if (!SeaborgiumConfig.ASYNC_JEI_FILTER.get() || ingredientFilter == null || result == null || unavailable) {
            return;
        }

        Reflection r = reflection(ingredientFilter.getClass());
        if (r == null) {
            return;
        }

        try {
            String filterText = String.valueOf(r.getFilterText.invoke(r.filterTextSource.get(ingredientFilter))).toLowerCase();
            State state = STATES.computeIfAbsent(ingredientFilter, ignored -> new State());
            synchronized (state) {
                state.readyFilter = filterText;
                state.requestedFilter = filterText;
                state.ready = List.copyOf(result);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
        }
    }

    private static void schedule(Object owner, String filterText, long generation, State state, Reflection r) {
        SeaborgiumJobSystem.executor().execute(() -> {
            try {
                @SuppressWarnings("unchecked")
                Stream<Object> stream = (Stream<Object>) r.getIngredientListUncached.invoke(owner, filterText);
                List<Object> typed = stream.toList();
                List<Object> elements = new ArrayList<>(typed.size());
                for (Object ingredient : typed) {
                    elements.add(r.ingredientElementCtor.newInstance(ingredient));
                }
                List<?> finished = List.copyOf(elements);

                synchronized (state) {
                    if (state.requestGeneration == generation && filterText.equals(state.requestedFilter)) {
                        state.readyFilter = filterText;
                        state.ready = finished;
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.warn("Seaborgium async JEI filter failed; this request will fall back to JEI's normal thread", throwable);
                synchronized (state) {
                    if (state.requestGeneration == generation) {
                        state.requestedFilter = null;
                    }
                }
            }
        });
    }

    private static Reflection reflection(Class<?> filterClass) {
        Reflection local = reflection;
        if (local != null) {
            return local;
        }
        if (unavailable) {
            return null;
        }

        synchronized (AsyncJeiSearch.class) {
            if (reflection != null) {
                return reflection;
            }
            try {
                Field filterTextSource = filterClass.getDeclaredField("filterTextSource");
                Field ingredientListCached = filterClass.getDeclaredField("ingredientListCached");
                Field searchIndexDirty = filterClass.getDeclaredField("searchIndexDirty");
                Field sortIndexesDirty = filterClass.getDeclaredField("sortIndexesDirty");
                Method getIngredientListUncached = filterClass.getDeclaredMethod("getIngredientListUncached", String.class);

                filterTextSource.setAccessible(true);
                ingredientListCached.setAccessible(true);
                searchIndexDirty.setAccessible(true);
                sortIndexesDirty.setAccessible(true);
                getIngredientListUncached.setAccessible(true);

                Class<?> sourceType = filterTextSource.getType();
                Method getFilterText = sourceType.getMethod("getFilterText");

                ClassLoader loader = filterClass.getClassLoader();
                Class<?> ingredientElementClass = Class.forName(
                        "mezz.jei.gui.overlay.elements.IngredientElement", false, loader);
                Constructor<?> constructor = null;
                for (Constructor<?> candidate : ingredientElementClass.getConstructors()) {
                    if (candidate.getParameterCount() == 1) {
                        constructor = candidate;
                        break;
                    }
                }
                if (constructor == null) {
                    throw new NoSuchMethodException("IngredientElement one-argument constructor");
                }

                reflection = new Reflection(
                        filterTextSource,
                        ingredientListCached,
                        searchIndexDirty,
                        sortIndexesDirty,
                        getIngredientListUncached,
                        getFilterText,
                        constructor
                );
                return reflection;
            } catch (ReflectiveOperationException | LinkageError exception) {
                disable(exception);
                return null;
            }
        }
    }

    private static void disable(Throwable throwable) {
        if (!unavailable) {
            unavailable = true;
            LOGGER.warn("Seaborgium async JEI filtering disabled because this JEI build does not match the compatibility path", throwable);
        }
    }

    private static final class State {
        private String requestedFilter;
        private String readyFilter;
        private long requestGeneration;
        private List<?> ready;
    }

    private record Reflection(
            Field filterTextSource,
            Field ingredientListCached,
            Field searchIndexDirty,
            Field sortIndexesDirty,
            Method getIngredientListUncached,
            Method getFilterText,
            Constructor<?> ingredientElementCtor
    ) {
    }
}
