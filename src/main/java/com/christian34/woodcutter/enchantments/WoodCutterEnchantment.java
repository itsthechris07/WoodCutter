package com.christian34.woodcutter.enchantments;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WoodCutter 2026.
 * <p>
 * The WoodCutter enchantment with its levels. Enchantments can't be registered through the Bukkit api anymore (they
 * are data-driven since Minecraft 1.21), so the level is a tag in the persistent data of the item, which is shown as
 * a lore line like "WoodCutter II". Items that were enchanted with WoodCutter 2.x only have the lore line and are
 * still accepted (as level I).
 *
 * @author Christian34
 */
public final class WoodCutterEnchantment {
    public static final String NAME = "WoodCutter";
    /**
     * the highest level 'levels.max' can be set to
     */
    public static final int LEVEL_LIMIT = 10;
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private WoodCutterEnchantment() {
    }

    private static NamespacedKey key() {
        return new NamespacedKey(WoodCutter.getInstance(), "woodcutter");
    }

    /**
     * @return the highest level of the config (1 if the levels are disabled)
     */
    public static int maxLevel() {
        int max = WoodCutter.getInstance().getConfigData().getInt(ConfigData.Keys.LEVELS_MAX);
        return Math.max(1, Math.min(LEVEL_LIMIT, max));
    }

    /**
     * @return the level an item gets at an enchanting table: I (60 %), II (30 %) or III (10 %), limited by the
     * highest level
     */
    public static int rollLevel() {
        int random = ThreadLocalRandom.current().nextInt(100);
        return Math.min(maxLevel(), random < 60 ? 1 : random < 90 ? 2 : 3);
    }

    /**
     * @return the level of two items that are combined: the next one for two equal levels, else the higher one
     */
    public static int combine(int first, int second) {
        if (first == second) return Math.min(maxLevel(), first + 1);
        return Math.min(maxLevel(), Math.max(first, second));
    }

    public static boolean isAxe(@Nullable ItemStack item) {
        return item != null && Tag.ITEMS_AXES.isTagged(item.getType());
    }

    /**
     * @return true if the item can get the enchantment at an enchanting table
     */
    public static boolean canEnchantItem(@Nullable ItemStack item) {
        return item != null && (item.getType() == Material.BOOK || isAxe(item));
    }

    public static boolean has(@Nullable ItemStack item) {
        return level(item) > 0;
    }

    /**
     * @return the level of the enchantment of the item, 0 if it has none
     */
    public static int level(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        Integer level = meta.getPersistentDataContainer().get(key(), PersistentDataType.INTEGER);
        if (level != null) return Math.max(1, Math.min(maxLevel(), level));
        List<Component> lore = meta.lore();
        if (lore == null) return 0;
        for (Component line : lore) {
            if (isLore(line)) return 1;
        }
        return 0;
    }

    private static boolean isLore(Component line) {
        String text = PlainTextComponentSerializer.plainText().serialize(line);
        return text.equals(NAME) || text.startsWith(NAME + " ");
    }

    /**
     * the levels are only shown if there is more than one, like vanilla does
     */
    private static Component lore(int level) {
        String text = maxLevel() == 1 ? NAME : NAME + " " + ROMAN[level];
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public static boolean apply(@Nullable ItemStack item) {
        return apply(item, 1);
    }

    /**
     * @return false if the item can't be enchanted (e.g. air)
     */
    public static boolean apply(@Nullable ItemStack item, int level) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return false;
        level = Math.max(1, Math.min(maxLevel(), level));
        meta.getPersistentDataContainer().set(key(), PersistentDataType.INTEGER, level);
        List<Component> lore = withoutLore(meta);
        lore.add(lore(level));
        meta.lore(lore);
        // tools without another enchantment would not glint
        if (!meta.hasEnchants()) meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * removes the enchantment from the item (e.g. in a grindstone)
     */
    public static void remove(@Nullable ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(key());
        List<Component> lore = withoutLore(meta);
        meta.lore(lore.isEmpty() ? null : lore);
        if (!meta.hasEnchants()) meta.setEnchantmentGlintOverride(null);
        item.setItemMeta(meta);
    }

    private static List<Component> withoutLore(ItemMeta meta) {
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(WoodCutterEnchantment::isLore);
        return lore;
    }

}
