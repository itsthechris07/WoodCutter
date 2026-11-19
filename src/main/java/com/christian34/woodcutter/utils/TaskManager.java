package com.christian34.woodcutter.utils;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.platform.Platform;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * WoodCutter 2026.
 * <p>
 * Runs tasks with the schedulers of the server: the region schedulers on Paper and Folia (on Paper they run on the
 * main thread, on Folia the region of the entity or location ticks them - the BukkitScheduler throws there), the
 * BukkitScheduler on Spigot.
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

    private static Platform platform() {
        return Platform.get();
    }

    /**
     * runs the task in the next tick on the thread that owns the entity (the main thread on Paper and Spigot) - the
     * task is dropped if the entity has been removed (the player has left)
     */
    public static void run(@NotNull Entity entity, @NotNull Runnable runnable) {
        platform().run(WoodCutter.getInstance(), entity, runnable);
    }

    /**
     * like {@link #run(Entity, Runnable)}, but runs the task at once if the current thread owns the entity already
     */
    public static void runNowOrLater(@NotNull Entity entity, @NotNull Runnable runnable) {
        if (owns(entity)) {
            runnable.run();
        } else {
            run(entity, runnable);
        }
    }

    /**
     * @param retired runs instead of the task if the entity has been removed until then (the player has left)
     */
    public static void runLater(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks) {
        platform().runLater(WoodCutter.getInstance(), entity, runnable, retired, delayTicks);
    }

    /**
     * runs the task again and again on another thread than the server, e.g. to write files
     */
    public static void asyncTimer(@NotNull Runnable runnable, long periodMinutes) {
        platform().asyncTimer(WoodCutter.getInstance(), runnable, periodMinutes);
    }

    /**
     * runs the task on the thread that owns the location (the main thread on Paper and Spigot), e.g. to change blocks
     */
    public static void regionLater(@NotNull Location location, @NotNull Runnable runnable, long delayTicks) {
        platform().runLater(WoodCutter.getInstance(), location, runnable, delayTicks);
    }

    /**
     * runs the task on the thread of the server (the global region on Folia), e.g. commands of the console
     */
    public static void global(@NotNull Runnable runnable) {
        platform().runGlobal(WoodCutter.getInstance(), runnable);
    }

    /**
     * @return true if the current thread may change the entity - on Folia only the thread of its region
     */
    public static boolean owns(@NotNull Entity entity) {
        return platform().owns(entity);
    }

    public static boolean owns(@NotNull Location location) {
        return platform().owns(location);
    }

    public static boolean owns(@NotNull Block block) {
        return platform().owns(block.getLocation());
    }

}
