package com.christian34.woodcutter.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * WoodCutter 2026.
 * <p>
 * Logs messages of WoodCutter to the console. Messages may contain legacy colors (§c), which are shown as colors in
 * the console.
 *
 * @author Christian34
 */
public final class Debug {
    private static ComponentLogger logger;

    private Debug() {
    }

    /**
     * called once when the plugin is enabled
     */
    public static void init(@NotNull Plugin plugin) {
        logger = plugin.getComponentLogger();
    }

    /**
     * logs an action on the debug level, which is hidden unless it is enabled for WoodCutter in the logging config
     */
    public static void recordAction(String message) {
        if (logger != null) logger.debug(format(message));
    }

    public static void log(String message) {
        if (logger == null) fallback(message);
        else logger.info(format(message));
    }

    public static void warn(String message) {
        if (logger == null) fallback(message);
        else logger.warn(format(message));
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
