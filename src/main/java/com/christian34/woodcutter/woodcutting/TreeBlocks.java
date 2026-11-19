package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Statistics;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * The blocks trees are made of: the logs and leaves of Minecraft and the ones of the settings 'extra-logs' and
 * 'extra-leaves', without the logs of 'excluded-logs' (names or patterns like stripped_*), which are never cut. Also
 * the blocks on trees that are removed with them ('attached-blocks') and the saplings of the logs.
 *
 * @author Christian34
 */
final class TreeBlocks {
    /**
     * the blocks of the settings, read again only when a setting has changed
     */
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private TreeBlocks() {
    }

    /**
     * @return true if the block is a log trees can be cut by
     */
    static boolean isLog(Material type) {
        return isAnyLog(type) && !blocks(ConfigData.Keys.EXCLUDED_LOGS).contains(type);
    }

    /**
     * @return true for all logs, also the excluded ones - they keep the leaves of a tree
     */
    static boolean isAnyLog(Material type) {
        return Tag.LOGS.isTagged(type) || blocks(ConfigData.Keys.EXTRA_LOGS).contains(type);
    }

    static boolean isLeaves(Material type) {
        return Tag.LEAVES.isTagged(type) || blocks(ConfigData.Keys.EXTRA_LEAVES).contains(type);
    }

    /**
     * @return true for blocks on the logs of a tree that are removed with it, like cocoa or vines
     */
    static boolean isAttached(Material type) {
        return blocks(ConfigData.Keys.ATTACHED_BLOCKS).contains(type);
    }

    /**
     * @return the sapling (or fungus) that grows the tree of the log - the one of the setting 'saplings' first -,
     * null if there is none
     */
    @Nullable
    static Material sapling(Material log) {
        ConfigData config = config();
        if (config != null) {
            for (Map.Entry<String, String> entry : config.getMap(ConfigData.Keys.SAPLINGS).entrySet()) {
                if (Material.matchMaterial(entry.getKey()) == log) {
                    Material sapling = Material.matchMaterial(entry.getValue());
                    if (sapling != null && sapling.isBlock()) return sapling;
                }
            }
        }
        String type = Statistics.treeType(log);
        String name = switch (type) {
            case "mangrove" -> "mangrove_propagule";
            case "crimson", "warped" -> type + "_fungus";
            default -> type + "_sapling";
        };
        return Material.matchMaterial(name);
    }

    @Nullable
    private static ConfigData config() {
        WoodCutter instance = WoodCutter.getInstance();
        return instance == null ? null : instance.getConfigData();
    }

    private static Set<Material> blocks(String key) {
        ConfigData config = config();
        if (config == null) return Set.of();
        List<String> names = config.getList(key);
        Cached cached = CACHE.get(key);
        if (cached == null || !cached.names().equals(names)) {
            cached = new Cached(names, materials(names, blocks()));
            CACHE.put(key, cached);
        }
        return cached.materials();
    }

    /**
     * @return all blocks of the game
     */
    private static List<Material> blocks() {
        return Arrays.stream(Material.values()).filter(type -> !type.name().startsWith("LEGACY_") && type.isBlock()).toList();
    }

    /**
     * @param names      names of blocks, * stands for any characters, upper and lower case don't matter
     * @param candidates the blocks the names may stand for
     */
    static Set<Material> materials(List<String> names, Iterable<Material> candidates) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Pattern pattern = pattern(name.trim().toLowerCase(Locale.ROOT).replace("minecraft:", ""));
            for (Material type : candidates) {
                if (pattern.matcher(type.name().toLowerCase(Locale.ROOT)).matches()) materials.add(type);
            }
        }
        return materials;
    }

    private static Pattern pattern(String name) {
        StringBuilder regex = new StringBuilder();
        for (String part : name.split("\\*", -1)) {
            if (!regex.isEmpty()) regex.append(".*");
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }

    private record Cached(List<String> names, Set<Material> materials) {
    }

}
