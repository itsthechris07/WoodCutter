package com.christian34.woodcutter.files;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.utils.Debug;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * WoodCutter 2026.
 * <p>
 * The messages.yml (or the file of another language) with all texts players see. Texts of newer versions are added to existing files on load.
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
        String name = fileName(instance.getConfigData().getString(ConfigData.Keys.LANGUAGE));
        File file = new File(instance.getDataFolder(), name);
        if (!file.exists()) instance.saveResource(name, false);
        try {
            YamlUpdater.update(instance, name, file, false);
        } catch (IOException | RuntimeException ex) {
            // missing texts have the default text then
            Debug.warn("Couldn't update file '" + name + "': " + ex);
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        try (InputStream defaults = instance.getResource(name)) {
            if (defaults != null) {
                data.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            Debug.warn("Couldn't load the default messages: " + ex);
        }
        this.data = data;
    }

    /**
     * @return the file of the language: messages.yml for English, messages_&lt;language&gt;.yml for the other ones
     * the plugin comes with
     */
    private String fileName(String language) {
        language = language.trim().toLowerCase(Locale.ROOT);
        if (language.isEmpty() || language.equals("en")) return FILE;
        String name = "messages_" + language + ".yml";
        if (language.matches("[a-z]{2,8}") && instance.getResource(name) != null) return name;
        Debug.warn("The language '" + language + "' is not available, the texts are English.");
        return FILE;
    }

    @NotNull
    public String getText(@NotNull String key) {
        // without a default value, so texts that are missing in the file have the default text
        String text = data.getString(DATA_PREFIX + key);
        return text == null ? "" : text;
    }

}
