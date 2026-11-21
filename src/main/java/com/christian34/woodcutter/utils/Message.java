package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.platform.Platform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * All texts of messages.yml, with colors and the placeholder %prefix% replaced.
 *
 * @author Christian34
 */
public enum Message {
    PREFIX("prefix"),
    NO_PERMISSION("no_permission"),
    PLAYERS_ONLY("players_only"),
    RELOADED("reloaded"),
    ENCHANT_SUCCESS("enchant_success"),
    ENCHANT_FAILED("enchant_failed"),
    INVALID_LEVEL("invalid_level"),
    PLAYER_NOT_ONLINE("player_not_online"),
    NO_AXE("no_axe"),
    GIVE_SENT("give_sent"),
    GIVE_RECEIVED_BOOK("give_received_book"),
    GIVE_RECEIVED_AXE("give_received_axe"),
    TOGGLE_ON("toggle_on"),
    TOGGLE_OFF("toggle_off"),
    NOT_ENOUGH_FOOD("not_enough_food"),
    NOT_ENOUGH_EXPERIENCE("not_enough_experience"),
    UPDATE_AVAILABLE("update_available"),
    HELP_TOGGLE("help_toggle"),
    HELP_RELOAD("help_reload"),
    HELP_ENCHANT("help_enchant"),
    HELP_GIVE("help_give"),
    AXE_ALMOST_BROKEN("axe_almost_broken"),
    SNEAK_HINT("sneak_hint"),
    SKILL_XP("skill_xp"),
    SKILL_XP_MAX("skill_xp_max"),
    SKILL_LEVEL_UP("skill_level_up"),
    SKILL_LEVEL_UP_TITLE("skill_level_up_title"),
    SKILL_LEVEL_UP_SUBTITLE("skill_level_up_subtitle"),
    SKILL_DISABLED("skill_disabled"),
    SKILL_NO_DATA("skill_no_data"),
    SKILL_INFO("skill_info"),
    SKILL_INFO_MAX("skill_info_max"),
    SKILL_PERK_SPEED("skill_perk_speed"),
    SKILL_PERK_DELAY("skill_perk_delay"),
    SKILL_PERK_MAX_BLOCKS("skill_perk_max_blocks"),
    SKILL_PERK_BONUS_DROPS("skill_perk_bonus_drops"),
    SKILL_PERK_AXE_SAVER("skill_perk_axe_saver"),
    SKILL_PERK_COSTS("skill_perk_costs"),
    SKILL_PERK_FORESTER_LUCK("skill_perk_forester_luck"),
    SKILL_UNLOCK_REPLANT("skill_unlock_replant"),
    SKILL_UNLOCK_DROPS_TO_INVENTORY("skill_unlock_drops_to_inventory"),
    SKILL_TOP_HEADER("skill_top_header"),
    SKILL_TOP_ENTRY("skill_top_entry"),
    HELP_LEVEL("help_level"),
    HELP_TOP("help_top"),
    HELP_MENU("help_menu"),
    MENU_TITLE("menu_title"),
    MENU_TOGGLE_ON("menu_toggle_on"),
    MENU_TOGGLE_OFF("menu_toggle_off"),
    MENU_TOGGLE_LORE("menu_toggle_lore"),
    MENU_SKILL("menu_skill"),
    MENU_SKILL_PROGRESS("menu_skill_progress"),
    MENU_SKILL_MAX("menu_skill_max"),
    MENU_TOP("menu_top"),
    MENU_SETTINGS("menu_settings"),
    MENU_SETTINGS_LORE("menu_settings_lore"),
    MENU_SETTINGS_TITLE("menu_settings_title"),
    MENU_SETTING_ON("menu_setting_on"),
    MENU_SETTING_OFF("menu_setting_off"),
    MENU_SETTING_LORE("menu_setting_lore"),
    MENU_SETTING_FAST_LEAF_DECAY("menu_setting_fast_leaf_decay"),
    MENU_SETTING_FAST_LEAF_DECAY_LORE("menu_setting_fast_leaf_decay_lore"),
    MENU_SETTING_REPLANT("menu_setting_replant"),
    MENU_SETTING_REPLANT_LORE("menu_setting_replant_lore"),
    MENU_SETTING_REPLANT_FREE("menu_setting_replant_free"),
    MENU_SETTING_REPLANT_FREE_LORE("menu_setting_replant_free_lore"),
    MENU_SETTING_DROPS_TO_INVENTORY("menu_setting_drops_to_inventory"),
    MENU_SETTING_DROPS_TO_INVENTORY_LORE("menu_setting_drops_to_inventory_lore"),
    MENU_SETTING_PROTECT_AXE("menu_setting_protect_axe"),
    MENU_SETTING_PROTECT_AXE_LORE("menu_setting_protect_axe_lore"),
    MENU_SETTING_REQUIRE_LEAVES("menu_setting_require_leaves"),
    MENU_SETTING_REQUIRE_LEAVES_LORE("menu_setting_require_leaves_lore"),
    MENU_SETTING_MUSHROOMS("menu_setting_mushrooms"),
    MENU_SETTING_MUSHROOMS_LORE("menu_setting_mushrooms_lore"),
    MENU_SETTING_DESTROY_FAKE_TREES("menu_setting_destroy_fake_trees"),
    MENU_SETTING_DESTROY_FAKE_TREES_LORE("menu_setting_destroy_fake_trees_lore"),
    MENU_SETTING_REQUIRE_SNEAKING("menu_setting_require_sneaking"),
    MENU_SETTING_REQUIRE_SNEAKING_LORE("menu_setting_require_sneaking_lore"),
    MENU_SETTING_DELAY("menu_setting_delay"),
    MENU_SETTING_DELAY_LORE("menu_setting_delay_lore"),
    MENU_SETTING_SKILL("menu_setting_skill"),
    MENU_SETTING_SKILL_LORE("menu_setting_skill_lore"),
    MENU_SETTING_VALUE("menu_setting_value"),
    MENU_SETTING_CHOICE_LORE("menu_setting_choice_lore"),
    MENU_SETTING_NUMBER_LORE("menu_setting_number_lore"),
    MENU_ANIMATION("menu_animation"),
    MENU_ANIMATION_LORE("menu_animation_lore"),
    MENU_ANIMATION_TITLE("menu_animation_title"),
    MENU_ANIMATION_ENABLED("menu_animation_enabled"),
    MENU_ANIMATION_ENABLED_LORE("menu_animation_enabled_lore"),
    MENU_ANIMATION_MODE("menu_animation_mode"),
    MENU_ANIMATION_MODE_LORE("menu_animation_mode_lore"),
    MENU_ANIMATION_SPEED("menu_animation_speed"),
    MENU_ANIMATION_SPEED_LORE("menu_animation_speed_lore"),
    MENU_ANIMATION_PHYSICS("menu_animation_physics"),
    MENU_ANIMATION_PHYSICS_LORE("menu_animation_physics_lore"),
    MENU_ANIMATION_DIRECTION("menu_animation_direction"),
    MENU_ANIMATION_DIRECTION_LORE("menu_animation_direction_lore"),
    MENU_ANIMATION_DROPS("menu_animation_drops"),
    MENU_ANIMATION_DROPS_LORE("menu_animation_drops_lore"),
    MENU_ANIMATION_KNOCKBACK("menu_animation_knockback"),
    MENU_ANIMATION_KNOCKBACK_LORE("menu_animation_knockback_lore"),
    MENU_ANIMATION_DAMAGE("menu_animation_damage"),
    MENU_ANIMATION_DAMAGE_LORE("menu_animation_damage_lore"),
    MENU_ANIMATION_FLOAT("menu_animation_float"),
    MENU_ANIMATION_FLOAT_LORE("menu_animation_float_lore"),
    MENU_ANIMATION_TIMBER("menu_animation_timber"),
    MENU_ANIMATION_TIMBER_LORE("menu_animation_timber_lore"),
    MENU_RESET("menu_reset"),
    MENU_RESET_LORE("menu_reset_lore"),
    MENU_RESET_ENTRY("menu_reset_entry"),
    MENU_RESET_CLICK("menu_reset_click"),
    MENU_RESET_CONFIRM("menu_reset_confirm"),
    MENU_RESET_NONE("menu_reset_none"),
    MENU_BACK("menu_back"),
    TIMBER("timber"),
    ECONOMY_REWARD("economy_reward"),
    NOT_ENOUGH_MONEY("not_enough_money"),
    SKILL_TREES("skill_trees"),
    CHOPPING("chopping"),
    SAPLING_PROTECTED("sapling_protected");

    /**
     * the colors of the server, with hex colors in the format Spigot uses (§x§r§r§g§g§b§b)
     */
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder().character('§')
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder().character('&')
            .hexColors().build();
    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("</?[a-z#!][a-z0-9_:#.\\-' ]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9a-fk-or]|#[0-9a-f]{6})", Pattern.CASE_INSENSITIVE);
    private final String key;

    Message(String key) {
        this.key = key;
    }

    /**
     * @param replacements pairs of a placeholder and its value, e.g. "%player%", name
     */
    @NotNull
    public String getText(String... replacements) {
        var messages = WoodCutter.getInstance().getMessageData();
        String text = messages.getText(key);
        if (this != PREFIX) text = text.replace("%prefix%", messages.getText(PREFIX.key));
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return format(text);
    }

    /**
     * @param text with color codes (&amp;c, &amp;#ff8800) or MiniMessage tags (&lt;gold&gt;, &lt;gradient:...&gt;)
     * @return the text with the colors of the server (§), hex colors included
     */
    @NotNull
    public static String format(@NotNull String text) {
        if (text.isEmpty()) return text;
        return SECTION.serialize(parse(text));
    }

    /**
     * MiniMessage is used for texts with its tags and without color codes, so texts like "&lt;player&gt;" in a help
     * line with color codes stay as they are
     */
    @NotNull
    static Component parse(@NotNull String text) {
        if (MINI_MESSAGE_TAG.matcher(text).find() && !LEGACY_CODE.matcher(text).find()) {
            return MiniMessage.miniMessage().deserialize(text);
        }
        return AMPERSAND.deserialize(text);
    }

    /**
     * @param text a text of {@link #getText(String...)}
     */
    @NotNull
    public static Component component(@NotNull String text) {
        return SECTION.deserialize(text);
    }

    /**
     * shows the text as a subtitle, without a title - e.g. 'Timber!' when a tree tips over
     */
    public void sendSubtitle(@NotNull Player receiver, String... replacements) {
        String text = getText(replacements);
        if (text.isEmpty()) return;
        Platform.get().showTitle(receiver, "", text, 2, 24, 10);
    }

    public void send(@NotNull CommandSender receiver, String... replacements) {
        String text = getText(replacements);
        if (!text.isEmpty()) receiver.sendMessage(text);
    }

    /**
     * sends the text as a line players can click, e.g. in the help
     *
     * @param command the command with its slash
     * @param run     true runs the command with the click, false writes it into the chat of the player, so they can
     *                add its arguments
     */
    public void sendCommand(@NotNull CommandSender receiver, @NotNull String command, boolean run) {
        String text = getText();
        if (text.isEmpty()) return;
        Platform.get().sendCommand(receiver, text, command, run);
    }

    public void sendActionBar(@NotNull Player receiver, String... replacements) {
        String text = getText(replacements);
        if (!text.isEmpty()) Platform.get().sendActionBar(receiver, text);
    }

}
