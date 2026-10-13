package com.christian34.woodcutter;

import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WoodCutter 2026.
 * <p>
 * Starts a mocked Paper server with WoodCutter enabled for every test. Each test gets a fresh plugin data folder.
 *
 * @author Christian34
 */
public abstract class PluginTestBase {
    protected ServerMock server;
    protected WorldMock world;
    protected WoodCutter plugin;

    @BeforeEach
    void setUpServer() {
        this.server = MockBukkit.mock();
        this.world = server.addSimpleWorld("world");
        this.plugin = MockBukkit.load(WoodCutter.class);
        // trees are cut at once, so the tests don't have to wait for the animation
        setConfig("animation.enabled", false);
        // the trees of the tests are just logs
        setConfig("require-leaves", false);
    }

    @AfterEach
    void tearDownServer() {
        MockBukkit.unmock();
    }

    protected PlayerMock addPlayer(String name, String... permissions) {
        PlayerMock player = server.addPlayer(name);
        for (String permission : permissions) {
            player.addAttachment(plugin, permission, true);
        }
        return player;
    }

    /**
     * a sneaking player who is allowed to use WoodCutter, with an enchanted axe in the main hand
     */
    protected PlayerMock addCutter(String name) {
        PlayerMock player = addPlayer(name, "woodcutter.use");
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(enchantedAxe());
        return player;
    }

    protected static ItemStack enchantedAxe() {
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        assertTrue(WoodCutterEnchantment.apply(axe));
        return axe;
    }

    protected void setConfig(String key, Object value) {
        plugin.getConfig().set("config." + key, value);
    }

    /**
     * all messages the sender received since the last call, without colors
     */
    protected List<String> messages(@NotNull MessageTarget target) {
        List<String> messages = new ArrayList<>();
        Component message;
        while ((message = target.nextComponentMessage()) != null) {
            // legacy messages keep their color codes in the text
            messages.add(ChatColor.stripColor(PlainTextComponentSerializer.plainText().serialize(message)));
        }
        return messages;
    }

    protected static void assertContains(List<String> messages, String expected) {
        assertTrue(messages.stream().anyMatch(message -> message.contains(expected)),
                () -> "expected a message containing '" + expected + "' but got " + messages);
    }

}
