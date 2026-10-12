package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
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
    HELP_GIVE("help_give");

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

    public void sendActionBar(@NotNull Player receiver, String... replacements) {
        String text = getText(replacements);
        if (!text.isEmpty()) receiver.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }

}
