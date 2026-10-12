package com.christian34.woodcutter.utils;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WoodCutter 2026.
 * <p>
 * Counts how the plugin is used for the anonymous statistics on bstats.org. The counters are reset whenever bStats
 * reads them, so every value is the usage since the last report. They are thread-safe, as Folia cuts trees on the
 * threads of its regions.
 *
 * @author Christian34
 */
public final class Statistics {
    private static final AtomicInteger TREES = new AtomicInteger();
    private static final AtomicInteger LOGS = new AtomicInteger();
    private static final AtomicInteger ENCHANTMENTS = new AtomicInteger();
    private static final Map<String, Integer> TREE_TYPES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> AXES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ENCHANTMENT_SOURCES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ENCHANTMENT_LEVELS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> USAGE = new ConcurrentHashMap<>();

    private Statistics() {
    }

    /**
     * @param log   the block the player has broken
     * @param axe   the tool of the player
     * @param logs  the amount of logs of the tree (without the broken block)
     * @param level the level of the enchantment, 0 if the axe is not enchanted (the player has WoodCutter as a goodie)
     */
    public static void recordTree(@NotNull Material log, @NotNull Material axe, int logs, int level) {
        TREES.incrementAndGet();
        LOGS.addAndGet(logs);
        TREE_TYPES.merge(treeType(log), 1, Integer::sum);
        AXES.merge(axe.name().toLowerCase(Locale.ROOT), 1, Integer::sum);
        USAGE.merge(level == 0 ? "goodie" : "level " + level, 1, Integer::sum);
    }

    /**
     * @param source how the item got the enchantment: table, command or give
     */
    public static void recordEnchantment(@NotNull String source, int level) {
        ENCHANTMENTS.incrementAndGet();
        ENCHANTMENT_SOURCES.merge(source, 1, Integer::sum);
        ENCHANTMENT_LEVELS.merge("level " + level, 1, Integer::sum);
    }

    public static int takeTrees() {
        return TREES.getAndSet(0);
    }

    public static int takeLogs() {
        return LOGS.getAndSet(0);
    }

    public static int takeEnchantments() {
        return ENCHANTMENTS.getAndSet(0);
    }

    public static Map<String, Integer> takeTreeTypes() {
        return take(TREE_TYPES);
    }

    public static Map<String, Integer> takeAxes() {
        return take(AXES);
    }

    public static Map<String, Integer> takeEnchantmentSources() {
        return take(ENCHANTMENT_SOURCES);
    }

    public static Map<String, Integer> takeEnchantmentLevels() {
        return take(ENCHANTMENT_LEVELS);
    }

    public static Map<String, Integer> takeUsage() {
        return take(USAGE);
    }

    private static Map<String, Integer> take(Map<String, Integer> counters) {
        Map<String, Integer> values = new HashMap<>();
        for (String key : counters.keySet()) {
            Integer value = counters.remove(key);
            if (value != null) values.put(key, value);
        }
        return values;
    }

    /**
     * @return the wood of a log, e.g. dark_oak for STRIPPED_DARK_OAK_WOOD or crimson for CRIMSON_STEM
     */
    public static String treeType(Material log) {
        String name = log.name().toLowerCase(Locale.ROOT);
        if (name.startsWith("stripped_")) name = name.substring("stripped_".length());
        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
            if (name.endsWith(suffix)) return name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    /**
     * groups a number for a pie chart, e.g. range(25, 10, 30, 60) is "11-30" and range(90, 10, 30, 60) is "60+"
     */
    public static String range(int value, int... limits) {
        if (value <= 0) return "0";
        int from = 1;
        for (int limit : limits) {
            if (value <= limit) return from == limit ? Integer.toString(limit) : from + "-" + limit;
            from = limit + 1;
        }
        return (from - 1) + "+";
    }

}
