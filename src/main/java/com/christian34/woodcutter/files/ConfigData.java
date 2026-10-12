package com.christian34.woodcutter.files;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.utils.Debug;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * The config.yml - {@link Keys} contains the paths of all settings (relative to 'config.'). Settings of newer
 * versions are added to existing files on load.
 *
 * @author Christian34
 */
public class ConfigData {
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
            migrate();
            update();
        } catch (IOException | RuntimeException ex) {
            // missing settings have the value of the default config then
            Debug.warn("Couldn't update file 'config.yml': " + ex);
        }
        instance.reloadConfig();
        this.data = instance.getConfig();
    }

    /**
     * adds the settings (and comments) of the default config that are missing in the file, e.g. after an update
     */
    public void update() throws IOException {
        ConfigUpdater.update(instance, "config.yml", new File(instance.getDataFolder(), "config.yml"), List.of());
    }

    /**
     * converts settings of older versions: 'probability' (4 = 40 %) became 'chance' in percent
     */
    private void migrate() throws IOException {
        File file = new File(instance.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String probability = DATA_PREFIX + "probability";
        if (!config.contains(probability)) return;
        if (!config.contains(DATA_PREFIX + Keys.CHANCE)) {
            config.set(DATA_PREFIX + Keys.CHANCE, Math.max(1, Math.min(10, config.getInt(probability))) * 10);
        }
        config.set(probability, null);
        // the comments are restored by the update
        config.save(file);
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
    }

}
