package com.christian34.woodcutter.files;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * Adds the settings of newer versions to the yml files in the plugin folder: the file is written again from the
 * default file of the jar (its order and comments) with the values of the server. Files without missing settings are
 * not touched, so own comments stay until the next update with new settings.
 *
 * @author Christian34
 */
final class YamlUpdater {
    /**
     * long texts stay in one line
     */
    private static final int LINE_WIDTH = 1000;

    private YamlUpdater() {
    }

    /**
     * @param resource the default file in the jar
     * @param file     the file in the plugin folder
     * @param force    writes the file even if no setting is missing, e.g. after a migration
     * @throws IOException if a file can't be read or written, or the file of the server is no valid yml - it is not
     *                     overwritten then
     */
    static void update(Plugin plugin, String resource, File file, boolean force) throws IOException {
        YamlConfiguration updated = new YamlConfiguration();
        YamlConfiguration current = new YamlConfiguration();
        String header;
        try (InputStream defaults = plugin.getResource(resource)) {
            if (defaults == null) throw new FileNotFoundException(resource);
            String text = new String(defaults.readAllBytes(), StandardCharsets.UTF_8);
            header = header(text);
            updated.loadFromString(text);
            current.load(file);
        } catch (InvalidConfigurationException ex) {
            throw new IOException(ex);
        }

        boolean write = force;
        for (String key : updated.getKeys(true)) {
            if (updated.isConfigurationSection(key)) continue;
            if (current.isSet(key) && !current.isConfigurationSection(key)) {
                updated.set(key, current.get(key));
            } else {
                write = true;
            }
        }
        if (!write) return;
        updated.options().width(LINE_WIDTH);
        // the header is written as it is, Bukkit would change the lines of its box
        updated.options().setHeader(List.of());
        Files.writeString(file.toPath(), header + updated.saveToString().stripLeading());
    }

    /**
     * @return the comments and empty lines at the top of the file
     */
    private static String header(String text) {
        StringBuilder header = new StringBuilder();
        for (String line : text.lines().toList()) {
            if (!line.isBlank() && !line.startsWith("#")) break;
            header.append(line).append('\n');
        }
        return header.toString();
    }

}
