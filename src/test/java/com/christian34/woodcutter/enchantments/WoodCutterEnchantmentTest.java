package com.christian34.woodcutter.enchantments;

import com.christian34.woodcutter.PluginTestBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the enchantment tag on items.
 *
 * @author Christian34
 */
class WoodCutterEnchantmentTest extends PluginTestBase {

    @Test
    void appliedEnchantmentIsFound() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        assertFalse(WoodCutterEnchantment.has(axe));
        assertTrue(WoodCutterEnchantment.apply(axe));
        assertTrue(WoodCutterEnchantment.has(axe));
        assertTrue(axe.getItemMeta().getEnchantmentGlintOverride());
    }

    @Test
    void loreIsAddedOnceAndKeepsOtherLines() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.lore(List.of(Component.text("My axe")));
        axe.setItemMeta(meta);
        WoodCutterEnchantment.apply(axe);
        WoodCutterEnchantment.apply(axe);
        List<Component> lore = axe.getItemMeta().lore();
        assertNotNull(lore);
        assertEquals(2, lore.size());
        assertEquals(Component.text("My axe"), lore.get(0));
    }

    @Test
    void itemsOfOlderVersionsAreAccepted() {
        // WoodCutter 2.x only set the lore
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.lore(List.of(LegacyComponentSerializer.legacySection().deserialize("§7WoodCutter")));
        axe.setItemMeta(meta);
        assertTrue(WoodCutterEnchantment.has(axe));
    }

    @Test
    void levelsAreSavedAndShown() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        assertEquals(0, WoodCutterEnchantment.level(axe));
        WoodCutterEnchantment.apply(axe, 2);
        assertEquals(2, WoodCutterEnchantment.level(axe));
        assertEquals("WoodCutter II", plainLore(axe).get(0));
        // a higher level replaces the line
        WoodCutterEnchantment.apply(axe, 3);
        assertEquals(List.of("WoodCutter III"), plainLore(axe));
        // levels above the highest one are limited
        WoodCutterEnchantment.apply(axe, 9);
        assertEquals(3, WoodCutterEnchantment.level(axe));
    }

    @Test
    void levelsCanBeDisabled() {
        setConfig("levels.max", 1);
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        WoodCutterEnchantment.apply(axe, 3);
        assertEquals(1, WoodCutterEnchantment.level(axe));
        assertEquals(List.of("WoodCutter"), plainLore(axe));
        for (int i = 0; i < 50; i++) assertEquals(1, WoodCutterEnchantment.rollLevel());
    }

    @Test
    void equalLevelsCombineToTheNextOne() {
        assertEquals(2, WoodCutterEnchantment.combine(1, 1));
        assertEquals(3, WoodCutterEnchantment.combine(2, 2));
        assertEquals(3, WoodCutterEnchantment.combine(3, 3));
        assertEquals(3, WoodCutterEnchantment.combine(1, 3));
        assertEquals(2, WoodCutterEnchantment.combine(0, 2));
    }

    @Test
    void enchantmentCanBeRemoved() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.lore(List.of(Component.text("My axe")));
        axe.setItemMeta(meta);
        WoodCutterEnchantment.apply(axe, 2);
        WoodCutterEnchantment.remove(axe);
        assertFalse(WoodCutterEnchantment.has(axe));
        assertEquals(List.of("My axe"), plainLore(axe));
        assertFalse(axe.getItemMeta().hasEnchantmentGlintOverride());
    }

    private static List<String> plainLore(ItemStack item) {
        List<Component> lore = item.getItemMeta().lore();
        assertNotNull(lore);
        return lore.stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    @Test
    void onlyAxesAndBooksCanBeEnchanted() {
        assertTrue(WoodCutterEnchantment.canEnchantItem(new ItemStack(Material.WOODEN_AXE)));
        assertTrue(WoodCutterEnchantment.canEnchantItem(new ItemStack(Material.NETHERITE_AXE)));
        assertTrue(WoodCutterEnchantment.canEnchantItem(new ItemStack(Material.BOOK)));
        assertFalse(WoodCutterEnchantment.canEnchantItem(new ItemStack(Material.DIAMOND_PICKAXE)));
        assertFalse(WoodCutterEnchantment.canEnchantItem(null));
    }

    @Test
    void airCanNotBeEnchanted() {
        assertFalse(WoodCutterEnchantment.apply(new ItemStack(Material.AIR)));
        assertFalse(WoodCutterEnchantment.apply(null));
    }

}
