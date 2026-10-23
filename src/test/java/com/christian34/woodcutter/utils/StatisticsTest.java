package com.christian34.woodcutter.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the counters and groups of the bStats charts.
 *
 * @author Christian34
 */
class StatisticsTest {

    @BeforeEach
    void reset() {
        Statistics.takeTrees();
        Statistics.takeLogs();
        Statistics.takeEnchantments();
        Statistics.takeTreeTypes();
        Statistics.takeAxes();
        Statistics.takeEnchantmentSources();
        Statistics.takeUsage();
        Statistics.takeEnchantmentLevels();
        Statistics.takeMenu();
    }

    @Test
    void menuActionsAreCounted() {
        Statistics.recordMenu("opened");
        Statistics.recordMenu("opened");
        Statistics.recordMenu("setting changed");
        assertEquals(Map.of("opened", 2, "setting changed", 1), Statistics.takeMenu());
        assertTrue(Statistics.takeMenu().isEmpty());
    }

    @Test
    void countersAreResetWhenRead() {
        Statistics.recordTree(Material.OAK_LOG, Material.DIAMOND_AXE, 5, 1);
        Statistics.recordTree(Material.STRIPPED_OAK_WOOD, Material.IRON_AXE, 3, 0);
        Statistics.recordTree(Material.WARPED_STEM, Material.IRON_AXE, 10, 2);
        assertEquals(3, Statistics.takeTrees());
        assertEquals(18, Statistics.takeLogs());
        assertEquals(Map.of("oak", 2, "warped", 1), Statistics.takeTreeTypes());
        assertEquals(Map.of("diamond_axe", 1, "iron_axe", 2), Statistics.takeAxes());
        assertEquals(Map.of("level 1", 1, "level 2", 1, "goodie", 1), Statistics.takeUsage());

        assertEquals(0, Statistics.takeTrees());
        assertEquals(0, Statistics.takeLogs());
        assertTrue(Statistics.takeTreeTypes().isEmpty());
        assertTrue(Statistics.takeAxes().isEmpty());
    }

    @Test
    void enchantmentsAreCountedBySource() {
        Statistics.recordEnchantment("table", 1);
        Statistics.recordEnchantment("table", 1);
        Statistics.recordEnchantment("command", 3);
        assertEquals(3, Statistics.takeEnchantments());
        assertEquals(Map.of("table", 2, "command", 1), Statistics.takeEnchantmentSources());
        assertEquals(Map.of("level 1", 2, "level 3", 1), Statistics.takeEnchantmentLevels());
    }

    @Test
    void treeTypeIsTheWood() {
        assertEquals("dark_oak", Statistics.treeType(Material.STRIPPED_DARK_OAK_WOOD));
        assertEquals("cherry", Statistics.treeType(Material.CHERRY_LOG));
        assertEquals("crimson", Statistics.treeType(Material.CRIMSON_HYPHAE));
        assertEquals("warped", Statistics.treeType(Material.STRIPPED_WARPED_STEM));
    }

    @Test
    void rangesGroupNumbers() {
        assertEquals("0", Statistics.range(0, 10, 30));
        assertEquals("1-10", Statistics.range(10, 10, 30));
        assertEquals("11-30", Statistics.range(11, 10, 30));
        assertEquals("30+", Statistics.range(31, 10, 30));
        assertEquals("1", Statistics.range(1, 1, 2, 5));
        assertEquals("2", Statistics.range(2, 1, 2, 5));
        assertEquals("3-5", Statistics.range(4, 1, 2, 5));
    }

}
