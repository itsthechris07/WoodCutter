package com.christian34.woodcutter.files;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.utils.Message;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the migration of old configs and the messages.yml.
 *
 * @author Christian34
 */
class FilesTest extends PluginTestBase {

    @Test
    void probabilityOfOlderVersionsBecomesChance() throws Exception {
        // 2.x: 7 = 70 %
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration old = YamlConfiguration.loadConfiguration(file);
        old.set("config.chance", null);
        old.set("config.probability", 7);
        old.save(file);
        plugin.reload();
        assertEquals(70, plugin.getConfigData().getDouble(ConfigData.Keys.CHANCE));
        YamlConfiguration updated = YamlConfiguration.loadConfiguration(file);
        assertFalse(updated.isSet("config.probability"));
        assertEquals(70, updated.getInt("config.chance"));
    }

    @Test
    void messagesAreCreatedAndCanBeChanged() throws Exception {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        assertTrue(file.isFile(), "messages.yml has not been created");
        assertTrue(Message.RELOADED.getText().contains("Config has been reloaded"));

        YamlConfiguration messages = YamlConfiguration.loadConfiguration(file);
        messages.set("messages.prefix", "&a[WC] ");
        messages.set("messages.reloaded", "%prefix%Neu geladen!");
        messages.set("messages.toggle_off", null);
        messages.save(file);
        plugin.reload();
        assertEquals("§a[WC] Neu geladen!", Message.RELOADED.getText());
        // missing texts are added again
        assertFalse(Message.TOGGLE_OFF.getText().isEmpty());
        assertTrue(YamlConfiguration.loadConfiguration(file).isSet("messages.toggle_off"));
    }

    @Test
    void emptyMessagesAreNotSent() throws Exception {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration messages = YamlConfiguration.loadConfiguration(file);
        messages.set("messages.toggle_off", "");
        messages.save(file);
        plugin.reload();
        PlayerMock player = addPlayer("Guest");
        player.performCommand("woodcutter toggle");
        assertTrue(messages(player).isEmpty());
    }

    @Test
    void placeholdersAreReplaced() {
        assertTrue(Message.PLAYER_NOT_ONLINE.getText("%player%", "Steve").contains("'Steve'"));
    }

}
