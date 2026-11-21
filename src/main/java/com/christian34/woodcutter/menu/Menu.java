package com.christian34.woodcutter.menu;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.commands.CommandWoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.platform.Platform;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.utils.Debug;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Statistics;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * The menu of /woodcutter menu: players switch WoodCutter on and off and see their woodcutting level, its perks and
 * the best players. Admins open a second page that switches the settings of the config.yml on and off, and a third
 * one that changes how trees are cut and fall.
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
     * opens the previous page on the settings and the animation
     */
    public static final int SLOT_BACK = 49;
    /**
     * opens the animation on the settings
     */
    public static final int SLOT_ANIMATION = 53;
    /**
     * sets the settings of the page (settings or animation) back to their default values
     */
    public static final int SLOT_RESET = 45;
    private static final Predicate<ConfigData> ALWAYS = config -> true;
    /**
     * the settings admins can switch on and off on the second page, in the order they are shown
     */
    private static final List<Option> SETTING_OPTIONS = settings(
            setting(ConfigData.Keys.ANIMATION, Message.MENU_ANIMATION_ENABLED, Message.MENU_ANIMATION_ENABLED_LORE, Material.FEATHER),
            setting(ConfigData.Keys.FAST_LEAF_DECAY, Message.MENU_SETTING_FAST_LEAF_DECAY, Message.MENU_SETTING_FAST_LEAF_DECAY_LORE, Material.OAK_LEAVES),
            setting(ConfigData.Keys.REPLANT, Message.MENU_SETTING_REPLANT, Message.MENU_SETTING_REPLANT_LORE, Material.OAK_SAPLING),
            setting(ConfigData.Keys.REPLANT_FREE, Message.MENU_SETTING_REPLANT_FREE, Message.MENU_SETTING_REPLANT_FREE_LORE, Material.EMERALD),
            setting(ConfigData.Keys.DROPS_TO_INVENTORY, Message.MENU_SETTING_DROPS_TO_INVENTORY, Message.MENU_SETTING_DROPS_TO_INVENTORY_LORE, Material.HOPPER),
            setting(ConfigData.Keys.PROTECT_AXE, Message.MENU_SETTING_PROTECT_AXE, Message.MENU_SETTING_PROTECT_AXE_LORE, Material.SHIELD),
            setting(ConfigData.Keys.REQUIRE_LEAVES, Message.MENU_SETTING_REQUIRE_LEAVES, Message.MENU_SETTING_REQUIRE_LEAVES_LORE, Material.AZALEA_LEAVES),
            setting(ConfigData.Keys.MUSHROOMS, Message.MENU_SETTING_MUSHROOMS, Message.MENU_SETTING_MUSHROOMS_LORE, Material.RED_MUSHROOM_BLOCK),
            setting(ConfigData.Keys.DESTROY_FAKE_TREES, Message.MENU_SETTING_DESTROY_FAKE_TREES, Message.MENU_SETTING_DESTROY_FAKE_TREES_LORE, Material.OAK_PLANKS),
            setting(ConfigData.Keys.REQUIRE_SNEAKING, Message.MENU_SETTING_REQUIRE_SNEAKING, Message.MENU_SETTING_REQUIRE_SNEAKING_LORE, Material.LEATHER_BOOTS),
            setting(ConfigData.Keys.DELAY, Message.MENU_SETTING_DELAY, Message.MENU_SETTING_DELAY_LORE, Material.CLOCK),
            setting(ConfigData.Keys.SKILL, Message.MENU_SETTING_SKILL, Message.MENU_SETTING_SKILL_LORE, Material.EXPERIENCE_BOTTLE));
    /**
     * the paths of the settings on the second page, in the order they are shown
     */
    public static final List<String> SETTINGS = SETTING_OPTIONS.stream().map(Option::key).toList();
    private static final Predicate<ConfigData> LAYERS = config -> !topple(config);
    private static final Predicate<ConfigData> TOPPLE = Menu::topple;
    /**
     * the settings of the animation: the general ones in the second row, the ones of 'topple' in the fourth - each
     * only while it changes something
     */
    public static final List<Option> ANIMATION = List.of(
            Option.toggle(ConfigData.Keys.ANIMATION, Message.MENU_ANIMATION_ENABLED, Message.MENU_ANIMATION_ENABLED_LORE,
                    Material.FEATHER, 11, ALWAYS),
            Option.choice(ConfigData.Keys.ANIMATION_MODE, Message.MENU_ANIMATION_MODE, Message.MENU_ANIMATION_MODE_LORE,
                    Material.OAK_LOG, 13, ALWAYS, "layers", "topple"),
            Option.number(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, Message.MENU_ANIMATION_SPEED, Message.MENU_ANIMATION_SPEED_LORE,
                    Material.SUGAR, 15, LAYERS, 1, 20),
            Option.choice(ConfigData.Keys.TOPPLE_PHYSICS, Message.MENU_ANIMATION_PHYSICS, Message.MENU_ANIMATION_PHYSICS_LORE,
                    Material.ANVIL, 28, TOPPLE, "simple", "rigid", "fall-apart"),
            Option.choice(ConfigData.Keys.TOPPLE_DIRECTION, Message.MENU_ANIMATION_DIRECTION, Message.MENU_ANIMATION_DIRECTION_LORE,
                    Material.COMPASS, 29, TOPPLE, "away", "facing", "slope", "random"),
            Option.toggle(ConfigData.Keys.TOPPLE_DROPS_WHERE_IT_LANDS, Message.MENU_ANIMATION_DROPS, Message.MENU_ANIMATION_DROPS_LORE,
                    Material.CHEST, 30, TOPPLE),
            Option.toggle(ConfigData.Keys.TOPPLE_KNOCKBACK, Message.MENU_ANIMATION_KNOCKBACK, Message.MENU_ANIMATION_KNOCKBACK_LORE,
                    Material.PISTON, 31, TOPPLE),
            Option.number(ConfigData.Keys.TOPPLE_DAMAGE, Message.MENU_ANIMATION_DAMAGE, Message.MENU_ANIMATION_DAMAGE_LORE,
                    Material.IRON_SWORD, 32, TOPPLE, 0, 20),
            Option.toggle(ConfigData.Keys.TOPPLE_FLOAT_ON_WATER, Message.MENU_ANIMATION_FLOAT, Message.MENU_ANIMATION_FLOAT_LORE,
                    Material.OAK_BOAT, 33, TOPPLE),
            Option.toggle(ConfigData.Keys.TOPPLE_TIMBER, Message.MENU_ANIMATION_TIMBER, Message.MENU_ANIMATION_TIMBER_LORE,
                    Material.OAK_SIGN, 34, TOPPLE));
    /**
     * the width of a line of a description, in the width of a letter (Chinese characters are two)
     */
    private static final int LINE_WIDTH = 38;
    private static final Pattern LEADING_COLORS = Pattern.compile("^(?:§x(?:§[0-9a-fA-F]){6}|§[0-9a-fk-orA-FK-OR])*");
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
    /**
     * true after the first click on the reset button, the second one resets the settings
     */
    private boolean confirmReset;

    public Menu(WoodCutter instance, Player player, Page page) {
        this.instance = instance;
        this.player = player;
        // only admins see the settings
        this.page = page != Page.MAIN && !isAdmin() ? Page.MAIN : page;
        Message title = switch (this.page) {
            case MAIN -> Message.MENU_TITLE;
            case SETTINGS -> Message.MENU_SETTINGS_TITLE;
            case ANIMATION -> Message.MENU_ANIMATION_TITLE;
        };
        this.inventory = Platform.get().createInventory(this, this.page == Page.MAIN ? SIZE : SETTINGS_SIZE, title.getText());
        fill();
    }

    /**
     * @return the slot of the icon of a setting, the dye that shows whether it is switched on is in the slot below
     */
    public static int settingSlot(int setting) {
        return setting < SETTINGS_PER_ROW ? 10 + setting : 29 + setting - SETTINGS_PER_ROW;
    }

    /**
     * a setting of the second page, its slot is set by {@link #settings(Option...)}
     */
    private static Option setting(String key, Message title, Message description, Material icon) {
        return Option.toggle(key, title, description, icon, -1, ALWAYS);
    }

    /**
     * @return the settings in the slots of their order
     */
    private static List<Option> settings(Option... settings) {
        List<Option> list = new ArrayList<>();
        for (int i = 0; i < settings.length; i++) {
            Option option = settings[i];
            list.add(Option.toggle(option.key(), option.title(), option.description(), option.icon(), settingSlot(i), option.shown()));
        }
        return List.copyOf(list);
    }

    /**
     * splits a description into lines that fit into a tooltip: a new line after each ';' and where a line gets too
     * long - every line has the colors the text starts with
     */
    static List<String> wrap(String text) {
        var matcher = LEADING_COLORS.matcher(text);
        String colors = matcher.lookingAt() ? matcher.group() : "";
        List<String> lines = new ArrayList<>();
        for (String part : text.substring(colors.length()).split("[;；]\\s*")) {
            StringBuilder line = new StringBuilder(part.strip());
            while (width(line) > LINE_WIDTH) {
                // breaks at the last space that fits, or within a word if there is none (e.g. in Chinese)
                int fits = fitting(line);
                int space = line.lastIndexOf(" ", fits);
                int end = space > 0 ? space : fits;
                lines.add(colors + line.substring(0, end).strip());
                line.delete(0, space > 0 ? end + 1 : end);
            }
            if (!line.isEmpty()) lines.add(colors + line);
        }
        return lines;
    }

    /**
     * @return how many characters of the text fit into a line
     */
    private static int fitting(CharSequence text) {
        for (int i = 1; i <= text.length(); i++) {
            if (width(text.subSequence(0, i)) > LINE_WIDTH) return i - 1;
        }
        return text.length();
    }

    private static int width(CharSequence text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                // a color code
                i++;
                continue;
            }
            width += Character.isIdeographic(c) || (c >= '　' && c <= '〿') || (c >= '＀' && c <= '￯') ? 2 : 1;
        }
        return width;
    }

    private static boolean topple(ConfigData config) {
        return config.getString(ConfigData.Keys.ANIMATION_MODE).equalsIgnoreCase("topple");
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

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Platform.get().setDisplayName(meta, name);
        Platform.get().setLore(meta, lore.stream().filter(line -> !line.isEmpty()).toList());
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
        switch (page) {
            case MAIN -> fillMain();
            case SETTINGS -> fillSettings();
            case ANIMATION -> fillAnimation();
        }
    }

    private void fillMain() {
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
                    "%level%", Integer.toString(skill.levelOf(entry.xp())), "%xp%", Long.toString(entry.xp()), "%trees%", Long.toString(entry.trees())));
        }
        // a crown for the best
        return item(Material.GOLDEN_HELMET, Message.MENU_TOP.getText(), lore);
    }

    private void fillSettings() {
        fillOptions(SETTING_OPTIONS);
        inventory.setItem(SLOT_RESET, resetItem());
        inventory.setItem(SLOT_BACK, item(Material.ARROW, Message.MENU_BACK.getText(), List.of()));
        inventory.setItem(SLOT_ANIMATION, item(Material.OAK_LOG, Message.MENU_ANIMATION.getText(), List.of(Message.MENU_ANIMATION_LORE.getText())));
    }

    private void fillAnimation() {
        fillOptions(ANIMATION);
        inventory.setItem(SLOT_RESET, resetItem());
        inventory.setItem(SLOT_BACK, item(Material.ARROW, Message.MENU_BACK.getText(), List.of()));
    }

    private void fillOptions(List<Option> options) {
        ConfigData config = instance.getConfigData();
        for (Option option : options) {
            if (!option.shown().test(config)) continue;
            // an icon for the setting, the item below shows its value
            List<String> lore = option.lore();
            ItemStack icon = item(option.icon(), option.name(config), lore);
            inventory.setItem(option.slot(), option.isOn(config) ? glowing(icon) : icon);
            ItemStack value = item(option.indicator(config), option.name(config), lore);
            // the amount shows a number
            if (option.min() >= 0) value.setAmount(Math.clamp(option.number(config), 1, value.getMaxStackSize()));
            inventory.setItem(option.slot() + 9, value);
        }
    }

    /**
     * @return the setting shown in the slot (its icon or the item below), null if there is none
     */
    @Nullable
    private Option optionAt(List<Option> options, int slot) {
        ConfigData config = instance.getConfigData();
        for (Option option : options) {
            if ((slot == option.slot() || slot == option.slot() + 9) && option.shown().test(config)) return option;
        }
        return null;
    }

    /**
     * called when the player left-clicks a slot of the menu
     */
    public void click(int slot) {
        click(slot, false);
    }

    /**
     * called when the player clicks a slot of the menu
     *
     * @param right true for a right click, which goes back to the previous value of a setting of the animation
     */
    public void click(int slot, boolean right) {
        if (page == Page.MAIN && slot == SLOT_PAGE) {
            if (isAdmin()) {
                new Menu(instance, player, Page.SETTINGS).open();
                Statistics.recordMenu("settings opened");
            }
        } else if (page == Page.SETTINGS && slot == SLOT_BACK) {
            new Menu(instance, player, Page.MAIN).open();
        } else if (page == Page.SETTINGS && slot == SLOT_ANIMATION) {
            new Menu(instance, player, Page.ANIMATION).open();
            Statistics.recordMenu("animation opened");
        } else if (page == Page.ANIMATION && slot == SLOT_BACK) {
            new Menu(instance, player, Page.SETTINGS).open();
        } else if (page == Page.MAIN && slot == SLOT_TOGGLE) {
            if (!player.hasPermission(BlockBreakListener.PERMISSION_USE)) {
                Message.NO_PERMISSION.send(player);
                return;
            }
            BlockBreakListener.setEnabled(player, !BlockBreakListener.isEnabled(player));
            Statistics.recordMenu("toggled");
            fill();
        } else if (page != Page.MAIN && slot == SLOT_RESET && isAdmin()) {
            resetClicked();
        } else if (page != Page.MAIN && isAdmin()) {
            Option option = optionAt(options(), slot);
            if (option == null) return;
            confirmReset = false;
            change(option.key(), option.next(instance.getConfigData(), right));
        }
    }

    /**
     * saves a setting in the config.yml and shows the new value
     */
    private void change(String setting, Object value) {
        try {
            instance.getConfigData().set(setting, value);
            Statistics.recordMenu("setting changed");
        } catch (IOException | RuntimeException ex) {
            Debug.warn("Couldn't save file 'config.yml': " + ex);
        }
        fill();
    }

    /**
     * @return the settings of the page
     */
    private List<Option> options() {
        return page == Page.ANIMATION ? ANIMATION : SETTING_OPTIONS;
    }

    /**
     * @return the settings of the page that differ from the default config
     */
    private List<Option> changedOptions() {
        ConfigData config = instance.getConfigData();
        return options().stream().filter(option -> !config.isDefault(option.key())).toList();
    }

    /**
     * the first click asks to click again, the second one sets the settings of the page back to their default values
     */
    private void resetClicked() {
        List<Option> changed = changedOptions();
        if (changed.isEmpty()) return;
        if (!confirmReset) {
            confirmReset = true;
            fill();
            return;
        }
        confirmReset = false;
        ConfigData config = instance.getConfigData();
        Map<String, Object> defaults = new LinkedHashMap<>();
        for (Option option : changed) defaults.put(option.key(), config.getDefault(option.key()));
        try {
            config.set(defaults);
            Statistics.recordMenu("settings reset");
        } catch (IOException | RuntimeException ex) {
            Debug.warn("Couldn't save file 'config.yml': " + ex);
        }
        fill();
    }

    /**
     * a button that sets the settings of the page back to their default values, with the ones that differ
     */
    private ItemStack resetItem() {
        List<Option> changed = changedOptions();
        if (changed.isEmpty()) {
            return item(Material.RECOVERY_COMPASS, Message.MENU_RESET.getText(), List.of(Message.MENU_RESET_NONE.getText()));
        }
        List<String> lore = new ArrayList<>(wrap(Message.MENU_RESET_LORE.getText()));
        for (Option option : changed) lore.add(Message.MENU_RESET_ENTRY.getText("%setting%", option.title().getText()));
        lore.add(" ");
        lore.add((confirmReset ? Message.MENU_RESET_CONFIRM : Message.MENU_RESET_CLICK).getText());
        ItemStack item = item(Material.RECOVERY_COMPASS, Message.MENU_RESET.getText(), lore);
        return confirmReset ? glowing(item) : item;
    }

    public enum Page {
        MAIN, SETTINGS, ANIMATION
    }

    /**
     * a setting of the animation: switched on and off, one of some values or a number from min to max
     *
     * @param title       its name in the menu
     * @param description what it does, a new line after each ';'
     * @param values      the values of a choice, empty for the others
     * @param min         the lowest number, -1 if it is no number
     * @param shown       whether the setting changes something with the config
     */
    public record Option(String key, Message title, Message description, Material icon, int slot,
                         Predicate<ConfigData> shown, List<String> values, int min, int max) {

        static Option toggle(String key, Message title, Message description, Material icon, int slot,
                             Predicate<ConfigData> shown) {
            return new Option(key, title, description, icon, slot, shown, List.of(), -1, 0);
        }

        static Option choice(String key, Message title, Message description, Material icon, int slot,
                             Predicate<ConfigData> shown, String... values) {
            return new Option(key, title, description, icon, slot, shown, List.of(values), -1, 0);
        }

        static Option number(String key, Message title, Message description, Material icon, int slot,
                             Predicate<ConfigData> shown, int min, int max) {
            return new Option(key, title, description, icon, slot, shown, List.of(), min, max);
        }

        private boolean isToggle() {
            return values.isEmpty() && min < 0;
        }

        /**
         * @return the number of the config within min and max
         */
        int number(ConfigData config) {
            return Math.clamp((int) Math.round(config.getDouble(key)), min, max);
        }

        /**
         * @return the value of a choice in the config, the first one if it is none of them
         */
        private String value(ConfigData config) {
            String value = config.getString(key).trim().toLowerCase(Locale.ROOT).replace('_', '-');
            return values.contains(value) ? value : values.getFirst();
        }

        /**
         * @return whether the icon glows: switched on, a number above 0 - a choice always glows
         */
        boolean isOn(ConfigData config) {
            if (isToggle()) return config.getBoolean(key);
            return min < 0 || number(config) > 0;
        }

        Material indicator(ConfigData config) {
            if (isToggle() || min >= 0) return isOn(config) ? Material.LIME_DYE : Material.GRAY_DYE;
            return Material.LIGHT_BLUE_DYE;
        }

        String name(ConfigData config) {
            String name = title.getText();
            if (isToggle()) {
                return (config.getBoolean(key) ? Message.MENU_SETTING_ON : Message.MENU_SETTING_OFF).getText("%setting%", name);
            }
            String value = min >= 0 ? Integer.toString(number(config)) : value(config);
            return Message.MENU_SETTING_VALUE.getText("%setting%", name, "%value%", value);
        }

        /**
         * @return what the setting does, how it is changed and its path in the config.yml
         */
        List<String> lore() {
            List<String> lore = new ArrayList<>(wrap(description.getText()));
            // an empty line, which the menu would leave out
            if (!lore.isEmpty()) lore.add(" ");
            Message click = isToggle() ? Message.MENU_SETTING_LORE
                    : min >= 0 ? Message.MENU_SETTING_NUMBER_LORE : Message.MENU_SETTING_CHOICE_LORE;
            lore.add(click.getText());
            lore.add(Message.format("&8" + key));
            return lore;
        }

        /**
         * @param back true for the previous value or a lower number
         * @return the value of the setting after a click
         */
        Object next(ConfigData config, boolean back) {
            if (isToggle()) return !config.getBoolean(key);
            if (min >= 0) return Math.clamp(number(config) + (back ? -1 : 1), min, max);
            int index = values.indexOf(value(config)) + (back ? -1 : 1);
            return values.get(Math.floorMod(index, values.size()));
        }
    }

}
