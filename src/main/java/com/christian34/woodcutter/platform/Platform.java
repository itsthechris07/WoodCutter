package com.christian34.woodcutter.platform;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * WoodCutter 2026.
 * <p>
 * What differs between the servers: Paper and Folia show texts as components and run tasks with their region
 * schedulers, Spigot has neither. The rest of the plugin only uses the api of Spigot (it is compiled against it) and
 * texts with the colors of the server (§c, hex colors as §x§r§r§g§g§b§b).
 *
 * @author Christian34
 */
public abstract class Platform {
    /**
     * the system property that chooses the platform (paper or spigot), e.g. to test the one of Spigot on Paper
     */
    public static final String PROPERTY = "woodcutter.platform";
    private static Platform platform;

    /**
     * @return the platform of the server
     */
    @NotNull
    public static synchronized Platform get() {
        if (platform == null) platform = create();
        return platform;
    }

    private static Platform create() {
        String forced = System.getProperty(PROPERTY, "");
        if (!forced.equalsIgnoreCase("spigot") && (forced.equalsIgnoreCase("paper") || isPaper())) {
            try {
                return (Platform) Class.forName("com.christian34.woodcutter.platform.PaperPlatform").getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError ex) {
                // a fork of Paper without its api, the one of Spigot works there
            }
        }
        return new SpigotPlatform();
    }

    /**
     * @return true if the server has the schedulers of Paper and shows components (Paper 1.20 and newer, Folia)
     */
    static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            return Class.forName("net.kyori.adventure.audience.Audience").isAssignableFrom(Player.class);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * @return paper or spigot
     */
    @NotNull
    public abstract String name();

    // texts

    /**
     * logs the text with its colors (§c) - level: debug, info or warn
     */
    public abstract void log(@NotNull Plugin plugin, @NotNull String level, @NotNull String text);

    /**
     * sends the text as a line players can click
     *
     * @param run true runs the command with the click, false writes it into the chat of the player
     */
    public abstract void sendCommand(@NotNull CommandSender receiver, @NotNull String text, @NotNull String command, boolean run);

    public abstract void sendActionBar(@NotNull Player player, @NotNull String text);

    /**
     * @param fadeIn ticks the title fades in, stay and fadeOut likewise
     */
    public abstract void showTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * the name of an item of a menu, not in italics
     */
    public abstract void setDisplayName(@NotNull ItemMeta meta, @NotNull String name);

    /**
     * the lore of an item of a menu, not in italics
     */
    public abstract void setLore(@NotNull ItemMeta meta, @NotNull List<String> lore);

    /**
     * @param line the text of a line of the lore without colors
     * @return true if a line of the lore matches
     */
    public abstract boolean hasLoreLine(@NotNull ItemMeta meta, @NotNull Predicate<String> line);

    /**
     * removes the lines of the lore that match and adds the line - other lines (of other plugins) stay as they are
     *
     * @param remove the text of a line without colors
     * @param add    a gray line, not in italics - null adds none
     */
    public abstract void replaceLoreLines(@NotNull ItemMeta meta, @NotNull Predicate<String> remove, @Nullable String add);

    @NotNull
    public abstract Inventory createInventory(@NotNull InventoryHolder holder, int size, @NotNull String title);

    // tasks

    /**
     * runs the task in the next tick on the thread that owns the entity - it is dropped if the entity has been
     * removed (the player has left)
     */
    public abstract void run(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task);

    /**
     * @param retired runs instead of the task if the entity has been removed until then (the player has left)
     */
    public abstract void runLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, @NotNull Runnable retired, long delayTicks);

    /**
     * runs the task on the thread that owns the location, e.g. to change blocks
     */
    public abstract void runLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks);

    /**
     * runs the task on the thread of the server (the global region on Folia), e.g. commands of the console
     */
    public abstract void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task);

    /**
     * runs the task again and again on another thread than the server, e.g. to write files
     */
    public abstract void asyncTimer(@NotNull Plugin plugin, @NotNull Runnable task, long periodMinutes);

    /**
     * @return true if the current thread may change the entity (always on the thread of the server, except on Folia)
     */
    public abstract boolean owns(@NotNull Entity entity);

    /**
     * @return true if the current thread may change the blocks at the location
     */
    public abstract boolean owns(@NotNull Location location);

}
