package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WoodCutter 2026.
 * <p>
 * The animation 'topple': the logs and leaves of a cut tree are shown as block displays that tip over together, away
 * from the player, and disappear when the tree hits the ground. The displays are only an effect - the logs are broken
 * and the leaves decay before, like without the animation. Their items can drop where the tree lands, and the tree
 * can push away and hurt the mobs and players it hits.
 *
 * @author Christian34
 */
final class FallingTree {
    /**
     * trees with more logs just disappear, so many entities would lag the players
     */
    private static final int MAX_LOGS = 400;
    /**
     * the most blocks shown with their leaves - a bigger tree falls without its leaves
     */
    private static final int MAX_BLOCKS = 1000;
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
    /**
     * how far from a landed log mobs and players are hit
     */
    private static final double HIT_RADIUS = 1.2;
    /**
     * blocks an item is moved up at most if the tree lands in a hill
     */
    private static final int MAX_LIFT = 6;
    /**
     * the trees that are falling right now, so their items are dropped if the plugin stops before they land
     */
    private static final Set<FallingTree> FALLING = ConcurrentHashMap.newKeySet();
    private final Location pivot;
    private final Player player;
    /**
     * the horizontal direction the tree falls to
     */
    private final Vector direction;
    private final List<Log> logs = new ArrayList<>();
    private final List<Log> leaves = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<BlockDisplay> displays = new ArrayList<>();
    private boolean knockback;
    private double damage;
    private boolean landed;

    /**
     * @param stump  the lowest log, the tree turns around its bottom
     * @param player the tree falls away from them
     */
    FallingTree(Block stump, Player player) {
        this.pivot = stump.getLocation().add(0.5, 0, 0.5);
        this.player = player;
        Vector direction = pivot.toVector().subtract(player.getLocation().toVector()).setY(0);
        // the player stands on the stump
        if (direction.lengthSquared() < 0.01) direction = player.getLocation().getDirection().setY(0);
        this.direction = direction.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : direction.normalize();
    }

    /**
     * @param knockback true pushes the mobs and players the tree hits away
     * @param damage    what the tree does to them, as the player who cut it (0 for nothing)
     */
    void hits(boolean knockback, double damage) {
        this.knockback = knockback;
        this.damage = Math.max(0, damage);
    }

    /**
     * @param data what the block looked like before it was broken
     */
    void add(Block block, BlockData data) {
        logs.add(new Log(block.getLocation(), data));
    }

    /**
     * @param data what the leaf looked like before it decayed
     */
    void addLeaf(Block block, BlockData data) {
        leaves.add(new Log(block.getLocation(), data));
    }

    /**
     * the item drops where the block lands - or where it was, if the tree does not fall
     */
    void drop(Block from, ItemStack item) {
        if (landed) {
            dropAt(from.getLocation().add(0.5, 0.5, 0.5), item);
        } else {
            drops.add(new Drop(from.getLocation(), item));
        }
    }

    /**
     * @return true if the tree is shown falling, false if it just disappears
     */
    boolean start() {
        if (logs.size() < 2 || logs.size() > MAX_LOGS) {
            dropWhereTheyWere();
            return false;
        }
        World world = pivot.getWorld();
        List<Log> blocks = new ArrayList<>(logs);
        if (logs.size() + leaves.size() <= MAX_BLOCKS) blocks.addAll(leaves);
        for (Log log : blocks) {
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(log.location())) continue;
            displays.add(world.spawn(log.location(), BlockDisplay.class, display -> {
                display.setBlock(log.data());
                // not saved, so no display is left if the server stops while the tree falls
                display.setPersistent(false);
            }));
        }
        FALLING.add(this);
        world.playSound(pivot, "minecraft:block.wood.break", 1.5f, 0.6f);
        // the players have to know the displays before they can move
        TaskManager.regionLater(pivot, () -> step(0), 2);
        return true;
    }

    /**
     * drops the items of all falling trees where their blocks were, e.g. when the plugin is disabled
     */
    static void landAll() {
        for (FallingTree tree : FALLING) {
            tree.dropWhereTheyWere();
            for (BlockDisplay display : tree.displays) {
                if (display.isValid() && Bukkit.isOwnedByCurrentRegion(display)) display.remove();
            }
        }
        FALLING.clear();
    }

    private void dropWhereTheyWere() {
        this.landed = true;
        for (Drop drop : drops) dropAt(drop.location().add(0.5, 0.5, 0.5), drop.item());
        drops.clear();
    }

    private void step(int index) {
        if (landed) return;
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
        if (landed) return;
        this.landed = true;
        FALLING.remove(this);
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
        for (Drop drop : drops) dropAt(landing(drop.location()), drop.item());
        drops.clear();
        if (knockback || damage > 0) hit();
    }

    /**
     * pushes away and hurts the mobs and players the logs land on - not the player who cut the tree
     */
    private void hit() {
        Set<LivingEntity> hit = new HashSet<>();
        for (Log log : logs) {
            Location landing = landing(log.location());
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(landing)) continue;
            for (Entity entity : landing.getWorld().getNearbyEntities(landing, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (entity instanceof LivingEntity living && !entity.equals(player) && !(entity instanceof ArmorStand)) {
                    hit.add(living);
                }
            }
        }
        for (LivingEntity entity : hit) {
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(entity)) continue;
            if (damage > 0) {
                // as the player, so protection and PvP plugins can deny it
                entity.damage(damage, player);
                if (entity.isDead()) continue;
            }
            if (knockback) entity.setVelocity(entity.getVelocity().add(direction.clone().multiply(0.7).setY(0.35)));
        }
    }

    /**
     * drops the item at the location, above the ground if the tree has landed in a hill
     */
    private static void dropAt(Location location, ItemStack item) {
        World world = location.getWorld();
        // on Folia the tree can land in another region
        if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(location)) {
            TaskManager.regionLater(location, () -> dropAt(location, item), 1);
            return;
        }
        Location at = location.clone();
        for (int i = 0; i < MAX_LIFT && at.getBlock().getType().isSolid() && at.getY() < world.getMaxHeight() - 1; i++) {
            at.add(0, 1, 0);
        }
        world.dropItemNaturally(at, item);
    }

    private record Log(Location location, BlockData data) {
    }

    private record Drop(Location location, ItemStack item) {
    }

}
