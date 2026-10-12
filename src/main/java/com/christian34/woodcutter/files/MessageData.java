package com.christian34.woodcutter.files;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.utils.Debug;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * The messages.yml with all texts players see. Texts of newer versions are added to existing files on load.
 *
 * @author Christian34
 */
public class MessageData {
    private static final String FILE = "messages.yml";
    private static final String DATA_PREFIX = "messages.";
    private final WoodCutter instance;
    private volatile YamlConfiguration data;

    public MessageData(WoodCutter instance) {
        this.instance = instance;
        load();
    }

    /**
     * creates the messages.yml if it does not exist, adds missing texts and loads it (again)
     */
    public void load() {
        File file = new File(instance.getDataFolder(), FILE);
        if (!file.exists()) instance.saveResource(FILE, false);
        try {
            ConfigUpdater.update(instance, FILE, file, List.of());
        } catch (IOException | RuntimeException ex) {
            // missing texts have the default text then
            Debug.warn("Couldn't update file '" + FILE + "': " + ex);
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        try (InputStream defaults = instance.getResource(FILE)) {
            if (defaults != null) {
                data.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            Debug.warn("Couldn't load the default messages: " + ex);
        }
        this.data = data;
    }

    @NotNull
    public String getText(@NotNull String key) {
        return data.getString(DATA_PREFIX + key, "");
    }

}
