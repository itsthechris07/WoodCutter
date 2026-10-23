package com.christian34.woodcutter.files;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.utils.Debug;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WoodCutter 2026.
 * <p>
 * The config.yml - {@link Keys} contains the paths of all settings (relative to 'config.'). Settings of newer
 * versions are added to existing files on load.
 *
 * @author Christian34
 */
public class ConfigData {
    private static final String FILE = "config.yml";
    private static final String DATA_PREFIX = "config.";
    private final WoodCutter instance;
    private volatile FileConfiguration data;

    public ConfigData(WoodCutter instance) {
        this.instance = instance;
        load();
    }

    /**
     * creates the config.yml if it does not exist, adds missing settings and loads it (again)
     */
    public void load() {
        instance.saveDefaultConfig();
        try {
            // adds the settings (and comments) of the default config that are missing in the file, e.g. after an update
            File file = new File(instance.getDataFolder(), FILE);
            YamlUpdater.update(instance, FILE, file, migrate(file));
        } catch (IOException | RuntimeException ex) {
            // missing settings have the value of the default config then
            Debug.warn("Couldn't update file '" + FILE + "': " + ex);
        }
        instance.reloadConfig();
        this.data = instance.getConfig();
    }

    /**
     * converts settings of older versions: 'probability' (4 = 40 %) became 'chance' in percent
     *
     * @return true if the file has been changed
     */
    private boolean migrate(File file) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String probability = DATA_PREFIX + "probability";
        if (!config.contains(probability)) return false;
        if (!config.contains(DATA_PREFIX + Keys.CHANCE)) {
            config.set(DATA_PREFIX + Keys.CHANCE, Math.max(1, Math.min(10, config.getInt(probability))) * 10);
        }
        config.set(probability, null);
        // the comments are restored by the update
        config.save(file);
        return true;
    }

    /**
     * changes a setting in the config.yml (its comments stay) and loads the file again
     */
    public void set(@NotNull String key, Object value) throws IOException {
        File file = new File(instance.getDataFolder(), FILE);
        String text = Files.readString(file.toPath());
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException ex) {
            throw new IOException(ex);
        }
        config.set(DATA_PREFIX + key, value);
        YamlUpdater.write(config, YamlUpdater.header(text), file);
        load();
    }

    @NotNull
    public String getString(@NotNull String key) {
        return data.getString(DATA_PREFIX + key, "");
    }

    public int getInt(@NotNull String key) {
        return data.getInt(DATA_PREFIX + key);
    }

    public double getDouble(@NotNull String key) {
        return data.getDouble(DATA_PREFIX + key);
    }

    public boolean getBoolean(@NotNull String key) {
        return data.getBoolean(DATA_PREFIX + key);
    }

    @NotNull
    public List<String> getList(@NotNull String key) {
        return data.getStringList(DATA_PREFIX + key);
    }

    /**
     * @return the settings (e.g. replant.enabled) that differ from the default config of the jar
     */
    @NotNull
    public List<String> changedSettings() {
        FileConfiguration config = data;
        Configuration defaults = config.getDefaults();
        if (defaults == null) return List.of();
        List<String> changed = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (!key.startsWith(DATA_PREFIX) || defaults.isConfigurationSection(key)) continue;
            if (!same(defaults.get(key), config.get(key))) changed.add(key.substring(DATA_PREFIX.length()));
        }
        return changed;
    }

    /**
     * 40 and 40.0 are the same chance
     */
    private static boolean same(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) return x.doubleValue() == y.doubleValue();
        return Objects.equals(a, b);
    }

    public static final class Keys {
        public static final String DISABLED_WORLDS = "disabled-worlds";
        public static final String MIN_LEVEL = "level";
        public static final String CHANCE = "chance";
        public static final String MAX_BLOCKS = "maxblocks";
        public static final String LEVELS_MAX = "levels.max";
        public static final String LEVELS_ADDITIONAL_BLOCKS = "levels.additional-blocks";
        public static final String DESTROY_FAKE_TREES = "destroy-fake-trees";
        public static final String REQUIRE_SNEAKING = "require-sneaking";
        public static final String DELAY = "delay.enabled";
        public static final String DELAY_TIME = "delay.time";
        public static final String DELAY_MSG = "delay.message";
        public static final String GOODIE = "goodie";
        public static final String ANIMATION = "animation.enabled";
        public static final String ANIMATION_BLOCKS_PER_TICK = "animation.blocks-per-tick";
        public static final String FAST_LEAF_DECAY = "fast-leaf-decay";
        public static final String REPLANT = "replant.enabled";
        public static final String REPLANT_FREE = "replant.free";
        public static final String DROPS_TO_INVENTORY = "drops-to-inventory";
        public static final String BONUS_DROPS_CHANCE = "bonus-drops.chance";
        public static final String COSTS_HUNGER = "costs.hunger";
        public static final String COSTS_EXPERIENCE = "costs.experience";
        public static final String LANGUAGE = "language";
        public static final String ANIMATION_MODE = "animation.mode";
        public static final String REQUIRE_LEAVES = "require-leaves";
        public static final String MUSHROOMS = "mushrooms";
        public static final String SOURCES_LIBRARIAN = "sources.librarian";
        public static final String SOURCES_WANDERING_TRADER = "sources.wandering-trader";
        public static final String SOURCES_LOOT = "sources.loot";
        public static final String SOURCES_PRICE = "sources.price";
        public static final String SKILL_LUCKY_DROPS = "skill.lucky-drops";
        public static final String PROTECT_AXE = "protect-axe";
        public static final String EXPERIENCE_DROPS_PER_LOG = "experience-drops.per-log";
        public static final String SKILL = "skill.enabled";
        public static final String SKILL_MAX_LEVEL = "skill.max-level";
        public static final String SKILL_XP_PER_LOG = "skill.xp-per-log";
        public static final String SKILL_XP_FIRST_LEVEL = "skill.xp-first-level";
        public static final String SKILL_XP_INCREASE = "skill.xp-increase";
        /**
         * followed by the name of the perk
         */
        public static final String SKILL_PERKS = "skill.perks.";
        /**
         * followed by the name of the feature
         */
        public static final String SKILL_UNLOCKS = "skill.unlocks.";
    }

}
