package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WoodCutter 2026.
 * <p>
 * Finds the logs of the tree above a broken log and breaks them as the player (at once or log by log), lets its
 * leaves decay and plants a new sapling.
 *
 * @author Christian34
 */
public class Cutter {
    public static final String PERMISSION_MAX_BLOCKS = "woodcutter.maxblocks.";
    /**
     * no level or permission can raise the limit above this
     */
    private static final int MAX_BLOCKS_LIMIT = 2000;
    /**
     * logs further away (horizontally) from the broken block don't belong to the tree
     */
    private static final int SEARCH_RADIUS = 25;
    /**
     * logs within this distance are connected, so branches with gaps (e.g. acacias) are found as well
     */
    private static final int REACH = 2;
    /**
     * leaves decay if no log is within this distance
     */
    private static final int LEAF_DISTANCE = 7;
    private static final int MAX_LEAVES = 800;
    /**
     * blocks saplings grow on - by name, as the block tags for this differ between the Minecraft versions (the dirt
     * tag lost grass blocks in 26.1) and some blocks are newer than 1.21
     */
    private static final Set<String> SOIL = Set.of("DIRT", "GRASS_BLOCK", "PODZOL", "COARSE_DIRT", "MYCELIUM", "ROOTED_DIRT",
            "MOSS_BLOCK", "PALE_MOSS_BLOCK", "MUD", "MUDDY_MANGROVE_ROOTS", "FARMLAND");
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    /**
     * players who are breaking logs of a tree right now: they are broken as the player, which calls the listeners
     */
    private static final Set<UUID> CUTTING = ConcurrentHashMap.newKeySet();
    /**
     * the log a player has broken to cut a tree, until the server has dropped its items
     */
    private static final Map<UUID, Block> ORIGINS = new ConcurrentHashMap<>();
    private final Player player;
    private final Block block;
    private final int maxBlocks;
    private final boolean cutPlacedLogs;
    private final boolean decayLeaves;
    private final boolean replant;
    private final boolean freeSapling;
    private final boolean dropsToInventory;
    private final double bonusDropChance;
    /**
     * 0 cuts the tree at once
     */
    private final int blocksPerTick;
    private final Map<Chunk, Set<Integer>> placedLogs = new HashMap<>();
    private List<Block> logs = List.of();
    private List<Block> leaves = List.of();

    public Cutter(WoodCutter instance, Player player, Block block, int maxBlocks) {
        ConfigData config = instance.getConfigData();
        this.player = player;
        this.block = block;
        this.maxBlocks = maxBlocks;
        this.cutPlacedLogs = config.getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES);
        this.decayLeaves = config.getBoolean(ConfigData.Keys.FAST_LEAF_DECAY);
        this.replant = config.getBoolean(ConfigData.Keys.REPLANT);
        this.freeSapling = config.getBoolean(ConfigData.Keys.REPLANT_FREE);
        this.dropsToInventory = config.getBoolean(ConfigData.Keys.DROPS_TO_INVENTORY);
        this.bonusDropChance = config.getDouble(ConfigData.Keys.BONUS_DROPS_CHANCE);
        this.blocksPerTick = config.getBoolean(ConfigData.Keys.ANIMATION) ? Math.max(1, config.getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK)) : 0;
    }

    /**
     * @return true while logs of a tree are broken as the player
     */
    public static boolean isCutting(Player player) {
        return CUTTING.contains(player.getUniqueId());
    }

    /**
     * @return true if the block belongs to a tree the player is cutting (the log they have broken or one of the tree)
     */
    public static boolean isCutBy(Player player, Block block) {
        return isCutting(player) || block.equals(ORIGINS.get(player.getUniqueId()));
    }

    /**
     * @param level the level of the enchantment
     * @return the amount of logs the player may cut at once: the amount of the level or the highest amount of
     * their permissions woodcutter.maxblocks.&lt;amount&gt; (e.g. for ranks), whichever is higher
     */
    public static int maxBlocks(Player player, ConfigData config, int level) {
        int max = config.getInt(ConfigData.Keys.MAX_BLOCKS) + (Math.max(1, level) - 1) * config.getInt(ConfigData.Keys.LEVELS_ADDITIONAL_BLOCKS);
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission().toLowerCase(Locale.ROOT);
            if (!info.getValue() || !permission.startsWith(PERMISSION_MAX_BLOCKS)) continue;
            try {
                max = Math.max(max, Integer.parseInt(permission.substring(PERMISSION_MAX_BLOCKS.length())));
            } catch (NumberFormatException ignored) {
                // not a number, e.g. woodcutter.maxblocks.*
            }
        }
        return Math.min(MAX_BLOCKS_LIMIT, max);
    }

    /**
     * @return the sapling (or fungus) that grows the tree of the log, null if there is none
     */
    @Nullable
    static Material sapling(Material log) {
        String type = Statistics.treeType(log);
        String name = switch (type) {
            case "mangrove" -> "mangrove_propagule";
            case "crimson", "warped" -> type + "_fungus";
            default -> type + "_sapling";
        };
        return Material.matchMaterial(name);
    }

    private static boolean isGround(Material sapling, Block ground) {
        if (!Tag.SAPLINGS.isTagged(sapling)) return Tag.NYLIUM.isTagged(ground.getType());
        return SOIL.contains(ground.getType().name());
    }

    /**
     * @return the logs that are connected to the broken block and not below it, without the broken block itself
     * (the server breaks it)
     */
    List<Block> findLogs() {
        List<Block> logs = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        visited.add(block);
        queue.add(block);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            for (int x = -REACH; x <= REACH; x++) {
                for (int y = 0; y <= REACH; y++) {
                    for (int z = -REACH; z <= REACH; z++) {
                        if (logs.size() >= maxBlocks) return logs;
                        Block next = current.getRelative(x, y, z);
                        if (!isInRange(next) || !visited.add(next) || !Tag.LOGS.isTagged(next.getType())) continue;
                        // logs of a building next to the tree
                        if (!cutPlacedLogs && isPlaced(next)) continue;
                        logs.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return logs;
    }

    private boolean isPlaced(Block log) {
        return placedLogs.computeIfAbsent(log.getChunk(), PlacedLogs::read).contains(PlacedLogs.pack(log));
    }

    /**
     * @return the leaves that are connected to the logs (through other leaves), which may belong to the tree
     */
    List<Block> findLeaves(List<Block> logs) {
        List<Block> leaves = new ArrayList<>();
        Map<Block, Integer> distances = new HashMap<>();
        Deque<Block> queue = new ArrayDeque<>();
        distances.put(block, 0);
        queue.add(block);
        for (Block log : logs) {
            distances.put(log, 0);
            queue.add(log);
        }

        while (!queue.isEmpty() && leaves.size() < MAX_LEAVES) {
            Block current = queue.poll();
            int distance = distances.get(current) + 1;
            if (distance > LEAF_DISTANCE) continue;
            for (BlockFace face : FACES) {
                Block next = current.getRelative(face);
                if (!isInRange(next) || distances.containsKey(next) || !Tag.LEAVES.isTagged(next.getType())) continue;
                distances.put(next, distance);
                leaves.add(next);
                queue.add(next);
            }
        }
        return leaves;
    }

    private boolean isInRange(Block target) {
        if (Math.abs(target.getX() - block.getX()) > SEARCH_RADIUS || Math.abs(target.getZ() - block.getZ()) > SEARCH_RADIUS) {
            return false;
        }
        if (target.getY() < target.getWorld().getMinHeight() || target.getY() >= target.getWorld().getMaxHeight()) {
            return false;
        }
        // on Folia blocks of other regions must not be touched
        return !TaskManager.isFolia() || Bukkit.isOwnedByCurrentRegion(target);
    }

    /**
     * Cuts the tree - at once or, with the animation, from the bottom to the top within the next ticks. The logs are
     * broken as the player: protection plugins can deny single logs, logging and skill plugins notice them, and the
     * server damages the axe and drops the items. It stops when the axe breaks or the player leaves.
     *
     * @return the amount of logs of the tree (without the block the player has broken), 0 if it is a single log
     */
    public int cut() {
        this.logs = findLogs();
        if (logs.isEmpty()) return 0;
        this.leaves = decayLeaves ? findLeaves(logs) : List.of();
        Material sapling = replant ? plantableSapling() : null;
        // the server removes the broken log and drops its items after the event
        ORIGINS.put(player.getUniqueId(), block);
        TaskManager.regionLater(block.getLocation(), () -> {
            ORIGINS.remove(player.getUniqueId(), block);
            if (sapling != null) plant(sapling);
        }, 1);

        if (blocksPerTick == 0) {
            breakLogs(0, logs.size());
            finish();
        } else {
            logs.sort(Comparator.comparingInt(Block::getY));
            step(0);
        }
        return logs.size();
    }

    private void step(int from) {
        int to = Math.min(logs.size(), from + blocksPerTick);
        if (!breakLogs(from, to) || to >= logs.size()) {
            finish();
            return;
        }
        TaskManager.runLater(player, () -> step(to), this::finish, 1);
    }

    /**
     * @return false if the player can't cut the tree any further
     */
    private boolean breakLogs(int from, int to) {
        if (!player.isOnline() || !player.getWorld().equals(block.getWorld())) return false;
        CUTTING.add(player.getUniqueId());
        try {
            for (Block target : logs.subList(from, to)) {
                if (!WoodCutterEnchantment.isAxe(player.getInventory().getItemInMainHand())) return false;
                BlockData data = target.getBlockData();
                Material type = data.getMaterial();
                // changed since the tree was found
                if (!Tag.LOGS.isTagged(type)) continue;
                if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(target)) continue;
                if (!player.breakBlock(target)) continue;
                // the player does not get the sound and particles of blocks they break themselves
                if (blocksPerTick > 0) playBreakEffect(target, data);
                if (bonusDropChance > 0 && player.getGameMode() != GameMode.CREATIVE
                        && ThreadLocalRandom.current().nextDouble(100) < bonusDropChance) {
                    drop(target, new ItemStack(type));
                }
            }
        } finally {
            CUTTING.remove(player.getUniqueId());
        }
        return true;
    }

    private void playBreakEffect(Block broken, BlockData data) {
        Location center = broken.getLocation().add(0.5, 0.5, 0.5);
        // by its name, as the sounds became a registry in 1.21.3
        player.playSound(center, "minecraft:block.wood.break", 1, 0.8f);
        player.spawnParticle(Particle.BLOCK, center, 20, 0.25, 0.25, 0.25, data);
    }

    private void drop(Block from, ItemStack item) {
        if (dropsToInventory) {
            for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        } else {
            from.getWorld().dropItemNaturally(from.getLocation().add(0.5, 0.5, 0.5), item);
        }
    }

    /**
     * called when the last log was broken
     */
    private void finish() {
        if (leaves.isEmpty()) return;
        // the server needs some ticks to notice that the leaves lost their logs, slow servers get a second chance
        TaskManager.regionLater(block.getLocation(), () -> decay(leaves), 40);
        TaskManager.regionLater(block.getLocation(), () -> decay(leaves), 100);
    }

    @Nullable
    private Material plantableSapling() {
        Material sapling = sapling(block.getType());
        if (sapling == null || !isGround(sapling, block.getRelative(BlockFace.DOWN))) return null;
        return sapling;
    }

    /**
     * plants the sapling as the player: protection plugins can deny it, logging plugins record it
     */
    private void plant(Material sapling) {
        Block ground = block.getRelative(BlockFace.DOWN);
        if (!player.isOnline() || !block.getType().isAir() || !isGround(sapling, ground)) return;
        ItemStack item = new ItemStack(sapling);
        boolean paid = !freeSapling && player.getGameMode() != GameMode.CREATIVE;
        if (paid && !player.getInventory().removeItem(item.clone()).isEmpty()) return;

        BlockState replaced = block.getState();
        block.setType(sapling);
        BlockPlaceEvent event = new BlockPlaceEvent(block, replaced, ground, item, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !event.canBuild()) {
            replaced.update(true, false);
            if (paid) player.getInventory().addItem(item);
        }
    }

    /**
     * breaks the leaves the server would let decay sooner or later: not placed by a player and without a log nearby
     */
    private static void decay(List<Block> leaves) {
        for (Block leaf : leaves) {
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(leaf)) continue;
            if (!(leaf.getBlockData() instanceof Leaves data) || data.isPersistent() || data.getDistance() < LEAF_DISTANCE) {
                continue;
            }
            LeavesDecayEvent event = new LeavesDecayEvent(leaf);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) leaf.breakNaturally();
        }
    }

}
