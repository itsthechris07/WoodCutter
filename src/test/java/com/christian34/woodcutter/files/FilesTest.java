package com.christian34.woodcutter.files;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.utils.Message;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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
    void missingSettingsAreAddedWithTheirComments() throws Exception {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration old = YamlConfiguration.loadConfiguration(file);
        old.set("config.maxblocks", 77);
        old.set("config.disabled-worlds", List.of("city"));
        old.set("config.delay.message", "&cWait %time%s");
        old.set("config.animation", null);
        old.set("config.removed-setting", true);
        old.save(file);
        plugin.reload();

        String text = Files.readString(file.toPath());
        assertTrue(text.startsWith("####") && text.contains("messages.yml\n\nconfig:\n"), "the header is missing: " + text);
        assertTrue(text.contains("# logs per tick (20 ticks are a second)"));
        YamlConfiguration updated = YamlConfiguration.loadConfiguration(file);
        assertEquals(2, updated.getInt("config.animation.blocks-per-tick"));
        assertEquals(77, updated.getInt("config.maxblocks"));
        assertEquals(List.of("city"), updated.getStringList("config.disabled-worlds"));
        assertEquals("&cWait %time%s", updated.getString("config.delay.message"));
        assertFalse(updated.isSet("config.removed-setting"));
        assertEquals(77, plugin.getConfigData().getInt(ConfigData.Keys.MAX_BLOCKS));
    }

    @Test
    void completeFilesAreNotTouched() throws Exception {
        File file = new File(plugin.getDataFolder(), "config.yml");
        String text = Files.readString(file.toPath()).replace("  level: 30", "  # my own note\n  level:    25");
        Files.writeString(file.toPath(), text);
        plugin.reload();
        assertEquals(text, Files.readString(file.toPath()));
        assertEquals(25, plugin.getConfigData().getInt(ConfigData.Keys.MIN_LEVEL));
    }

    @Test
    void brokenFilesAreNotOverwritten() throws Exception {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        String text = "messages:\n  prefix: '&a[WC]\n  reloaded: [";
        Files.writeString(file.toPath(), text);
        plugin.reload();
        assertEquals(text, Files.readString(file.toPath()));
        assertFalse(Message.RELOADED.getText().isEmpty());
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
