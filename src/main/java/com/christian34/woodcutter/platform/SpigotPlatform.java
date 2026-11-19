package com.christian34.woodcutter.platform;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * WoodCutter 2026.
 * <p>
 * Spigot (and servers without the api of Paper): texts with the colors of the server, the chat api of BungeeCord for
 * the action bar and clickable lines, and the BukkitScheduler - everything runs on the thread of the server.
 *
 * @author Christian34
 */
final class SpigotPlatform extends Platform {
    /**
     * resets the style of the game for names and lore, which are in italics otherwise
     */
    private static final String RESET = ChatColor.RESET.toString();

    @Override
    public @NotNull String name() {
        return "spigot";
    }

    @Override
    public void log(@NotNull Plugin plugin, @NotNull String level, @NotNull String text) {
        Level logLevel = switch (level) {
            case "debug" -> Level.FINE;
            case "warn" -> Level.WARNING;
            default -> Level.INFO;
        };
        plugin.getLogger().log(logLevel, ChatColor.stripColor(text));
    }

    @Override
    public void sendCommand(@NotNull CommandSender receiver, @NotNull String text, @NotNull String command, boolean run) {
        if (!(receiver instanceof Player player)) {
            receiver.sendMessage(text);
            return;
        }
        BaseComponent line = TextComponent.fromLegacy(text);
        line.setClickEvent(new ClickEvent(run ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command));
        line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + command)));
        player.spigot().sendMessage(line);
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String text) {
        BaseComponent components = TextComponent.fromLegacy(text);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
    }

    @Override
    public void showTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void setDisplayName(@NotNull ItemMeta meta, @NotNull String name) {
        meta.setDisplayName(RESET + name);
    }

    @Override
    public void setLore(@NotNull ItemMeta meta, @NotNull List<String> lore) {
        meta.setLore(lore.stream().map(line -> RESET + line).toList());
    }

    @Override
    public boolean hasLoreLine(@NotNull ItemMeta meta, @NotNull Predicate<String> line) {
        List<String> lore = meta.getLore();
        return lore != null && lore.stream().anyMatch(text -> line.test(ChatColor.stripColor(text)));
    }

    @Override
    public void replaceLoreLines(@NotNull ItemMeta meta, @NotNull Predicate<String> remove, @Nullable String add) {
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.removeIf(text -> remove.test(ChatColor.stripColor(text)));
        if (add != null) lore.add(ChatColor.GRAY + add);
        meta.setLore(lore.isEmpty() ? null : lore);
    }

    @Override
    public @NotNull Inventory createInventory(@NotNull InventoryHolder holder, int size, @NotNull String title) {
        return Bukkit.createInventory(holder, size, title);
    }

    @Override
    public void run(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (isValid(entity)) task.run();
        });
    }

    @Override
    public void runLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, @NotNull Runnable retired, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isValid(entity)) task.run();
            else retired.run();
        }, Math.max(1, delayTicks));
    }

    /**
     * @return false if the entity has been removed or the player has left
     */
    private static boolean isValid(Entity entity) {
        return entity instanceof Player player ? player.isOnline() : entity.isValid();
    }

    @Override
    public void runLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks));
    }

    @Override
    public void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void asyncTimer(@NotNull Plugin plugin, @NotNull Runnable task, long periodMinutes) {
        long ticks = periodMinutes * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, ticks, ticks);
    }

    @Override
    public boolean owns(@NotNull Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean owns(@NotNull Location location) {
        return Bukkit.isPrimaryThread();
    }

}
