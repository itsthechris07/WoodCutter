package com.christian34.woodcutter.menu;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.commands.CommandWoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.utils.Debug;
import com.christian34.woodcutter.utils.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * The menu of /woodcutter menu: players switch WoodCutter on and off and see their woodcutting level, its perks and
 * the best players. Admins open a second page that switches the settings of the config.yml on and off.
 *
 * @author Christian34
 */
public class Menu implements InventoryHolder {
    public static final int SLOT_TOGGLE = 11;
    public static final int SLOT_SKILL = 13;
    public static final int SLOT_TOP = 15;
    /**
     * opens the settings on the main page
     */
    public static final int SLOT_PAGE = 26;
    /**
     * opens the main page on the settings
     */
    public static final int SLOT_BACK = 49;
    /**
     * the settings admins can switch on and off, in the order they are shown
     */
    public static final List<String> SETTINGS = List.of(ConfigData.Keys.ANIMATION, ConfigData.Keys.FAST_LEAF_DECAY,
            ConfigData.Keys.REPLANT, ConfigData.Keys.REPLANT_FREE, ConfigData.Keys.DROPS_TO_INVENTORY, ConfigData.Keys.PROTECT_AXE,
            ConfigData.Keys.REQUIRE_LEAVES, ConfigData.Keys.MUSHROOMS, ConfigData.Keys.DESTROY_FAKE_TREES,
            ConfigData.Keys.REQUIRE_SNEAKING, ConfigData.Keys.DELAY, ConfigData.Keys.SKILL);
    /**
     * the icons of the settings, in the order of {@link #SETTINGS}
     */
    private static final List<Material> ICONS = List.of(Material.FEATHER, Material.OAK_LEAVES,
            Material.OAK_SAPLING, Material.EMERALD, Material.HOPPER, Material.SHIELD,
            Material.AZALEA_LEAVES, Material.RED_MUSHROOM_BLOCK, Material.OAK_PLANKS,
            Material.LEATHER_BOOTS, Material.CLOCK, Material.EXPERIENCE_BOTTLE);
    private static final int SIZE = 27;
    private static final int SETTINGS_SIZE = 54;
    /**
     * the settings of the first row, the others are centered in the second one
     */
    private static final int SETTINGS_PER_ROW = 7;
    private static final int TOP_ENTRIES = 10;
    private final WoodCutter instance;
    private final Player player;
    private final Page page;
    private final Inventory inventory;

    public Menu(WoodCutter instance, Player player, Page page) {
        this.instance = instance;
        this.player = player;
        // only admins see the settings
        this.page = page == Page.SETTINGS && !isAdmin() ? Page.MAIN : page;
        Message title = this.page == Page.MAIN ? Message.MENU_TITLE : Message.MENU_SETTINGS_TITLE;
        this.inventory = Bukkit.createInventory(this, this.page == Page.MAIN ? SIZE : SETTINGS_SIZE, component(title.getText()));
        fill();
    }

    /**
     * @return the slot of the icon of a setting, the dye that shows whether it is switched on is in the slot below
     */
    public static int settingSlot(int setting) {
        return setting < SETTINGS_PER_ROW ? 10 + setting : 29 + setting - SETTINGS_PER_ROW;
    }

    /**
     * @return the setting of the icon or the dye in the slot, -1 if there is none
     */
    private static int settingAt(int slot) {
        for (int setting = 0; setting < SETTINGS.size(); setting++) {
            int icon = settingSlot(setting);
            if (slot == icon || slot == icon + 9) return setting;
        }
        return -1;
    }

    /**
     * the background of the menu, without a tooltip
     */
    private static ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glowing(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private static Component component(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(component(name));
        meta.lore(lore.stream().filter(line -> !line.isEmpty()).map(Menu::component).toList());
        // no damage of the axe and the like
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isAdmin() {
        return player.hasPermission(CommandWoodCutter.PERMISSION_ADMIN);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    private void fill() {
        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        if (page == Page.SETTINGS) {
            fillSettings();
            return;
        }
        // a glowing axe while WoodCutter is switched on
        boolean enabled = BlockBreakListener.isEnabled(player);
        ItemStack toggle = item(enabled ? Material.DIAMOND_AXE : Material.BARRIER,
                (enabled ? Message.MENU_TOGGLE_ON : Message.MENU_TOGGLE_OFF).getText(), List.of(Message.MENU_TOGGLE_LORE.getText()));
        inventory.setItem(SLOT_TOGGLE, enabled ? glowing(toggle) : toggle);
        Skill skill = instance.getSkill();
        if (skill.isEnabled()) {
            inventory.setItem(SLOT_SKILL, skillItem(skill));
            inventory.setItem(SLOT_TOP, topItem(skill));
        }
        if (isAdmin()) {
            inventory.setItem(SLOT_PAGE, item(Material.COMPARATOR, Message.MENU_SETTINGS.getText(), List.of(Message.MENU_SETTINGS_LORE.getText())));
        }
    }

    private ItemStack skillItem(Skill skill) {
        long xp = skill.xp(player.getUniqueId());
        int level = skill.levelOf(xp);
        List<String> lore = new ArrayList<>();
        if (level >= skill.maxLevel()) {
            lore.add(Message.MENU_SKILL_MAX.getText("%xp%", Long.toString(xp)));
        } else {
            long reached = skill.xpForLevel(level);
            lore.add(Message.MENU_SKILL_PROGRESS.getText("%current%", Long.toString(xp - reached),
                    "%needed%", Long.toString(skill.xpForLevel(level + 1) - reached)));
        }
        lore.addAll(skill.describePerks(player));
        // the amount shows the level
        ItemStack item = item(Material.EXPERIENCE_BOTTLE, Message.MENU_SKILL.getText("%level%", Integer.toString(level)), lore);
        item.setAmount(Math.clamp(level, 1, item.getMaxStackSize()));
        return item;
    }

    private ItemStack topItem(Skill skill) {
        List<String> lore = new ArrayList<>();
        int rank = 1;
        for (Skill.Entry entry : skill.top(TOP_ENTRIES)) {
            lore.add(Message.SKILL_TOP_ENTRY.getText("%rank%", Integer.toString(rank++), "%player%", entry.name(),
                    "%level%", Integer.toString(skill.levelOf(entry.xp())), "%xp%", Long.toString(entry.xp())));
        }
        // a crown for the best
        return item(Material.GOLDEN_HELMET, Message.MENU_TOP.getText(), lore);
    }

    private void fillSettings() {
        ConfigData config = instance.getConfigData();
        for (int i = 0; i < SETTINGS.size(); i++) {
            String setting = SETTINGS.get(i);
            boolean enabled = config.getBoolean(setting);
            String name = (enabled ? Message.MENU_SETTING_ON : Message.MENU_SETTING_OFF).getText("%setting%", setting);
            List<String> lore = List.of(Message.MENU_SETTING_LORE.getText());
            // an icon for the setting, the dye below shows whether it is switched on
            ItemStack icon = item(ICONS.get(i), name, lore);
            inventory.setItem(settingSlot(i), enabled ? glowing(icon) : icon);
            inventory.setItem(settingSlot(i) + 9, item(enabled ? Material.LIME_DYE : Material.GRAY_DYE, name, lore));
        }
        inventory.setItem(SLOT_BACK, item(Material.ARROW, Message.MENU_BACK.getText(), List.of()));
    }

    /**
     * called when the player clicks a slot of the menu
     */
    public void click(int slot) {
        if (page == Page.MAIN && slot == SLOT_PAGE) {
            if (isAdmin()) new Menu(instance, player, Page.SETTINGS).open();
        } else if (page == Page.SETTINGS && slot == SLOT_BACK) {
            new Menu(instance, player, Page.MAIN).open();
        } else if (page == Page.MAIN && slot == SLOT_TOGGLE) {
            if (!player.hasPermission(BlockBreakListener.PERMISSION_USE)) {
                Message.NO_PERMISSION.send(player);
                return;
            }
            BlockBreakListener.setEnabled(player, !BlockBreakListener.isEnabled(player));
            fill();
        } else if (page == Page.SETTINGS && settingAt(slot) >= 0 && isAdmin()) {
            String setting = SETTINGS.get(settingAt(slot));
            try {
                instance.getConfigData().set(setting, !instance.getConfigData().getBoolean(setting));
            } catch (IOException | RuntimeException ex) {
                Debug.warn("Couldn't save file 'config.yml': " + ex);
            }
            fill();
        }
    }

    public enum Page {
        MAIN, SETTINGS
    }

}
