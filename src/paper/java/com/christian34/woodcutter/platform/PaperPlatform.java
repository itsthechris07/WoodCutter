package com.christian34.woodcutter.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * WoodCutter 2026.
 * <p>
 * Paper and Folia: texts as components and the region schedulers, so the same jar runs on both - on Paper the tasks
 * run on the thread of the server, on Folia the region of the entity or location ticks them (the BukkitScheduler
 * throws there). Compiled against the api of Paper, loaded only on these servers.
 *
 * @author Christian34
 */
public final class PaperPlatform extends Platform {
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder().character('§')
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static Component component(String text) {
        return SECTION.deserialize(text);
    }

    /**
     * names and lore are in italics otherwise
     */
    private static Component upright(String text) {
        return component(text).decoration(TextDecoration.ITALIC, false);
    }

    private static String plain(Component line) {
        return PlainTextComponentSerializer.plainText().serialize(line);
    }

    @Override
    public @NotNull String name() {
        return "paper";
    }

    @Override
    public void log(@NotNull Plugin plugin, @NotNull String level, @NotNull String text) {
        switch (level) {
            case "debug" -> plugin.getComponentLogger().debug(component(text));
            case "warn" -> plugin.getComponentLogger().warn(component(text));
            default -> plugin.getComponentLogger().info(component(text));
        }
    }

    @Override
    public void sendCommand(@NotNull CommandSender receiver, @NotNull String text, @NotNull String command, boolean run) {
        receiver.sendMessage(component(text)
                .clickEvent(run ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command, NamedTextColor.GRAY))));
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String text) {
        player.sendActionBar(component(text));
    }

    @Override
    public void showTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(component(title), component(subtitle),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
    }

    @Override
    public void setDisplayName(@NotNull ItemMeta meta, @NotNull String name) {
        meta.displayName(upright(name));
    }

    @Override
    public void setLore(@NotNull ItemMeta meta, @NotNull List<String> lore) {
        meta.lore(lore.stream().map(PaperPlatform::upright).toList());
    }

    @Override
    public boolean hasLoreLine(@NotNull ItemMeta meta, @NotNull Predicate<String> line) {
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(text -> line.test(plain(text)));
    }

    @Override
    public void replaceLoreLines(@NotNull ItemMeta meta, @NotNull Predicate<String> remove, @Nullable String add) {
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(text -> remove.test(plain(text)));
        if (add != null) lore.add(Component.text(add, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.isEmpty() ? null : lore);
    }

    @Override
    public @NotNull Inventory createInventory(@NotNull InventoryHolder holder, int size, @NotNull String title) {
        return Bukkit.createInventory(holder, size, upright(title));
    }

    @Override
    public void run(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    @Override
    public void runLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, @NotNull Runnable retired, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), retired, Math.max(1, delayTicks));
    }

    @Override
    public void runLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduled -> task.run(), Math.max(1, delayTicks));
    }

    @Override
    public void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void asyncTimer(@NotNull Plugin plugin, @NotNull Runnable task, long periodMinutes) {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduled -> task.run(), periodMinutes, periodMinutes, TimeUnit.MINUTES);
    }

    @Override
    public boolean owns(@NotNull Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public boolean owns(@NotNull Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

}
