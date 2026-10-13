package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * The animation 'topple': the logs of a cut tree are shown as block displays that tip over together, away from the
 * player, and disappear when the tree hits the ground. The displays are only an effect - the logs are broken and
 * dropped before, like without the animation.
 *
 * @author Christian34
 */
final class FallingTree {
    /**
     * trees with more logs just disappear, so many entities would lag the players
     */
    private static final int MAX_LOGS = 400;
    /**
     * the angles of the tree in degrees, one after the other: it falls faster and faster. The players see a smooth
     * movement between them.
     */
    private static final float[] ANGLES = {5, 16, 34, 60, 90};
    /**
     * the ticks between two angles
     */
    private static final int STEP_TICKS = 5;
    /**
     * logs that show particles when the tree hits the ground
     */
    private static final int MAX_PARTICLES = 40;
    private final Location pivot;
    /**
     * the horizontal direction the tree falls to
     */
    private final Vector direction;
    private final List<Log> logs = new ArrayList<>();
    private final List<BlockDisplay> displays = new ArrayList<>();

    /**
     * @param stump  the lowest log, the tree turns around its bottom
     * @param player the tree falls away from them
     */
    FallingTree(Block stump, Player player) {
        this.pivot = stump.getLocation().add(0.5, 0, 0.5);
        Vector direction = pivot.toVector().subtract(player.getLocation().toVector()).setY(0);
        // the player stands on the stump
        if (direction.lengthSquared() < 0.01) direction = player.getLocation().getDirection().setY(0);
        this.direction = direction.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : direction.normalize();
    }

    /**
     * @param data what the block looked like before it was broken
     */
    void add(Block block, BlockData data) {
        logs.add(new Log(block.getLocation(), data));
    }

    void start() {
        if (logs.size() < 2 || logs.size() > MAX_LOGS) return;
        World world = pivot.getWorld();
        for (Log log : logs) {
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(log.location())) continue;
            displays.add(world.spawn(log.location(), BlockDisplay.class, display -> {
                display.setBlock(log.data());
                // not saved, so no display is left if the server stops while the tree falls
                display.setPersistent(false);
            }));
        }
        world.playSound(pivot, "minecraft:block.wood.break", 1.5f, 0.6f);
        // the players have to know the displays before they can move
        TaskManager.regionLater(pivot, () -> step(0), 2);
    }

    private void step(int index) {
        float angle = (float) Math.toRadians(ANGLES[index]);
        for (BlockDisplay display : displays) {
            if (!display.isValid() || !Bukkit.isOwnedByCurrentRegion(display)) continue;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(STEP_TICKS);
            display.setTransformation(transformation(display.getLocation(), angle));
        }
        if (index + 1 < ANGLES.length) {
            TaskManager.regionLater(pivot, () -> step(index + 1), STEP_TICKS);
        } else {
            TaskManager.regionLater(pivot, this::land, STEP_TICKS);
        }
    }

    /**
     * @return the rotation around the axis that lies on the ground across the direction, through the pivot
     */
    private Quaternionf rotation(float angle) {
        return new Quaternionf().fromAxisAngleRad((float) direction.getZ(), 0, (float) -direction.getX(), angle);
    }

    /**
     * a display turns around its own corner, so it is also moved to where the tree is at the angle
     */
    private Transformation transformation(Location display, float angle) {
        Quaternionf rotation = rotation(angle);
        Vector3f fromPivot = display.toVector().subtract(pivot.toVector()).toVector3f();
        Vector3f translation = rotation.transform(new Vector3f(fromPivot)).sub(fromPivot);
        return new Transformation(translation, rotation, new Vector3f(1, 1, 1), new Quaternionf());
    }

    /**
     * @return where the center of a log is when the tree lies on the ground
     */
    Location landing(Location log) {
        Vector3f fromPivot = log.toVector().subtract(pivot.toVector()).toVector3f().add(0.5f, 0.5f, 0.5f);
        Vector3f landed = rotation((float) Math.toRadians(90)).transform(fromPivot);
        return pivot.clone().add(landed.x, landed.y, landed.z);
    }

    private void land() {
        World world = pivot.getWorld();
        int step = Math.max(1, logs.size() / MAX_PARTICLES);
        Location middle = null;
        for (int i = 0; i < logs.size(); i += step) {
            Location landing = landing(logs.get(i).location());
            world.spawnParticle(Particle.BLOCK, landing, 12, 0.3, 0.2, 0.3, logs.get(i).data());
            if (i >= logs.size() / 2 && middle == null) middle = landing;
        }
        world.playSound(middle == null ? pivot : middle, "minecraft:block.wood.break", 2, 0.5f);
        for (BlockDisplay display : displays) {
            if (display.isValid() && Bukkit.isOwnedByCurrentRegion(display)) display.remove();
        }
    }

    private record Log(Location location, BlockData data) {
    }

}
