package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.platform.Platform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * WoodCutter 2026.
 * <p>
 * Logs messages of WoodCutter to the console. Messages may contain legacy colors (§c), which are shown as colors in
 * the console of Paper.
 *
 * @author Christian34
 */
public final class Debug {
    private static Plugin plugin;

    private Debug() {
    }

    /**
     * called once when the plugin is enabled
     */
    public static void init(@NotNull Plugin plugin) {
        Debug.plugin = plugin;
    }

    /**
     * logs an action on the debug level, which is hidden unless it is enabled for WoodCutter in the logging config
     */
    public static void recordAction(String message) {
        if (plugin != null) Platform.get().log(plugin, "debug", message);
    }

    public static void log(String message) {
        if (plugin == null) fallback(message);
        else Platform.get().log(plugin, "info", message);
    }

    public static void warn(String message) {
        if (plugin == null) fallback(message);
        else Platform.get().log(plugin, "warn", message);
    }

    static Component format(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    /**
     * before the plugin is enabled (e.g. in tests without a server)
     */
    private static void fallback(String message) {
        System.out.println("[WoodCutter] " + PlainTextComponentSerializer.plainText().serialize(format(message)));
    }

}
