package com.christian34.woodcutter.commands;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the command /woodcutter.
 *
 * @author Christian34
 */
class CommandWoodCutterTest extends PluginTestBase {

    @Test
    void showsHelpWithoutArguments() {
        PlayerMock player = addPlayer("Guest");
        assertTrue(player.performCommand("woodcutter"));
        List<String> messages = messages(player);
        assertContains(messages, "/WoodCutter toggle");
        assertTrue(messages.stream().noneMatch(message -> message.contains("reload")), "admin commands shown to a player");

        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.performCommand("woodcutter");
        assertContains(messages(admin), "/WoodCutter give");
    }

    @Test
    void helpLinesAreClickable() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.performCommand("woodcutter");
        List<ClickEvent> clicks = new ArrayList<>();
        Component message;
        while ((message = admin.nextComponentMessage()) != null) {
            if (message.clickEvent() != null) clicks.add(message.clickEvent());
        }
        assertTrue(clicks.contains(ClickEvent.runCommand("/woodcutter toggle")));
        assertTrue(clicks.contains(ClickEvent.runCommand("/woodcutter top")));
        // commands with arguments are only written into the chat
        assertTrue(clicks.contains(ClickEvent.suggestCommand("/woodcutter give ")));
    }

    @Test
    void givesEnchantedItems() {
        PlayerMock player = addPlayer("Lucky");
        // e.g. a reward that is run by the console
        server.dispatchCommand(server.getConsoleSender(), "woodcutter give Lucky");
        server.dispatchCommand(server.getConsoleSender(), "woodcutter give Lucky netherite_axe");
        server.getScheduler().performOneTick();
        ItemStack book = player.getInventory().getItem(0);
        ItemStack axe = player.getInventory().getItem(1);
        assertNotNull(book);
        assertNotNull(axe);
        assertEquals(Material.ENCHANTED_BOOK, book.getType());
        assertEquals(Material.NETHERITE_AXE, axe.getType());
        assertTrue(WoodCutterEnchantment.has(book));
        assertTrue(WoodCutterEnchantment.has(axe));
    }

    @Test
    void giveRejectsOtherItemsAndPlayers() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.performCommand("woodcutter give Admin diamond_pickaxe");
        assertContains(messages(admin), "is no axe");
        admin.performCommand("woodcutter give Nobody");
        assertContains(messages(admin), "is not online");
        assertTrue(admin.getInventory().isEmpty());
    }

    @Test
    void enchantsTheAxeInTheMainHand() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.getInventory().setItemInMainHand(new ItemStack(Material.STONE_AXE));
        admin.performCommand("woodcutter enchant");
        assertContains(messages(admin), "successfully enchanted");
        assertTrue(WoodCutterEnchantment.has(admin.getInventory().getItemInMainHand()));
    }

    @Test
    void levelsCanBeChosen() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.getInventory().setItemInMainHand(new ItemStack(Material.STONE_AXE));
        admin.performCommand("woodcutter enchant 3");
        assertEquals(3, WoodCutterEnchantment.level(admin.getInventory().getItemInMainHand()));
        admin.performCommand("woodcutter enchant 4");
        assertContains(messages(admin), "from 1 to 3");

        admin.performCommand("woodcutter give Admin book 2");
        server.getScheduler().performOneTick();
        assertEquals(2, WoodCutterEnchantment.level(admin.getInventory().getItem(1)));
        assertEquals(List.of("1", "2", "3"), server.getCommandTabComplete(admin, "woodcutter give Admin book "));
    }

    @Test
    void otherItemsAreNotEnchanted() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        admin.getInventory().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));
        admin.performCommand("woodcutter enchant");
        assertContains(messages(admin), "could not be enchanted");
        assertFalse(WoodCutterEnchantment.has(admin.getInventory().getItemInMainHand()));
    }

    @Test
    void needsPermission() {
        PlayerMock player = addPlayer("Guest");
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE_AXE));
        player.performCommand("woodcutter enchant");
        assertContains(messages(player), "do not have the permission");
        assertFalse(WoodCutterEnchantment.has(player.getInventory().getItemInMainHand()));
    }

    @Test
    void reloadReadsTheConfigAgain() throws Exception {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        plugin.getConfig().set("config.maxblocks", 7);
        plugin.saveConfig();
        plugin.getConfig().set("config.maxblocks", 1);
        admin.performCommand("woodcutter reload");
        assertContains(messages(admin), "Config has been reloaded");
        assertEquals(7, plugin.getConfigData().getInt(ConfigData.Keys.MAX_BLOCKS));
    }

    @Test
    void completesSubcommandsForAdmins() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        assertEquals(java.util.List.of("reload"), server.getCommandTabComplete(admin, "woodcutter re"));
        assertTrue(server.getCommandTabComplete(admin, "woodcutter give Admin ").contains("diamond_axe"));
        assertEquals(List.of("level", "menu", "toggle", "top"), server.getCommandTabComplete(addPlayer("Guest"), "woodcutter "));
        setConfig(ConfigData.Keys.SKILL, false);
        assertEquals(List.of("menu", "toggle"), server.getCommandTabComplete(addPlayer("Other"), "woodcutter "));
    }

}
