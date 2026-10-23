package com.christian34.woodcutter;

import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests that the plugin starts with its config and command.
 *
 * @author Christian34
 */
class PluginLoadTest extends PluginTestBase {

    @Test
    void pluginIsEnabled() {
        assertTrue(plugin.isEnabled());
        assertSame(plugin, WoodCutter.getInstance());
    }

    @Test
    void createsConfigWithDefaults() {
        assertTrue(new File(plugin.getDataFolder(), "config.yml").isFile(), "config.yml has not been created");
        ConfigData config = plugin.getConfigData();
        assertEquals(30, config.getInt(ConfigData.Keys.MIN_LEVEL));
        assertEquals(120, config.getInt(ConfigData.Keys.MAX_BLOCKS));
        assertTrue(config.getBoolean(ConfigData.Keys.REQUIRE_SNEAKING));
        assertEquals(4, config.getList(ConfigData.Keys.DISABLED_WORLDS).size());
        assertEquals(40, config.getDouble(ConfigData.Keys.CHANCE));
        assertFalse(config.getString(ConfigData.Keys.DELAY_MSG).isEmpty());
    }

    @Test
    void missingConfigKeysAreAdded() throws Exception {
        // a config without the delay section
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration old = YamlConfiguration.loadConfiguration(file);
        old.set("config.delay", null);
        old.set("config.maxblocks", 5);
        old.save(file);
        plugin.reload();
        assertEquals(5, plugin.getConfigData().getInt(ConfigData.Keys.MAX_BLOCKS));
        assertEquals(30, plugin.getConfigData().getInt(ConfigData.Keys.DELAY_TIME));
        // settings of newer versions are written to the file
        YamlConfiguration updated = YamlConfiguration.loadConfiguration(file);
        assertTrue(updated.isSet("config.delay.time"));
        assertEquals(5, updated.getInt("config.maxblocks"));
    }

    @Test
    void changedSettingsAreFound() throws Exception {
        // without the changes of the tests
        plugin.reload();
        assertEquals(List.of(), plugin.getConfigData().changedSettings());
        plugin.getConfigData().set(ConfigData.Keys.MAX_BLOCKS, 200);
        // the same chance as the default (40)
        plugin.getConfigData().set(ConfigData.Keys.CHANCE, 40.0);
        assertEquals(List.of(ConfigData.Keys.MAX_BLOCKS), plugin.getConfigData().changedSettings());
    }

    @Test
    void commandIsRegistered() {
        assertNotNull(plugin.getCommand("woodcutter"));
        assertNotNull(server.getCommandMap().getCommand("wcutter"));
    }

}
