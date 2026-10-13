package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.woodcutting.Cutter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * WoodCutter 2026.
 * <p>
 * The placeholders of PlaceholderAPI, e.g. for scoreboards: %woodcutter_enabled%, %woodcutter_level%,
 * %woodcutter_max_blocks% and %woodcutter_delay%. This class must only be loaded if PlaceholderAPI is installed.
 *
 * @author Christian34
 */
public class PlaceholderHook extends PlaceholderExpansion {
    private final WoodCutter instance;

    public PlaceholderHook(WoodCutter instance) {
        this.instance = instance;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "woodcutter";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Christian34";
    }

    @Override
    public @NotNull String getVersion() {
        return instance.getPluginMeta().getVersion();
    }

    /**
     * the placeholders belong to the plugin, they stay registered on a reload of PlaceholderAPI
     */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of("%woodcutter_enabled%", "%woodcutter_level%", "%woodcutter_max_blocks%", "%woodcutter_delay%");
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !(offlinePlayer.getPlayer() instanceof Player player)) return "";
        int level = WoodCutterEnchantment.level(player.getInventory().getItemInMainHand());
        return switch (params.toLowerCase(Locale.ROOT)) {
            // false if the player has switched WoodCutter off (/woodcutter toggle)
            case "enabled" -> Boolean.toString(BlockBreakListener.isEnabled(player));
            // the level of the enchantment of the item in the main hand, 0 without the enchantment
            case "level" -> Integer.toString(level);
            // the amount of logs the player cuts at once with the item in the main hand
            case "max_blocks" -> Integer.toString(Cutter.maxBlocks(player, instance.getConfigData(), level));
            // seconds until the player can cut the next tree, 0 if they can right away
            case "delay" -> Long.toString(instance.remainingDelay(player));
            default -> null;
        };
    }

}
