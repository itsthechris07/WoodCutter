package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * The logs trees are made of: the logs of Minecraft without the ones of the setting 'excluded-logs' (names or
 * patterns like stripped_*), which are never cut.
 *
 * @author Christian34
 */
final class TreeBlocks {
    private static volatile Excluded excluded = new Excluded(List.of(), Set.of());

    private TreeBlocks() {
    }

    /**
     * @return true if the block is a log trees can be cut by
     */
    static boolean isLog(Material type) {
        return Tag.LOGS.isTagged(type) && !excluded().contains(type);
    }

    private static Set<Material> excluded() {
        WoodCutter instance = WoodCutter.getInstance();
        if (instance == null || instance.getConfigData() == null) return Set.of();
        List<String> names = instance.getConfigData().getList(ConfigData.Keys.EXCLUDED_LOGS);
        Excluded current = excluded;
        // read again only when the setting has changed
        if (!current.names().equals(names)) {
            current = new Excluded(names, materials(names));
            excluded = current;
        }
        return current.materials();
    }

    /**
     * @param names names of logs, * stands for any characters, upper and lower case don't matter
     */
    static Set<Material> materials(List<String> names) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Pattern pattern = pattern(name.trim().toLowerCase(Locale.ROOT).replace("minecraft:", ""));
            for (Material log : Tag.LOGS.getValues()) {
                if (pattern.matcher(log.name().toLowerCase(Locale.ROOT)).matches()) materials.add(log);
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

    private record Excluded(List<String> names, Set<Material> materials) {
    }

}
