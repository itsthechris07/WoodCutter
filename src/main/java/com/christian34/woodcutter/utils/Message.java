package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    MENU_BACK("menu_back");

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
        return LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
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
        receiver.sendMessage(LegacyComponentSerializer.legacySection().deserialize(text)
                .clickEvent(run ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command, NamedTextColor.GRAY))));
    }

    public void sendActionBar(@NotNull Player receiver, String... replacements) {
        String text = getText(replacements);
        if (!text.isEmpty()) receiver.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }

}
