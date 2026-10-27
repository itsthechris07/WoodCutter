package com.christian34.woodcutter.skill;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Debug;
import com.christian34.woodcutter.utils.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WoodCutter 2026.
 * <p>
 * The woodcutting skill: players gain experience for every log of a tree they cut and reach levels. The perks of the
 * config grow evenly with the level, up to their value at the highest level. The experience of all players is kept
 * in the levels.yml, so the ranking contains players who are offline as well.
 *
 * @author Christian34
 */
public class Skill {
    public static final String PERK_SPEED = "speed";
    public static final String PERK_DELAY = "delay";
    public static final String PERK_MAX_BLOCKS = "max-blocks";
    public static final String PERK_BONUS_DROPS = "bonus-drops";
    public static final String PERK_AXE_SAVER = "axe-saver";
    public static final String PERK_COSTS = "costs";
    public static final String PERK_FORESTER_LUCK = "forester-luck";
    public static final String UNLOCK_REPLANT = "replant";
    public static final String UNLOCK_DROPS_TO_INVENTORY = "drops-to-inventory";
    /**
     * the highest level 'skill.max-level' can be set to
     */
    private static final int LEVEL_LIMIT = 1000;
    private static final String FILE = "levels.yml";
    private static final String PLAYERS = "players";
    private final WoodCutter instance;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private volatile boolean changed;

    public Skill(WoodCutter instance) {
        this.instance = instance;
        load();
    }

    private void load() {
        File file = new File(instance.getDataFolder(), FILE);
        if (!file.exists()) return;
        ConfigurationSection players = YamlConfiguration.loadConfiguration(file).getConfigurationSection(PLAYERS);
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            try {
                entries.put(UUID.fromString(key), new Entry(players.getString(key + ".name", "?"), players.getLong(key + ".xp")));
            } catch (IllegalArgumentException ex) {
                Debug.warn("Skipped invalid player '" + key + "' in file '" + FILE + "'");
            }
        }
    }

    /**
     * writes the levels.yml if a player has gained experience since the last time
     */
    public synchronized void save() {
        if (!changed) return;
        this.changed = false;
        YamlConfiguration data = new YamlConfiguration();
        entries.forEach((uuid, entry) -> {
            data.set(PLAYERS + "." + uuid + ".name", entry.name());
            data.set(PLAYERS + "." + uuid + ".xp", entry.xp());
        });
        try {
            // into another file first, so a crash while writing does not destroy the levels of all players
            Path file = instance.getDataFolder().toPath().resolve(FILE);
            Path temporary = file.resolveSibling(FILE + ".tmp");
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, data.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            this.changed = true;
            Debug.warn("Couldn't save file '" + FILE + "': " + ex);
        }
    }

    public boolean isEnabled() {
        return instance.getConfigData().getBoolean(ConfigData.Keys.SKILL);
    }

    public int maxLevel() {
        return Math.max(1, Math.min(LEVEL_LIMIT, instance.getConfigData().getInt(ConfigData.Keys.SKILL_MAX_LEVEL)));
    }

    /**
     * @return the experience a player needs in total to reach the level: every level needs 'xp-increase' more than
     * the one before
     */
    public long xpForLevel(int level) {
        ConfigData config = instance.getConfigData();
        long first = Math.max(1, config.getInt(ConfigData.Keys.SKILL_XP_FIRST_LEVEL));
        long increase = Math.max(0, config.getInt(ConfigData.Keys.SKILL_XP_INCREASE));
        return level * first + increase * level * (level - 1) / 2;
    }

    /**
     * @return the level of a player with the experience, 0 to the highest level
     */
    public int levelOf(long xp) {
        int max = maxLevel();
        int level = 0;
        while (level < max && xp >= xpForLevel(level + 1)) level++;
        return level;
    }

    public long xp(UUID player) {
        Entry entry = entries.get(player);
        return entry == null ? 0 : entry.xp();
    }

    public int level(UUID player) {
        return levelOf(xp(player));
    }

    /**
     * @return the value of the perk for the player: 0 without a level (or with the skill disabled) up to the value
     * of the config at the highest level
     */
    public double perk(Player player, String perk) {
        return perk(level(player.getUniqueId()), perk);
    }

    private double perk(int level, String perk) {
        if (!isEnabled()) return 0;
        double value = Math.max(0, instance.getConfigData().getDouble(ConfigData.Keys.SKILL_PERKS + perk));
        return value * level / maxLevel();
    }

    /**
     * @return true if the player has reached the level that unlocks the feature (0 in the config: never)
     */
    public boolean isUnlocked(Player player, String unlock) {
        return isUnlocked(level(player.getUniqueId()), unlock);
    }

    private boolean isUnlocked(int level, String unlock) {
        if (!isEnabled()) return false;
        int needed = instance.getConfigData().getInt(ConfigData.Keys.SKILL_UNLOCKS + unlock);
        return needed > 0 && level >= needed;
    }

    /**
     * @return a text for every perk and unlocked feature the player has, perks they don't have (yet) are left out
     */
    public List<String> describePerks(Player player) {
        return describePerks(level(player.getUniqueId()));
    }

    private List<String> describePerks(int level) {
        List<String> lines = new ArrayList<>();
        addPerk(lines, Message.SKILL_PERK_SPEED, Math.round(perk(level, PERK_SPEED)));
        addPerk(lines, Message.SKILL_PERK_DELAY, perk(level, PERK_DELAY));
        addPerk(lines, Message.SKILL_PERK_MAX_BLOCKS, Math.floor(perk(level, PERK_MAX_BLOCKS)));
        addPerk(lines, Message.SKILL_PERK_BONUS_DROPS, perk(level, PERK_BONUS_DROPS));
        addPerk(lines, Message.SKILL_PERK_AXE_SAVER, perk(level, PERK_AXE_SAVER));
        addPerk(lines, Message.SKILL_PERK_COSTS, perk(level, PERK_COSTS));
        addPerk(lines, Message.SKILL_PERK_FORESTER_LUCK, perk(level, PERK_FORESTER_LUCK));
        if (isUnlocked(level, UNLOCK_REPLANT)) lines.add(Message.SKILL_UNLOCK_REPLANT.getText());
        if (isUnlocked(level, UNLOCK_DROPS_TO_INVENTORY)) lines.add(Message.SKILL_UNLOCK_DROPS_TO_INVENTORY.getText());
        lines.removeIf(String::isEmpty);
        return lines;
    }

    private static void addPerk(List<String> lines, Message message, double value) {
        // one decimal place, whole numbers without it
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded <= 0) return;
        lines.add(message.getText("%value%", rounded == Math.rint(rounded) ? Long.toString((long) rounded) : Double.toString(rounded)));
    }

    /**
     * gives the player the experience for a cut tree, shows the progress over the action bar and announces a new level
     *
     * @param logs the amount of logs of the tree
     */
    public void reward(Player player, int logs) {
        if (!isEnabled()) return;
        long gained = (long) logs * Math.max(0, instance.getConfigData().getInt(ConfigData.Keys.SKILL_XP_PER_LOG));
        if (gained <= 0) return;
        long before = xp(player.getUniqueId());
        long xp = before + gained;
        entries.put(player.getUniqueId(), new Entry(player.getName(), xp));
        this.changed = true;

        int level = levelOf(xp);
        String levelText = Integer.toString(level);
        if (level >= maxLevel()) {
            Message.SKILL_XP_MAX.sendActionBar(player, "%xp%", Long.toString(gained), "%level%", levelText);
        } else {
            long reached = xpForLevel(level);
            Message.SKILL_XP.sendActionBar(player, "%xp%", Long.toString(gained), "%level%", levelText,
                    "%current%", Long.toString(xp - reached), "%needed%", Long.toString(xpForLevel(level + 1) - reached));
        }
        if (level > levelOf(before)) announceLevel(player, levelOf(before), level);
    }

    /**
     * tells the player their new level and what they got for it: the perks that are better than before and the
     * features they have unlocked
     */
    private void announceLevel(Player player, int before, int reached) {
        String level = Integer.toString(reached);
        Message.SKILL_LEVEL_UP.send(player, "%level%", level);
        List<String> perks = describePerks(reached);
        perks.removeAll(describePerks(before));
        perks.forEach(player::sendMessage);
        String title = Message.SKILL_LEVEL_UP_TITLE.getText("%level%", level);
        String subtitle = Message.SKILL_LEVEL_UP_SUBTITLE.getText("%level%", level);
        if (title.isEmpty() && subtitle.isEmpty()) return;
        player.showTitle(Title.title(component(title), component(subtitle)));
        // by its name, as the sounds became a registry in 1.21.3
        player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 1, 1);
    }

    private static Component component(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    /**
     * @return the entry of the player with the name (upper and lower case don't matter), also if they are offline
     */
    @Nullable
    public Entry find(String name) {
        for (Entry entry : entries.values()) {
            if (entry.name().equalsIgnoreCase(name)) return entry;
        }
        return null;
    }

    /**
     * @return the players with the most experience, the best one first
     */
    public List<Entry> top(int amount) {
        return entries.values().stream().sorted(Comparator.comparingLong(Entry::xp).reversed()).limit(amount).toList();
    }

    /**
     * @param name the name the player had when they cut their last tree
     */
    public record Entry(String name, long xp) {
    }

}
