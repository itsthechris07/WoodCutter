package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.platform.Platform;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WoodCutter 2026.
 * <p>
 * Bigger trees take longer to chop (setting 'chopping'): while a player breaks a log that cuts a tree, the attribute
 * block_break_speed of the player is lowered by the size of the tree, and a boss bar or the action bar shows the
 * progress. The attribute is restored when the log breaks or the player stops - and when they join, in case the server
 * stopped in between.
 *
 * @author Christian34
 */
public final class Chopping implements Listener {
    /**
     * ticks the player may look away from the log before the chopping stops
     */
    private static final int AWAY_TICKS = 5;
    private final WoodCutter instance;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public Chopping(WoodCutter instance) {
        this.instance = instance;
    }

    private static NamespacedKey key() {
        return new NamespacedKey(WoodCutter.getInstance(), "chopping");
    }

    /**
     * @return the attribute of the speed players break blocks with - by its name, it was renamed in 1.21.2
     */
    @Nullable
    public static Attribute breakSpeed() {
        for (String name : List.of("block_break_speed", "player.block_break_speed")) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
            if (attribute != null) return attribute;
        }
        return null;
    }

    /**
     * @return how many times as long breaking the log takes for a tree of the size, 1 if it is not slower
     */
    static double factor(int logs, double slowdownPerLog, double maxFactor) {
        if (slowdownPerLog <= 0) return 1;
        return Math.max(1, Math.min(Math.max(1, maxFactor), 1 + logs * slowdownPerLog / 100));
    }

    /**
     * the player starts to break a log that cuts a tree
     */
    public void start(Player player, Block block, int maxBlocks) {
        ConfigData config = instance.getConfigData();
        double slowdown = config.getDouble(ConfigData.Keys.CHOPPING_SLOWDOWN);
        if (slowdown <= 0 || player.getGameMode() == GameMode.CREATIVE) return;
        stop(player);
        // the log the player breaks belongs to the tree as well
        int others = new Cutter(instance, player, block, maxBlocks).count();
        int logs = others == 0 ? 0 : others + 1;
        if (logs == 0) return;
        double factor = factor(logs, slowdown, config.getDouble(ConfigData.Keys.CHOPPING_MAX_FACTOR));
        Attribute attribute = breakSpeed();
        AttributeInstance speed = attribute == null ? null : player.getAttribute(attribute);
        if (factor <= 1 || speed == null) return;
        speed.addModifier(new AttributeModifier(key(), 1 / factor - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));

        String progress = config.getString(ConfigData.Keys.CHOPPING_PROGRESS).toLowerCase(Locale.ROOT);
        String title = Message.CHOPPING.getText("%logs%", Integer.toString(logs));
        BossBar bar = progress.equals("bossbar") ? Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SEGMENTED_10) : null;
        if (bar != null) bar.setProgress(0);
        Session session = new Session(block, title, bar, progress.equals("actionbar"));
        sessions.put(player.getUniqueId(), session);
        if (bar != null) bar.addPlayer(player);
        TaskManager.runLater(player, () -> tick(player, session), () -> sessions.remove(player.getUniqueId(), session), 1);
    }

    private void tick(Player player, Session session) {
        if (sessions.get(player.getUniqueId()) != session) return;
        Block target = player.getTargetBlockExact(6);
        session.away = session.block.equals(target) ? 0 : session.away + 1;
        if (session.away > AWAY_TICKS || !TreeBlocks.isLog(session.block.getType()) && session.block.getType() != org.bukkit.Material.MUSHROOM_STEM) {
            stop(player);
            return;
        }
        if (session.away == 0) {
            try {
                session.progress += session.block.getBreakSpeed(player);
            } catch (UnsupportedOperationException ex) {
                // not every server knows it, the bar stays empty then
            }
        }
        float progress = (float) Math.max(0, Math.min(1, session.progress));
        if (session.bar != null) session.bar.setProgress(progress);
        if (session.actionBar) Platform.get().sendActionBar(player, session.title + " " + bar(progress));
        TaskManager.runLater(player, () -> tick(player, session), () -> sessions.remove(player.getUniqueId(), session), 1);
    }

    /**
     * @return a bar of 20 characters, green up to the progress
     */
    static String bar(float progress) {
        int filled = Math.round(progress * 20);
        return "§a" + "|".repeat(filled) + "§8" + "|".repeat(20 - filled);
    }

    /**
     * restores the speed of the player and hides the bar
     */
    public void stop(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null && session.bar != null) session.bar.removeAll();
        removeModifier(player);
    }

    public void stopAll() {
        for (UUID id : List.copyOf(sessions.keySet())) {
            Player player = instance.getServer().getPlayer(id);
            if (player != null) stop(player);
        }
        sessions.clear();
    }

    private static void removeModifier(Player player) {
        Attribute attribute = breakSpeed();
        AttributeInstance speed = attribute == null ? null : player.getAttribute(attribute);
        if (speed == null) return;
        for (AttributeModifier modifier : List.copyOf(speed.getModifiers())) {
            if (modifier.getKey().equals(key())) speed.removeModifier(modifier);
        }
    }

    /**
     * @return true while the player breaks a log slower
     */
    public boolean isChopping(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onAbort(BlockDamageAbortEvent e) {
        stop(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        Session session = sessions.get(e.getPlayer().getUniqueId());
        if (session != null && session.block.equals(e.getBlock())) stop(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stop(e.getPlayer());
    }

    /**
     * the server may have stopped while the player chopped
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        removeModifier(e.getPlayer());
    }

    private static final class Session {
        final Block block;
        final String title;
        @Nullable
        final BossBar bar;
        final boolean actionBar;
        double progress;
        int away;

        Session(Block block, String title, @Nullable BossBar bar, boolean actionBar) {
            this.block = block;
            this.title = title;
            this.bar = bar;
            this.actionBar = actionBar;
        }
    }

}
