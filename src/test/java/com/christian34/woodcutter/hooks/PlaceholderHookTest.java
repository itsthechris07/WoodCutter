package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the placeholders of PlaceholderAPI.
 *
 * @author Christian34
 */
class PlaceholderHookTest extends PluginTestBase {

    @Test
    void placeholdersOfThePlayer() {
        setConfig(ConfigData.Keys.MAX_BLOCKS, 120);
        setConfig(ConfigData.Keys.LEVELS_ADDITIONAL_BLOCKS, 60);
        setConfig(ConfigData.Keys.DELAY, true);
        setConfig(ConfigData.Keys.DELAY_TIME, 30);
        PlaceholderHook hook = new PlaceholderHook(plugin);
        PlayerMock player = addCutter("Cutter");
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        WoodCutterEnchantment.apply(axe, 2);
        player.getInventory().setItemInMainHand(axe);

        assertEquals("true", hook.onRequest(player, "enabled"));
        assertEquals("2", hook.onRequest(player, "level"));
        assertEquals("180", hook.onRequest(player, "max_blocks"));
        assertEquals("0", hook.onRequest(player, "delay"));
        assertNull(hook.onRequest(player, "unknown"));

        player.performCommand("woodcutter toggle");
        assertEquals("false", hook.onRequest(player, "enabled"));
        plugin.getDelays().put(player.getUniqueId(), System.currentTimeMillis());
        assertEquals("30", hook.onRequest(player, "delay"));
        player.getInventory().setItemInMainHand(null);
        assertEquals("0", hook.onRequest(player, "level"));
    }

    @Test
    void placeholdersOfTheSkill() {
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 10);
        setConfig(ConfigData.Keys.SKILL_XP_INCREASE, 0);
        PlaceholderHook hook = new PlaceholderHook(plugin);
        PlayerMock player = addCutter("Cutter");
        PlayerMock better = addCutter("Better");
        plugin.getSkill().reward(player, 25);
        plugin.getSkill().reward(better, 40);
        assertEquals("2", hook.onRequest(player, "skill_level"));
        assertEquals("25", hook.onRequest(player, "skill_xp"));
        assertEquals("5", hook.onRequest(player, "skill_xp_needed"));

        // the ranking needs no player
        assertEquals("Better", hook.onRequest(null, "top_1_name"));
        assertEquals("4", hook.onRequest(null, "top_1_level"));
        assertEquals("25", hook.onRequest(null, "top_2_xp"));
        assertEquals("", hook.onRequest(null, "top_3_name"));
        assertEquals("", hook.onRequest(null, "top_0_name"));
    }

    @Test
    void noPlaceholdersWithoutAPlayer() {
        assertEquals("", new PlaceholderHook(plugin).onRequest(null, "enabled"));
    }

}
