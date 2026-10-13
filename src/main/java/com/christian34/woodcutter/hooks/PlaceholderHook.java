package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.woodcutting.Cutter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * The placeholders of PlaceholderAPI, e.g. for scoreboards: %woodcutter_enabled%, %woodcutter_level%,
 * %woodcutter_max_blocks%, %woodcutter_delay% and %woodcutter_skill_level%, %woodcutter_skill_xp%,
 * %woodcutter_skill_xp_needed% of the woodcutting skill. This class must only be loaded if PlaceholderAPI is installed.
 *
 * @author Christian34
 */
public class PlaceholderHook extends PlaceholderExpansion {
    private static final Pattern TOP = Pattern.compile("top_(\\d{1,3})_(name|level|xp)");
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
        return List.of("%woodcutter_enabled%", "%woodcutter_level%", "%woodcutter_max_blocks%", "%woodcutter_delay%",
                "%woodcutter_skill_level%", "%woodcutter_skill_xp%", "%woodcutter_skill_xp_needed%",
                "%woodcutter_top_<rank>_name%", "%woodcutter_top_<rank>_level%", "%woodcutter_top_<rank>_xp%");
    }

    /**
     * @return the value of the player with the rank in the woodcutting skill, empty if there is none
     */
    private String top(int rank, String value) {
        Skill skill = instance.getSkill();
        List<Skill.Entry> best = rank < 1 || !skill.isEnabled() ? List.of() : skill.top(rank);
        if (rank < 1 || best.size() < rank) return "";
        Skill.Entry entry = best.get(rank - 1);
        return switch (value) {
            case "name" -> entry.name();
            case "level" -> Integer.toString(skill.levelOf(entry.xp()));
            default -> Long.toString(entry.xp());
        };
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // the ranking needs no player: %woodcutter_top_<rank>_name%, ..._level% and ..._xp%, e.g. for holograms
        Matcher top = TOP.matcher(params.toLowerCase(Locale.ROOT));
        if (top.matches()) return top(Integer.parseInt(top.group(1)), top.group(2));
        if (offlinePlayer == null || !(offlinePlayer.getPlayer() instanceof Player player)) return "";
        int level = WoodCutterEnchantment.level(player.getInventory().getItemInMainHand());
        Skill skill = instance.getSkill();
        return switch (params.toLowerCase(Locale.ROOT)) {
            // false if the player has switched WoodCutter off (/woodcutter toggle)
            case "enabled" -> Boolean.toString(BlockBreakListener.isEnabled(player));
            // the level of the enchantment of the item in the main hand, 0 without the enchantment
            case "level" -> Integer.toString(level);
            // the amount of logs the player cuts at once with the item in the main hand
            case "max_blocks" -> Integer.toString(Cutter.maxBlocks(player, instance.getConfigData(), level));
            // seconds until the player can cut the next tree, 0 if they can right away
            case "delay" -> Long.toString(instance.remainingDelay(player));
            // the level of the woodcutting skill and the experience of the player in it
            case "skill_level" -> Integer.toString(skill.level(player.getUniqueId()));
            case "skill_xp" -> Long.toString(skill.xp(player.getUniqueId()));
            // the experience that is missing for the next level, 0 at the highest level
            case "skill_xp_needed" -> {
                int skillLevel = skill.level(player.getUniqueId());
                yield Long.toString(skillLevel >= skill.maxLevel() ? 0 : skill.xpForLevel(skillLevel + 1) - skill.xp(player.getUniqueId()));
            }
            default -> null;
        };
    }

}
