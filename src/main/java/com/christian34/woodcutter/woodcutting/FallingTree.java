package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WoodCutter 2026.
 * <p>
 * The animation 'topple': the logs and leaves of a cut tree are shown as block displays that tip over together. The
 * displays are only an effect - the logs are broken and the leaves decay before, like without the animation. Their
 * items can drop where the tree lands, and the tree can push away and hurt the mobs and players it hits.
 * <p>
 * With the physics 'simple' the tree tips over at an even pace around the middle of its stump. With 'rigid' it turns
 * around the front edge of its trunk and falls faster and faster, like a real tree: it stops at the first block it
 * hits (hills, walls, other trees), bounces back, slides on and floats on water. 'fall-apart' breaks it into its logs
 * when it hits the ground, which tumble apart.
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
     * the physics 'simple': the angles of the tree in degrees, one after the other. The players see a smooth
     * movement between them.
     */
    private static final float[] ANGLES = {5, 16, 34, 60, 90};
    /**
     * the physics 'simple': the ticks between two angles
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
     * blocks per tick², like the gravity of Minecraft
     */
    private static final double GRAVITY = 0.08;
    /**
     * the fastest the tree turns, in radians per tick
     */
    private static final double MAX_SPEED = 0.35;
    /**
     * a tree that falls down a slope or a cliff turns at most this far
     */
    private static final double MAX_ANGLE = Math.toRadians(150);
    /**
     * the logs whose positions are checked for blocks the tree hits
     */
    private static final int MAX_PROBES = 160;
    /**
     * the longest a tree is shown, in ticks
     */
    private static final int MAX_TICKS = 200;
    /**
     * ticks a tree lies still on the ground before it disappears
     */
    private static final int REST_TICKS = 10;
    /**
     * ticks a tree floats on water before it disappears
     */
    private static final int FLOAT_TICKS = 60;
    /**
     * ticks the logs of a tree that falls apart tumble before they disappear
     */
    private static final int APART_TICKS = 40;
    /**
     * the bottom of a log is this far below its center (a bit less than half a block, so a log on the ground does not
     * touch it)
     */
    private static final float BOTTOM = 0.45f;
    /**
     * the trees that are falling right now, so their items are dropped if the plugin stops before they land
     */
    private static final Set<FallingTree> FALLING = ConcurrentHashMap.newKeySet();
    private final Settings settings;
    /**
     * the bottom of the middle of the stump: the physics 'simple' turns the tree around it
     */
    private final Location pivot;
    private final Player player;
    /**
     * the horizontal direction the tree falls to
     */
    private final Vector direction;
    /**
     * the axis the tree turns around: on the ground, across the direction
     */
    private final Vector3f axis;
    private final List<Log> logs = new ArrayList<>();
    private final List<Log> leaves = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Piece> pieces = new ArrayList<>();
    private final Map<Location, Piece> piecesByBlock = new HashMap<>();
    /**
     * the centers of the logs that are checked for blocks the tree hits, relative to the hinge
     */
    private final List<Vector3f> probes = new ArrayList<>();
    /**
     * the point the tree turns around: the pivot for the physics 'simple', else the front edge of the trunk
     */
    private Location hinge;
    private boolean landed;
    private Phase phase = Phase.TIPPING;
    /**
     * the angle of the tree in radians and how fast it turns, per tick
     */
    private double angle;
    private double speed;
    /**
     * how far the tree has moved since it started to fall (sliding, floating) and how fast it moves
     */
    private final Vector3f shift = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    /**
     * the height of the tree, which makes it fall slower or faster
     */
    private double length = 3;
    private int ticks;
    private int bounces;
    private int resting;
    private int floating;
    private int apartTicks;
    private boolean impacted;
    private boolean splashed;

    /**
     * a tree that falls with the physics 'simple' away from the player
     *
     * @param stump  the lowest log, the tree turns around its bottom
     * @param player the tree falls away from them
     */
    FallingTree(Block stump, Player player) {
        this(stump, player, Settings.SIMPLE, 0);
    }

    /**
     * @param height the height of the tree, for the direction 'slope'
     */
    FallingTree(Block stump, Player player, Settings settings, int height) {
        this.settings = settings;
        this.pivot = new Location(stump.getWorld(), stump.getX() + 0.5, stump.getY(), stump.getZ() + 0.5);
        this.hinge = pivot;
        this.player = player;
        this.direction = direction(settings.direction(), stump, player, height);
        this.axis = new Vector3f((float) direction.getZ(), 0, (float) -direction.getX());
    }

    /**
     * @return the horizontal direction the tree falls to, normalized
     */
    static Vector direction(Direction mode, Block stump, Player player, int height) {
        Location center = new Location(stump.getWorld(), stump.getX() + 0.5, stump.getY(), stump.getZ() + 0.5);
        Vector away = center.toVector().subtract(player.getLocation().toVector()).setY(0);
        // the player stands on the stump
        if (away.lengthSquared() < 0.01) away = player.getLocation().getDirection().setY(0);
        away = away.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : away.normalize();
        return switch (mode) {
            case AWAY -> away;
            case FACING -> {
                Vector facing = player.getLocation().getDirection().setY(0);
                yield facing.lengthSquared() < 0.01 ? away : facing.normalize();
            }
            case RANDOM -> {
                double random = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                yield new Vector(Math.cos(random), 0, Math.sin(random));
            }
            case SLOPE -> slope(stump, away, height);
        };
    }

    /**
     * @return the direction of the eight around the stump with the lowest ground (down the hill) - where it is even,
     * the one away from the player
     */
    private static Vector slope(Block stump, Vector away, int height) {
        int reach = Math.max(4, Math.min(12, height));
        Vector best = away;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double radians = Math.toRadians(i * 45);
            Vector candidate = new Vector(Math.cos(radians), 0, Math.sin(radians));
            double score = 0;
            int samples = 0;
            for (int distance = 2; distance <= reach; distance += 2) {
                double x = stump.getX() + 0.5 + candidate.getX() * distance;
                double z = stump.getZ() + 0.5 + candidate.getZ() * distance;
                score += ground(stump.getWorld(), x, z, stump.getY() + 3, stump.getY() - 12);
                samples++;
            }
            // what is even falls away from the player
            score = score / samples - 0.1 * candidate.dot(away);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * @return the height of the highest block a tree would lie on (not logs or leaves of trees) from the top down to
     * the bottom
     */
    private static int ground(World world, double x, double z, int top, int bottom) {
        for (int y = top; y > bottom; y--) {
            Location at = new Location(world, x, y, z);
            if (TaskManager.isFolia() && !TaskManager.owns(at)) return top;
            if (y < world.getMinHeight()) return y;
            if (y >= world.getMaxHeight()) continue;
            Material type = at.getBlock().getType();
            if (type.isSolid() && !Tag.LEAVES.isTagged(type) && !Tag.LOGS.isTagged(type)) return y;
        }
        return bottom;
    }

    /**
     * @param data what the block looked like before it was broken
     */
    void add(Block block, BlockData data) {
        logs.add(new Log(block.getLocation().clone(), data));
    }

    /**
     * @param data what the leaf (or a block on the tree) looked like before it was removed
     */
    void addLeaf(Block block, BlockData data) {
        leaves.add(new Log(block.getLocation().clone(), data));
    }

    /**
     * the item drops where the block lands - or where it was, if the tree does not fall
     */
    void drop(Block from, ItemStack item) {
        if (landed) {
            dropAt(from.getLocation().clone().add(0.5, 0.5, 0.5), item);
        } else {
            drops.add(new Drop(from.getLocation().clone(), item));
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
        if (settings.physics() != Physics.SIMPLE) this.hinge = hinge();
        List<Log> blocks = new ArrayList<>(logs);
        if (logs.size() + leaves.size() <= MAX_BLOCKS) blocks.addAll(leaves);
        for (int i = 0; i < blocks.size(); i++) {
            Log log = blocks.get(i);
            if (TaskManager.isFolia() && !TaskManager.owns(log.location())) continue;
            BlockDisplay display = world.spawn(log.location(), BlockDisplay.class, spawned -> {
                spawned.setBlock(log.data());
                // not saved, so no display is left if the server stops while the tree falls
                spawned.setPersistent(false);
                if (settings.glow() != null) {
                    spawned.setGlowing(true);
                    spawned.setGlowColorOverride(settings.glow());
                }
            });
            Piece piece = new Piece(display, log, offset(log.location()), i < logs.size());
            pieces.add(piece);
            piecesByBlock.put(log.location(), piece);
        }
        int step = Math.max(1, logs.size() / MAX_PROBES);
        for (int i = 0; i < logs.size(); i += step) {
            Vector3f center = offset(logs.get(i).location()).add(0.5f, 0.5f, 0.5f);
            probes.add(center);
            length = Math.max(length, center.y);
        }
        FALLING.add(this);
        Effects.play(pivot, Effects.CREAK, 0);
        // the players have to know the displays before they can move
        if (settings.physics() == Physics.SIMPLE) {
            TaskManager.regionLater(pivot, () -> step(0), 2);
        } else {
            TaskManager.regionLater(pivot, this::tick, 2);
        }
        return true;
    }

    /**
     * @return the front edge of the trunk at the ground, where a real tree turns around
     */
    private Location hinge() {
        double front = 0;
        for (Log log : logs) {
            Location location = log.location();
            if (location.getBlockY() != pivot.getBlockY()) continue;
            Vector fromStump = location.clone().add(0.5, 0, 0.5).toVector().subtract(pivot.toVector());
            // a log of the same trunk (2x2 or a cross of the Nether), not a root or a branch further away
            if (fromStump.lengthSquared() > 2.5) continue;
            front = Math.max(front, fromStump.dot(direction));
        }
        return pivot.clone().add(direction.clone().multiply(front + 0.5));
    }

    /**
     * @return the corner of the block relative to the hinge
     */
    private Vector3f offset(Location block) {
        return block.toVector().subtract(hinge.toVector()).toVector3f();
    }

    /**
     * drops the items of all falling trees where their blocks were, e.g. when the plugin is disabled
     */
    static void landAll() {
        for (FallingTree tree : FALLING) {
            tree.landed = true;
            tree.dropWhereTheyWere();
            tree.removeDisplays();
        }
        FALLING.clear();
    }

    private void dropWhereTheyWere() {
        this.landed = true;
        for (Drop drop : drops) dropAt(drop.location().clone().add(0.5, 0.5, 0.5), drop.item());
        drops.clear();
    }

    private void removeDisplays() {
        for (Piece piece : pieces) {
            BlockDisplay display = piece.display;
            if (display.isValid() && TaskManager.owns(display)) display.remove();
        }
    }

    // ---- the physics 'simple' ----

    private void step(int index) {
        if (landed) return;
        float radians = (float) Math.toRadians(ANGLES[index]);
        this.angle = radians;
        send(STEP_TICKS);
        if (index + 1 < ANGLES.length) {
            TaskManager.regionLater(pivot, () -> step(index + 1), STEP_TICKS);
        } else {
            TaskManager.regionLater(pivot, this::land, STEP_TICKS);
        }
    }

    /**
     * @return the rotation around the axis that lies on the ground across the direction, through the hinge
     */
    private Quaternionf rotation(double angle) {
        return new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, (float) angle);
    }

    /**
     * the physics 'simple'
     *
     * @return where the center of a log is when the tree lies on the ground
     */
    Location landing(Location log) {
        Vector3f fromPivot = log.toVector().subtract(pivot.toVector()).toVector3f().add(0.5f, 0.5f, 0.5f);
        Vector3f landed = rotation(Math.toRadians(90)).transform(fromPivot);
        return pivot.clone().add(landed.x, landed.y, landed.z);
    }

    // ---- the physics 'rigid' and 'fall-apart' ----

    private void tick() {
        if (landed) return;
        ticks++;
        switch (phase) {
            case TIPPING -> tip();
            case SLIDING -> slide();
            case APART -> tumble();
        }
        if (landed) return;
        if (ticks % settings.updateInterval() == 0) send(settings.updateInterval());
        if (ticks >= MAX_TICKS) {
            land();
            return;
        }
        TaskManager.regionLater(hinge, this::tick, 1);
    }

    /**
     * the tree turns around its hinge, faster and faster, until it hits a block
     */
    private void tip() {
        double acceleration = 1.5 * GRAVITY / length * Math.sin(Math.max(angle, 0.03));
        speed = Math.min(MAX_SPEED, speed + acceleration);
        double next = Math.min(MAX_ANGLE, angle + speed);
        int wet = wet(next, shift);
        if (wet > 0) {
            splash();
            speed *= 0.8;
        }
        boolean blocked = collides(next, shift);
        boolean afloat = wet * 3 >= probes.size();
        if (!blocked && !afloat && next < MAX_ANGLE) {
            this.angle = next;
            fallingLeaves();
            return;
        }
        this.angle = blocked ? contact(angle, next) : next;
        if (blocked && !impacted) impact();
        if (blocked && settings.physics() == Physics.FALL_APART) {
            breakApart();
            return;
        }
        if (blocked && speed > 0.06 && bounces < 2) {
            // bounces back a bit and falls again
            bounces++;
            speed = -speed * 0.3;
            return;
        }
        // what is left of the fall makes the tree slide on
        float push = (float) (Math.max(0, speed) * length * 0.25);
        velocity.set(direction.getX() * push, 0, direction.getZ() * push);
        speed = 0;
        phase = Phase.SLIDING;
    }

    /**
     * @return the angle between the two (without and with a block hit) at which the tree just touches the block
     */
    private double contact(double free, double blocked) {
        for (int i = 0; i < 6; i++) {
            double middle = (free + blocked) / 2;
            if (collides(middle, shift)) blocked = middle;
            else free = middle;
        }
        return free;
    }

    /**
     * the tree lies on the ground (or floats): it slides, falls down slopes and floats on water
     */
    private void slide() {
        int wet = wet(angle, shift);
        if (wet > 0) {
            splash();
            floating++;
            // the half of a floating tree is below the surface
            velocity.y = velocity.y * 0.6f + (wet * 2 >= probes.size() ? 0.03f : -0.01f);
            velocity.x *= 0.9f;
            velocity.z *= 0.9f;
        } else {
            velocity.y = Math.max(-1, velocity.y - 0.06f);
        }
        boolean supported = false;
        Vector3f moved = new Vector3f(shift).add(0, velocity.y, 0);
        if (collides(angle, moved)) {
            supported = velocity.y <= 0;
            if (!impacted && velocity.y < -0.2f) impact();
            if (velocity.y < -0.3f && bounces < 3) {
                bounces++;
                velocity.y = -velocity.y * 0.25f;
            } else {
                velocity.y = 0;
            }
        } else {
            shift.set(moved);
        }
        moved = new Vector3f(shift).add(velocity.x, 0, velocity.z);
        if (collides(angle, moved)) {
            velocity.x = 0;
            velocity.z = 0;
        } else {
            shift.set(moved);
        }
        if (supported) {
            velocity.x *= 0.6f;
            velocity.z *= 0.6f;
        }
        boolean still = velocity.lengthSquared() < 0.0004f && (supported || wet > 0);
        resting = still ? resting + 1 : 0;
        if (wet == 0 && resting >= REST_TICKS || floating >= FLOAT_TICKS) land();
    }

    /**
     * the physics 'fall-apart': the logs go on on their own, with the speed they had, and the leaves burst
     */
    private void breakApart() {
        phase = Phase.APART;
        Quaternionf rotation = rotation(angle);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = hinge.getWorld();
        boolean particles = Effects.particles(ConfigData.Keys.PARTICLES_LEAVES);
        int burst = 0;
        for (Piece piece : pieces) {
            Vector3f relative = rotation.transform(new Vector3f(piece.offset).add(0.5f, 0.5f, 0.5f));
            if (!piece.log) {
                if (particles && burst++ % 3 == 0) {
                    world.spawnParticle(Particle.BLOCK, world(relative.add(shift)), 4, 0.3, 0.3, 0.3, piece.data);
                }
                BlockDisplay display = piece.display;
                if (display.isValid() && TaskManager.owns(display)) display.remove();
                continue;
            }
            Vector3f tangential = new Vector3f(axis).cross(relative).mul((float) speed * 0.35f);
            piece.center = relative.add(shift);
            piece.velocity = tangential.add((float) random.nextDouble(-0.06, 0.06), (float) random.nextDouble(0.02, 0.14),
                    (float) random.nextDouble(-0.06, 0.06));
            piece.rotation = new Quaternionf(rotation);
            piece.spinAxis = new Vector3f((float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian());
            if (piece.spinAxis.lengthSquared() < 0.001f) piece.spinAxis.set(0, 1, 0);
            piece.spinAxis.normalize();
            piece.spin = (float) random.nextDouble(0.05, 0.25);
        }
    }

    /**
     * the physics 'fall-apart': every log falls, bounces and rolls on its own
     */
    private void tumble() {
        apartTicks++;
        boolean moving = false;
        for (Piece piece : pieces) {
            if (piece.center == null || piece.still) continue;
            Vector3f center = piece.center;
            Vector3f velocity = piece.velocity;
            boolean wet = settings.floatOnWater() && isWater(center.x, center.y, center.z);
            if (wet) {
                splash();
                velocity.y = velocity.y * 0.5f + 0.03f;
                velocity.x *= 0.9f;
                velocity.z *= 0.9f;
            } else {
                velocity.y = Math.max(-1, velocity.y - 0.06f);
            }
            boolean supported = false;
            double x = hinge.getX() + center.x, y = hinge.getY() + center.y, z = hinge.getZ() + center.z;
            if (isObstacle(x, y + velocity.y - BOTTOM, z)) {
                supported = velocity.y <= 0;
                velocity.y = velocity.y < -0.15f ? -velocity.y * 0.3f : 0;
                velocity.x *= 0.6f;
                velocity.z *= 0.6f;
                piece.spin *= 0.6f;
            } else {
                center.y += velocity.y;
            }
            if (isObstacle(x + velocity.x, hinge.getY() + center.y, z + velocity.z)) {
                velocity.x *= -0.3f;
                velocity.z *= -0.3f;
            } else {
                center.x += velocity.x;
                center.z += velocity.z;
            }
            if (piece.spin > 0.01f) piece.rotation = new Quaternionf().fromAxisAngleRad(piece.spinAxis, piece.spin).mul(piece.rotation);
            piece.still = (supported || wet) && velocity.lengthSquared() < 0.0004f;
            moving |= !piece.still;
        }
        if (apartTicks >= APART_TICKS || !moving && apartTicks >= REST_TICKS) land();
    }

    /**
     * sends the positions of the blocks to the players, who see a smooth movement within the ticks
     */
    private void send(int interpolation) {
        Quaternionf rotation = rotation(angle);
        for (Piece piece : pieces) {
            BlockDisplay display = piece.display;
            if (!display.isValid() || !TaskManager.owns(display)) continue;
            Vector3f translation;
            Quaternionf turned;
            if (piece.center != null) {
                // the corner of the block, which the display turns around
                Vector3f half = piece.rotation.transform(new Vector3f(0.5f, 0.5f, 0.5f));
                translation = new Vector3f(piece.center).sub(half).sub(piece.offset);
                turned = piece.rotation;
            } else {
                translation = rotation.transform(new Vector3f(piece.offset)).add(shift).sub(piece.offset);
                turned = rotation;
            }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(interpolation);
            display.setTransformation(new Transformation(translation, turned, new Vector3f(1, 1, 1), new Quaternionf()));
        }
    }

    /**
     * @return true if a log of the tree hits a block at the angle and the shift
     */
    private boolean collides(double angle, Vector3f shift) {
        Quaternionf rotation = rotation(angle);
        for (Vector3f probe : probes) {
            Vector3f at = rotation.transform(new Vector3f(probe)).add(shift);
            if (isObstacle(hinge.getX() + at.x, hinge.getY() + at.y - BOTTOM, hinge.getZ() + at.z)) return true;
        }
        return false;
    }

    /**
     * @return the amount of logs in water, 0 if the tree does not float
     */
    private int wet(double angle, Vector3f shift) {
        if (!settings.floatOnWater()) return 0;
        Quaternionf rotation = rotation(angle);
        int wet = 0;
        for (Vector3f probe : probes) {
            Vector3f at = rotation.transform(new Vector3f(probe)).add(shift);
            if (isWater(at.x, at.y, at.z)) wet++;
        }
        return wet;
    }

    /**
     * @param x relative to the hinge, like y and z
     */
    private boolean isWater(float x, float y, float z) {
        Location at = world(new Vector3f(x, y, z));
        World world = at.getWorld();
        if (at.getY() < world.getMinHeight() || at.getY() >= world.getMaxHeight()) return false;
        if (TaskManager.isFolia() && !TaskManager.owns(at)) return false;
        Block block = at.getBlock();
        Material type = block.getType();
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN || type.name().contains("SEAGRASS") || type.name().startsWith("KELP")) {
            return true;
        }
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    /**
     * @return true if a falling tree stops at the block: solid blocks, not leaves - blocks of other regions (Folia)
     * as well, as they must not be read
     */
    private boolean isObstacle(double x, double y, double z) {
        World world = hinge.getWorld();
        if (y < world.getMinHeight()) return true;
        if (y >= world.getMaxHeight()) return false;
        Location at = new Location(world, x, y, z);
        // the log the player has broken, which the server removes after the event
        if (at.getBlockX() == pivot.getBlockX() && at.getBlockY() == pivot.getBlockY() && at.getBlockZ() == pivot.getBlockZ()) return false;
        if (TaskManager.isFolia() && !TaskManager.owns(at)) return true;
        Material type = at.getBlock().getType();
        return type.isSolid() && !Tag.LEAVES.isTagged(type);
    }

    /**
     * @param relative a position relative to the hinge
     */
    private Location world(Vector3f relative) {
        return hinge.clone().add(relative.x, relative.y, relative.z);
    }

    /**
     * @return where the center of a block is now
     */
    private Location center(Piece piece) {
        if (piece.center != null) return world(piece.center);
        return world(rotation(angle).transform(new Vector3f(piece.offset).add(0.5f, 0.5f, 0.5f)).add(shift));
    }

    /**
     * @return where the center of the block of the location is now, also for blocks that are not shown
     */
    private Location center(Location block) {
        if (settings.physics() == Physics.SIMPLE) return landing(block);
        Piece piece = piecesByBlock.get(block);
        if (piece != null) return center(piece);
        return world(rotation(angle).transform(offset(block).add(0.5f, 0.5f, 0.5f)).add(shift));
    }

    /**
     * some leaves fall from the crown while the tree falls
     */
    private void fallingLeaves() {
        if (leaves.isEmpty() || ticks % 2 != 0 || !Effects.particles(ConfigData.Keys.PARTICLES_FALLING_LEAVES)) return;
        List<Piece> crown = pieces.stream().filter(piece -> !piece.log).toList();
        if (crown.isEmpty()) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 2; i++) {
            Piece leaf = crown.get(random.nextInt(crown.size()));
            hinge.getWorld().spawnParticle(Particle.BLOCK, center(leaf), 2, 0.4, 0.4, 0.4, leaf.data);
        }
    }

    private void splash() {
        if (splashed) return;
        this.splashed = true;
        Location middle = middle();
        Effects.play(middle, Effects.SPLASH, 0);
        if (Effects.particles(ConfigData.Keys.PARTICLES_IMPACT)) {
            middle.getWorld().spawnParticle(Particle.SPLASH, middle, 40, 1.5, 0.3, 1.5);
        }
    }

    /**
     * @return the center of the log in the middle of the tree, where it is now
     */
    private Location middle() {
        return logs.isEmpty() ? hinge.clone() : center(logs.get(logs.size() / 2).location());
    }

    /**
     * the tree hits the ground: particles, a sound, and the mobs and players below it are hit
     */
    private void impact() {
        this.impacted = true;
        World world = hinge.getWorld();
        if (Effects.particles(ConfigData.Keys.PARTICLES_IMPACT)) {
            int step = Math.max(1, logs.size() / MAX_PARTICLES);
            for (int i = 0; i < logs.size(); i += step) {
                Location at = center(logs.get(i).location());
                world.spawnParticle(Particle.BLOCK, at, 12, 0.3, 0.2, 0.3, logs.get(i).data());
                Block below = at.clone().subtract(0, 1, 0).getBlock();
                if (below.getType().isSolid() && (!TaskManager.isFolia() || TaskManager.owns(below))) {
                    world.spawnParticle(Particle.BLOCK, at.clone().subtract(0, 0.4, 0), 6, 0.4, 0.1, 0.4, below.getBlockData());
                }
            }
        }
        Effects.play(middle(), Effects.IMPACT, 0);
        if (settings.knockback() || settings.damage() > 0) hit();
    }

    private void land() {
        if (landed) return;
        this.landed = true;
        FALLING.remove(this);
        if (settings.physics() == Physics.SIMPLE) {
            impact();
        } else if (Effects.particles(ConfigData.Keys.PARTICLES_IMPACT)) {
            // the logs vanish in a bit of dust
            World world = hinge.getWorld();
            int step = Math.max(1, logs.size() / MAX_PARTICLES);
            for (int i = 0; i < logs.size(); i += step) {
                world.spawnParticle(Particle.BLOCK, center(logs.get(i).location()), 6, 0.3, 0.3, 0.3, logs.get(i).data());
            }
        }
        for (Drop drop : drops) dropAt(center(drop.location()), drop.item());
        drops.clear();
        removeDisplays();
    }

    /**
     * pushes away and hurts the mobs and players the logs land on - not the player who cut the tree
     */
    private void hit() {
        Set<LivingEntity> hit = new HashSet<>();
        for (Log log : logs) {
            Location landing = center(log.location());
            if (TaskManager.isFolia() && !TaskManager.owns(landing)) continue;
            for (Entity entity : landing.getWorld().getNearbyEntities(landing, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (entity instanceof LivingEntity living && !entity.equals(player) && !(entity instanceof ArmorStand)) {
                    hit.add(living);
                }
            }
        }
        for (LivingEntity entity : hit) {
            if (TaskManager.isFolia() && !TaskManager.owns(entity)) continue;
            if (settings.damage() > 0) {
                // as the player, so protection and PvP plugins can deny it
                entity.damage(settings.damage(), player);
                if (entity.isDead()) continue;
            }
            if (settings.knockback()) entity.setVelocity(entity.getVelocity().add(direction.clone().multiply(0.7).setY(0.35)));
        }
    }

    /**
     * drops the item at the location, above the ground if the tree has landed in a hill
     */
    private static void dropAt(Location location, ItemStack item) {
        World world = location.getWorld();
        // on Folia the tree can land in another region
        if (TaskManager.isFolia() && !TaskManager.owns(location)) {
            TaskManager.regionLater(location, () -> dropAt(location, item), 1);
            return;
        }
        Location at = location.clone();
        for (int i = 0; i < MAX_LIFT && at.getBlock().getType().isSolid() && at.getY() < world.getMaxHeight() - 1; i++) {
            at.add(0, 1, 0);
        }
        world.dropItemNaturally(at, item);
    }

    private enum Phase {
        TIPPING, SLIDING, APART
    }

    enum Physics {
        SIMPLE, RIGID, FALL_APART;

        static Physics of(String name) {
            return switch (name.trim().toLowerCase(Locale.ROOT).replace('_', '-')) {
                case "simple", "legacy" -> SIMPLE;
                case "fall-apart", "apart" -> FALL_APART;
                default -> RIGID;
            };
        }
    }

    enum Direction {
        AWAY, FACING, SLOPE, RANDOM;

        static Direction of(String name) {
            return switch (name.trim().toLowerCase(Locale.ROOT).replace('_', '-')) {
                case "facing", "looking" -> FACING;
                case "slope", "down-slope", "downhill" -> SLOPE;
                case "random" -> RANDOM;
                default -> AWAY;
            };
        }
    }

    /**
     * @param updateInterval ticks between two positions sent to the players
     * @param glow           the color the tree glows in, null for none
     * @param knockback      true pushes the mobs and players the tree hits away
     * @param damage         what the tree does to them, as the player who cut it (0 for nothing)
     */
    record Settings(Physics physics, Direction direction, boolean floatOnWater, int updateInterval, @Nullable Color glow,
                    boolean knockback, double damage) {
        static final Settings SIMPLE = new Settings(Physics.SIMPLE, Direction.AWAY, false, 1, null, false, 0);

        static Settings of(ConfigData config) {
            return new Settings(Physics.of(config.getString(ConfigData.Keys.TOPPLE_PHYSICS)),
                    Direction.of(config.getString(ConfigData.Keys.TOPPLE_DIRECTION)),
                    config.getBoolean(ConfigData.Keys.TOPPLE_FLOAT_ON_WATER),
                    Math.max(1, Math.min(10, config.getInt(ConfigData.Keys.TOPPLE_UPDATE_INTERVAL))),
                    color(config.getString(ConfigData.Keys.TOPPLE_GLOW)),
                    config.getBoolean(ConfigData.Keys.TOPPLE_KNOCKBACK),
                    Math.max(0, config.getDouble(ConfigData.Keys.TOPPLE_DAMAGE)));
        }

        /**
         * @return the color of '#rrggbb' (white for 'true'), null for '' or 'false'
         */
        @Nullable
        static Color color(String text) {
            String value = text.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty() || value.equals("false")) return null;
            if (value.equals("true")) return Color.WHITE;
            if (value.startsWith("#")) value = value.substring(1);
            try {
                return value.length() == 6 ? Color.fromRGB(Integer.parseInt(value, 16)) : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    private record Log(Location location, BlockData data) {
    }

    private record Drop(Location location, ItemStack item) {
    }

    /**
     * a block of the tree, shown as a display
     */
    private static final class Piece {
        final BlockDisplay display;
        final BlockData data;
        /**
         * the corner of the block relative to the hinge, where the display is
         */
        final Vector3f offset;
        final boolean log;
        /**
         * with the physics 'fall-apart', after the impact: the log on its own, relative to the hinge
         */
        @Nullable
        Vector3f center;
        Vector3f velocity = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        Vector3f spinAxis = new Vector3f(0, 1, 0);
        float spin;
        boolean still;

        Piece(BlockDisplay display, Log block, Vector3f offset, boolean log) {
            this.display = display;
            this.data = block.data();
            this.offset = offset;
            this.log = log;
        }
    }

}
