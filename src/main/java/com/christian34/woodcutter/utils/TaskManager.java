package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * WoodCutter 2026.
 * <p>
 * Runs tasks with the region schedulers of Paper, so the plugin runs on Paper and Folia with the same code: on Paper
 * they run on the main thread, on Folia the region of the entity or location ticks them. The BukkitScheduler throws
 * on Folia and must not be used.
 *
 * @author Christian34
 */
public class TaskManager {
    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    /**
     * @return true if the server is Folia
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * runs the task in the next tick on the thread that owns the entity (the main thread on Paper) - the task is
     * dropped if the entity has been removed (the player has left)
     */
    public static void run(@NotNull Entity entity, @NotNull Runnable runnable) {
        entity.getScheduler().run(WoodCutter.getInstance(), task -> runnable.run(), null);
    }

    /**
     * like {@link #run(Entity, Runnable)}, but runs the task at once if the current thread owns the entity already
     */
    public static void runNowOrLater(@NotNull Entity entity, @NotNull Runnable runnable) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            runnable.run();
        } else {
            run(entity, runnable);
        }
    }

    /**
     * @param retired runs instead of the task if the entity has been removed until then (the player has left)
     */
    public static void runLater(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks) {
        entity.getScheduler().runDelayed(WoodCutter.getInstance(), task -> runnable.run(), retired, delayTicks);
    }

    /**
     * runs the task again and again on another thread than the server, e.g. to write files
     */
    public static void asyncTimer(@NotNull Runnable runnable, long periodMinutes) {
        Bukkit.getAsyncScheduler().runAtFixedRate(WoodCutter.getInstance(), task -> runnable.run(), periodMinutes, periodMinutes, TimeUnit.MINUTES);
    }

    /**
     * runs the task on the thread that owns the location (the main thread on Paper), e.g. to change blocks
     */
    public static void regionLater(@NotNull Location location, @NotNull Runnable runnable, long delayTicks) {
        Bukkit.getRegionScheduler().runDelayed(WoodCutter.getInstance(), location, task -> runnable.run(), delayTicks);
    }

}
