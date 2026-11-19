package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Permissions;
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
import org.bukkit.block.Beehive;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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
     * logs below a log that are searched for the ground the trunk stands on
     */
    private static final int TRUNK_HEIGHT = 64;
    /**
     * leaves decay if no log is within this distance
     */
    private static final int LEAF_DISTANCE = 7;
    private static final int MAX_LEAVES = 800;
    /**
     * leaves that decay in a tick of the animation for every log that is broken in a tick
     */
    private static final int LEAVES_PER_LOG = 4;
    /**
     * blocks saplings grow on - by name, as the block tags for this differ between the Minecraft versions (the dirt
     * tag lost grass blocks in 26.1) and some blocks are newer than 1.21
     */
    private static final Set<String> SOIL = Set.of("DIRT", "GRASS_BLOCK", "PODZOL", "COARSE_DIRT", "MYCELIUM", "ROOTED_DIRT",
            "MOSS_BLOCK", "PALE_MOSS_BLOCK", "MUD", "MUDDY_MANGROVE_ROOTS", "FARMLAND");
    /**
     * what grows at the stems of the trees of the Nether instead of leaves
     */
    private static final Set<String> CROWNS = Set.of("NETHER_WART_BLOCK", "WARPED_WART_BLOCK", "SHROOMLIGHT");
    private static final Set<Material> MUSHROOM = Set.of(Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK);
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    /**
     * players who are breaking logs of a tree right now: they are broken as the player, which calls the listeners
     */
    private static final Set<UUID> CUTTING = ConcurrentHashMap.newKeySet();
    /**
     * the log a player has broken to cut a tree, until the server has dropped its items
     */
    private static final Map<UUID, Block> ORIGINS = new ConcurrentHashMap<>();
    /**
     * the trees that tip over while their logs are broken, if the items drop where the tree lands
     */
    private static final Map<UUID, FallingTree> LANDING = new ConcurrentHashMap<>();
    private final Player player;
    private final Block block;
    private final int maxBlocks;
    private final boolean cutPlacedLogs;
    private final boolean decayLeaves;
    private final boolean replant;
    private final boolean freeSapling;
    private final boolean dropsToInventory;
    private final double bonusDropChance;
    private final double foresterLuck;
    private final double experiencePerLog;
    private final boolean protectAxe;
    /**
     * the type of the broken log, which is air when the tree has been cut
     */
    private final Material logType;
    /**
     * the logs of the tree that have been broken so far
     */
    private int broken;
    private final boolean requireLeaves;
    /**
     * a huge mushroom is cut: its stem and its cap
     */
    private final boolean mushroom;
    /**
     * the items of the luck of a forester and their weights
     */
    private final Map<Material, Integer> luckyDrops;
    /**
     * the tree does not just disappear
     */
    private final boolean animated;
    /**
     * the tree is cut at once and tips over
     */
    private final boolean topple;
    /**
     * 0 cuts the tree at once
     */
    private final int blocksPerTick;
    /**
     * with the animation 'topple': the items drop where the tree hits the ground
     */
    private final boolean dropsWhereItLands;
    /**
     * how a tree of the animation 'topple' falls
     */
    private final FallingTree.Settings toppleSettings;
    /**
     * planted saplings can't be broken by players until they grow
     */
    private final boolean protectSaplings;
    private final boolean angryBees;
    private final boolean dropNests;
    /**
     * with the animation 'topple': "Timber!" is shown to the player
     */
    private final boolean timber;
    @Nullable
    private Material sapling;
    /**
     * the other logs of a trunk of 2x2 logs, which get a sapling as well
     */
    private List<Block> trunk = List.of();
    @Nullable
    private FallingTree fallingTree;
    private final Map<Chunk, Set<Integer>> placedLogs = new HashMap<>();
    private List<Block> logs = List.of();
    private List<Block> leaves = List.of();

    public Cutter(WoodCutter instance, Player player, Block block, int maxBlocks) {
        ConfigData config = instance.getConfigData();
        this.player = player;
        this.block = block;
        this.maxBlocks = maxBlocks;
        this.logType = block.getType();
        this.cutPlacedLogs = config.getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES);
        this.decayLeaves = config.getBoolean(ConfigData.Keys.FAST_LEAF_DECAY);
        // the perks of the woodcutting skill add to the settings
        Skill skill = instance.getSkill();
        boolean replantUnlocked = skill.isUnlocked(player, Skill.UNLOCK_REPLANT);
        this.replant = replantUnlocked || config.getBoolean(ConfigData.Keys.REPLANT);
        this.freeSapling = replantUnlocked || config.getBoolean(ConfigData.Keys.REPLANT_FREE);
        this.dropsToInventory = dropsToInventory(instance, player);
        this.bonusDropChance = config.getDouble(ConfigData.Keys.BONUS_DROPS_CHANCE) + skill.perk(player, Skill.PERK_BONUS_DROPS);
        this.foresterLuck = skill.perk(player, Skill.PERK_FORESTER_LUCK);
        this.experiencePerLog = Math.max(0, config.getDouble(ConfigData.Keys.EXPERIENCE_DROPS_PER_LOG));
        this.protectAxe = config.getBoolean(ConfigData.Keys.PROTECT_AXE);
        this.requireLeaves = config.getBoolean(ConfigData.Keys.REQUIRE_LEAVES);
        this.mushroom = logType == Material.MUSHROOM_STEM;
        this.luckyDrops = foresterLuck > 0 ? luckyDrops(config.getList(ConfigData.Keys.SKILL_LUCKY_DROPS), logType) : Map.of();
        this.animated = config.getBoolean(ConfigData.Keys.ANIMATION);
        this.topple = animated && config.getString(ConfigData.Keys.ANIMATION_MODE).equalsIgnoreCase("topple");
        this.blocksPerTick = !animated || topple ? 0
                : Math.max(1, config.getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK)) + (int) Math.round(skill.perk(player, Skill.PERK_SPEED));
        this.dropsWhereItLands = topple && !dropsToInventory && config.getBoolean(ConfigData.Keys.TOPPLE_DROPS_WHERE_IT_LANDS);
        this.toppleSettings = topple ? FallingTree.Settings.of(config) : FallingTree.Settings.SIMPLE;
        this.protectSaplings = config.getBoolean(ConfigData.Keys.REPLANT_PROTECT);
        this.angryBees = config.getBoolean(ConfigData.Keys.BEES_ANGRY);
        this.dropNests = config.getBoolean(ConfigData.Keys.BEES_DROP_NEST);
        this.timber = config.getBoolean(ConfigData.Keys.TOPPLE_TIMBER);
    }

    /**
     * with the animation 'topple': the items of a log the player breaks drop where the tree lands
     *
     * @return false if the item drops as usual
     */
    public static boolean dropWhereItLands(Player player, Block block, ItemStack item) {
        FallingTree tree = isCutting(player) ? LANDING.get(player.getUniqueId()) : null;
        if (tree == null) return false;
        tree.drop(block, item);
        return true;
    }

    /**
     * drops the items of the trees that are falling right now where they stood, e.g. when the plugin is disabled
     */
    public static void landAll() {
        LANDING.clear();
        FallingTree.landAll();
    }

    /**
     * @return true if a tree is cut when a player breaks the block: a log or the stem of a huge mushroom
     */
    public static boolean isTrunk(Material type, ConfigData config) {
        return TreeBlocks.isLog(type) || type == Material.MUSHROOM_STEM && config.getBoolean(ConfigData.Keys.MUSHROOMS);
    }

    /**
     * @return true for the blocks trees and huge mushrooms are made of, which are remembered when players place them
     */
    public static boolean isWood(Material type) {
        return TreeBlocks.isAnyLog(type) || MUSHROOM.contains(type);
    }

    /**
     * @return true if the block belongs to the tree: a log, or a block of the huge mushroom
     */
    private boolean isLog(Material type) {
        return mushroom ? MUSHROOM.contains(type) : TreeBlocks.isLog(type);
    }

    /**
     * @return true if the drops of a tree the player cuts are put into their inventory: by the setting or unlocked
     * with the woodcutting skill
     */
    public static boolean dropsToInventory(WoodCutter instance, Player player) {
        return instance.getConfigData().getBoolean(ConfigData.Keys.DROPS_TO_INVENTORY)
                || instance.getSkill().isUnlocked(player, Skill.UNLOCK_DROPS_TO_INVENTORY);
    }

    /**
     * @return true while the player cuts a tree, including the log they have broken themselves
     */
    public static boolean isCuttingTree(Player player) {
        return isCutting(player) || ORIGINS.containsKey(player.getUniqueId());
    }

    /**
     * @return the amount of blocks the item can break until it breaks itself
     */
    static int hitsLeft(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE || !(item.getItemMeta() instanceof Damageable meta) || meta.isUnbreakable()) {
            return Integer.MAX_VALUE;
        }
        int durability = meta.hasMaxDamage() ? meta.getMaxDamage() : item.getType().getMaxDurability();
        return durability <= 0 ? Integer.MAX_VALUE : durability - meta.getDamage();
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
     * @return the amount of logs the player may cut at once: the amount of the level (with the perk of the
     * woodcutting skill) or the highest amount of their permissions woodcutter.maxblocks.&lt;amount&gt; (e.g. for
     * ranks), whichever is higher
     */
    public static int maxBlocks(Player player, ConfigData config, int level) {
        int max = config.getInt(ConfigData.Keys.MAX_BLOCKS) + (Math.max(1, level) - 1) * config.getInt(ConfigData.Keys.LEVELS_ADDITIONAL_BLOCKS);
        max += (int) WoodCutter.getInstance().getSkill().perk(player, Skill.PERK_MAX_BLOCKS);
        max = Math.max(max, Permissions.numbers(player, PERMISSION_MAX_BLOCKS).max().orElse(0));
        return Math.min(MAX_BLOCKS_LIMIT, max);
    }

    /**
     * @return the sapling (or fungus) that grows the tree of the log, null if there is none
     */
    @Nullable
    static Material sapling(Material log) {
        return TreeBlocks.sapling(log);
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
                        if (!isInRange(next) || !visited.add(next) || !isLog(next.getType())) continue;
                        // the trunk of a tree next to this one
                        if (isOtherTrunk(next)) continue;
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

    /**
     * Logs within the reach are connected, so a tree next to the cut one would be found as a branch. Branches hang in
     * the air, but the logs of a trunk stand on the ground: a trunk that stands further away from the broken block
     * than the logs of a trunk of 2x2 (or a cross of the Nether) belongs to another tree.
     *
     * @return true if the log is part of the trunk of another tree
     */
    private boolean isOtherTrunk(Block log) {
        Block bottom = log;
        for (int i = 0; i < TRUNK_HEIGHT; i++) {
            Block below = bottom.getRelative(BlockFace.DOWN);
            if (!isInRange(below) || !isLog(below.getType())) break;
            bottom = below;
        }
        Material ground = bottom.getRelative(BlockFace.DOWN).getType();
        if (!ground.isSolid() || TreeBlocks.isLeaves(ground) || CROWNS.contains(ground.name())) return false;
        int x = bottom.getX() - block.getX();
        int z = bottom.getZ() - block.getZ();
        // the trunk of the tree itself, or another log of its trunk next to it
        if (Math.abs(x) + Math.abs(z) <= 1) return false;
        // the corner of a trunk of 2x2 logs - a tree that just touches it diagonally is another one
        return Math.abs(x) != 1 || Math.abs(z) != 1
                || !isLog(bottom.getRelative(-x, 0, 0).getType()) || !isLog(bottom.getRelative(0, 0, -z).getType());
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
                if (!isInRange(next) || distances.containsKey(next) || !TreeBlocks.isLeaves(next.getType())) continue;
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
        return !TaskManager.isFolia() || TaskManager.owns(target);
    }

    /**
     * @return the amount of logs of the tree the player would cut (without the block itself), 0 if it is no tree -
     * nothing is changed
     */
    public int count() {
        this.logs = findLogs();
        if (logs.isEmpty() || requireLeaves && !mushroom && !hasCrown()) return 0;
        return logs.size();
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
        // the log the player has broken costs a hit as well, a second one has to be left for the axe
        if (protectAxe && hitsLeft(player, player.getInventory().getItemInMainHand()) <= 2) {
            Message.AXE_ALMOST_BROKEN.sendActionBar(player);
            return 0;
        }
        // logs without leaves are rather a building than a tree
        if (requireLeaves && !mushroom && !hasCrown()) return 0;
        this.leaves = decayLeaves ? findLeaves(logs) : List.of();
        this.sapling = replant ? plantableSapling() : null;
        if (sapling != null) this.trunk = findTrunk();
        List<Block> attached = findAttached();
        // the server removes the broken log and drops its items after the event
        ORIGINS.put(player.getUniqueId(), block);
        TaskManager.regionLater(block.getLocation(), () -> {
            ORIGINS.remove(player.getUniqueId(), block);
            if (sapling != null) plant(block, trunk.isEmpty());
        }, 1);

        if (topple) {
            int height = logs.stream().mapToInt(Block::getY).max().orElse(block.getY()) - block.getY() + 1;
            FallingTree tree = new FallingTree(block, player, toppleSettings, height);
            tree.add(block, block.getBlockData());
            this.fallingTree = tree;
            // cocoa, vines and nests would drop or stay in the air when their log is gone
            removeAttached(attached);
            if (dropsWhereItLands) LANDING.put(player.getUniqueId(), tree);
            try {
                breakLogs(0, logs.size());
            } finally {
                LANDING.remove(player.getUniqueId(), tree);
            }
            // the crown falls with the trunk, else it would stay in the air and hide the falling logs
            for (Block leaf : crown()) {
                BlockData data = leaf.getBlockData();
                if (decayLeaf(leaf, dropsWhereItLands ? tree : null, false)) tree.addLeaf(leaf, data);
            }
            if (tree.start() && timber) Message.TIMBER.sendSubtitle(player);
            finish();
        } else if (blocksPerTick == 0) {
            removeAttached(attached);
            breakLogs(0, logs.size());
            finish();
        } else {
            removeAttached(attached);
            // layer by layer, every layer from the trunk to the tips of its branches
            logs.sort(Comparator.comparingInt(Block::getY).thenComparingInt(this::distanceToTrunk));
            step(0);
        }
        return logs.size();
    }

    /**
     * @return the blocks on the logs of the tree that are removed with it (setting 'attached-blocks', and the vines
     * below them) and the nests of bees
     */
    private List<Block> findAttached() {
        if (mushroom) return List.of();
        List<Block> attached = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        List<Block> trunk = new ArrayList<>(logs);
        trunk.add(block);
        for (Block log : trunk) {
            for (BlockFace face : FACES) {
                Block next = log.getRelative(face);
                if (!isInRange(next) || !visited.add(next)) continue;
                Material type = next.getType();
                if (type != Material.BEE_NEST && !TreeBlocks.isAttached(type)) continue;
                attached.add(next);
                // the vines that hang below
                Block below = next.getRelative(BlockFace.DOWN);
                while (type == Material.VINE && below.getType() == Material.VINE && isInRange(below) && visited.add(below)) {
                    attached.add(below);
                    below = below.getRelative(BlockFace.DOWN);
                }
            }
        }
        return attached;
    }

    /**
     * removes the blocks on the tree as the player (protection plugins can deny it), their items drop like the ones
     * of the logs - the bees of a nest fly out and attack the player
     */
    private void removeAttached(List<Block> attached) {
        ItemStack axe = player.getInventory().getItemInMainHand();
        for (Block target : attached) {
            if (TaskManager.isFolia() && !TaskManager.owns(target)) continue;
            BlockData data = target.getBlockData();
            BlockBreakEvent event = new BlockBreakEvent(target, player);
            // nests drop nothing without silk touch, unless the setting wants it
            boolean nest = data.getMaterial() == Material.BEE_NEST;
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            List<ItemStack> items = new ArrayList<>();
            if (event.isDropItems() && player.getGameMode() != GameMode.CREATIVE) {
                if (nest) {
                    if (dropNests) items.add(new ItemStack(Material.BEE_NEST));
                } else {
                    items.addAll(target.getDrops(axe, player));
                }
            }
            if (nest) releaseBees(target);
            target.setType(Material.AIR, false);
            for (ItemStack item : items) drop(target, item);
            if (fallingTree != null) fallingTree.addLeaf(target, data);
        }
    }

    /**
     * the bees of the nest fly out, they and the bees outside that belong to it attack the player
     */
    private void releaseBees(Block nest) {
        List<Bee> bees = new ArrayList<>();
        if (nest.getState() instanceof Beehive hive && hive.getEntityCount() > 0) bees.addAll(hive.releaseEntities());
        Location location = nest.getLocation();
        for (Entity entity : nest.getWorld().getNearbyEntities(location.clone().add(0.5, 0.5, 0.5), 32, 32, 32, Bee.class::isInstance)) {
            Bee bee = (Bee) entity;
            if (location.equals(bee.getHive()) && !bees.contains(bee)) bees.add(bee);
        }
        if (!angryBees || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        for (Bee bee : bees) {
            if (TaskManager.isFolia() && !TaskManager.owns(bee)) continue;
            bee.setAnger(400);
            bee.setTarget(player);
        }
    }

    /**
     * @return true if leaves (or the wart blocks of the trees of the Nether) grow at a log of the tree
     */
    private boolean hasCrown() {
        if (isCrown(block)) return true;
        for (Block log : logs) {
            if (isCrown(log)) return true;
        }
        return false;
    }

    private static boolean isCrown(Block log) {
        for (BlockFace face : FACES) {
            Material type = log.getRelative(face).getType();
            if (TreeBlocks.isLeaves(type) || CROWNS.contains(type.name())) return true;
        }
        return false;
    }

    /**
     * @return the other three logs next to the broken one if the tree has a trunk of 2x2 logs (e.g. dark oaks, which
     * only grow from four saplings), else none
     */
    private List<Block> findTrunk() {
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                List<Block> others = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    Block log = block.getRelative(x + i % 2, 0, z + i / 2);
                    if (!log.equals(block) && log.getType() == logType && isInRange(log)) others.add(log);
                }
                if (others.size() == 3) return others;
            }
        }
        return List.of();
    }

    private int distanceToTrunk(Block target) {
        int x = target.getX() - block.getX();
        int z = target.getZ() - block.getZ();
        return x * x + z * z;
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
            for (int i = from; i < to; i++) {
                Block target = logs.get(i);
                ItemStack axe = player.getInventory().getItemInMainHand();
                if (!WoodCutterEnchantment.isAxe(axe)) return false;
                // the server damages the axe for the log the player has broken after the event
                int reserved = block.equals(ORIGINS.get(player.getUniqueId())) ? 2 : 1;
                if (protectAxe && hitsLeft(player, axe) <= reserved) {
                    Message.AXE_ALMOST_BROKEN.sendActionBar(player);
                    return false;
                }
                BlockData data = target.getBlockData();
                Material type = data.getMaterial();
                // changed since the tree was found
                if (!isLog(type)) continue;
                if (TaskManager.isFolia() && !TaskManager.owns(target)) continue;
                if (!player.breakBlock(target)) continue;
                broken++;
                // the player does not get the sound and particles of blocks they break themselves
                if (blocksPerTick > 0) playBreakEffect(target, data, (float) i / logs.size());
                if (fallingTree != null) fallingTree.add(target, data);
                // the blocks of a huge mushroom are no items players can get
                if (bonusDropChance > 0 && !mushroom && player.getGameMode() != GameMode.CREATIVE
                        && ThreadLocalRandom.current().nextDouble(100) < bonusDropChance) {
                    drop(target, new ItemStack(type));
                }
            }
        } finally {
            CUTTING.remove(player.getUniqueId());
        }
        return true;
    }

    /**
     * @param progress 0 for the first log of the tree to 1 for the last one: the sound gets higher towards the top
     */
    private void playBreakEffect(Block broken, BlockData data, float progress) {
        Location center = broken.getLocation().add(0.5, 0.5, 0.5);
        Effects.playTo(player, center, Effects.CHOP, 0.5f * progress);
        player.spawnParticle(Particle.BLOCK, center, 20, 0.25, 0.25, 0.25, data);
    }

    private void drop(Block from, ItemStack item) {
        if (dropsWhereItLands && fallingTree != null) {
            fallingTree.drop(from, item);
        } else if (dropsToInventory) {
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
        if (experiencePerLog > 0) TaskManager.regionLater(block.getLocation(), this::dropExperience, 1);
        // the other saplings of a big tree, when its logs are gone
        if (!trunk.isEmpty()) TaskManager.regionLater(block.getLocation(), () -> trunk.forEach(log -> plant(log, false)), 2);
        if (leaves.isEmpty()) return;
        // with the animation the leaves follow the logs: from the bottom to the top within two seconds at most
        int leavesPerTick = !animated ? leaves.size() : Math.max(Math.max(1, blocksPerTick) * LEAVES_PER_LOG, (leaves.size() + 39) / 40);
        if (animated) leaves.sort(Comparator.comparingInt(Block::getY));
        // the server needs some ticks to notice that the leaves lost their logs, slow servers get a second chance
        TaskManager.regionLater(block.getLocation(), () -> decay(0, leavesPerTick), 40);
        TaskManager.regionLater(block.getLocation(), () -> decay(0, leavesPerTick), 100);
    }

    /**
     * drops the experience of the cut logs (and the one the player has broken) where the tree stood
     */
    private void dropExperience() {
        double exact = (broken + 1) * experiencePerLog;
        int points = (int) exact;
        // the rest is the chance for another point
        if (ThreadLocalRandom.current().nextDouble() < exact - points) points++;
        if (points <= 0) return;
        if (dropsToInventory && player.isOnline() && TaskManager.owns(player)) {
            player.giveExp(points);
            return;
        }
        int experience = points;
        block.getWorld().spawn(block.getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class, orb -> orb.setExperience(experience));
    }

    @Nullable
    private Material plantableSapling() {
        Material sapling = sapling(logType);
        if (sapling == null || !isGround(sapling, block.getRelative(BlockFace.DOWN))) return null;
        return sapling;
    }

    /**
     * @return true if a sapling can be planted there: air or a plant like grass, with ground for the sapling below
     */
    private boolean canPlant(Block target) {
        if (!isInRange(target)) return false;
        Material type = target.getType();
        boolean free = type.isAir() || !target.isLiquid() && Tag.REPLACEABLE.isTagged(type) && !Tag.SAPLINGS.isTagged(type);
        return free && isGround(sapling, target.getRelative(BlockFace.DOWN));
    }

    /**
     * plants the sapling as the player: protection plugins can deny it, logging plugins record it
     *
     * @param movable true plants it next to the stump if something is on it (not for the saplings of big trees)
     */
    private void plant(Block block, boolean movable) {
        if (!player.isOnline()) return;
        if (!canPlant(block)) {
            if (!movable) return;
            Block free = null;
            // the closest free place around the stump, also one block higher or lower
            for (int radius = 1; radius <= 2 && free == null; radius++) {
                for (int x = -radius; x <= radius && free == null; x++) {
                    for (int z = -radius; z <= radius && free == null; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                        for (int y : new int[]{0, 1, -1}) {
                            Block candidate = block.getRelative(x, y, z);
                            if (canPlant(candidate)) {
                                free = candidate;
                                break;
                            }
                        }
                    }
                }
            }
            if (free == null) return;
            block = free;
        }
        Block ground = block.getRelative(BlockFace.DOWN);
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
        } else if (protectSaplings) {
            PlantedSaplings.mark(block);
        }
    }

    /**
     * @return the items a leaf can drop with the luck of a forester and their weights, read from the config
     * (item:weight, 'sapling' is the sapling of the tree)
     */
    private static Map<Material, Integer> luckyDrops(List<String> entries, Material logType) {
        Map<Material, Integer> weights = new HashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            String name = parts[0].trim();
            Material item = name.equalsIgnoreCase("sapling") ? sapling(logType) : Material.matchMaterial(name);
            int weight;
            try {
                weight = parts.length == 2 ? Integer.parseInt(parts[1].trim()) : 1;
            } catch (NumberFormatException ex) {
                continue;
            }
            // trees without a sapling and unknown items are skipped
            if (item == null || !item.isItem() || weight <= 0) continue;
            weights.merge(item, weight, Integer::sum);
        }
        return weights;
    }

    /**
     * @return what a leaf drops with the luck of a forester: one of the items of the config, null for nothing
     */
    @Nullable
    private Material luckyDrop() {
        int total = luckyDrops.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return null;
        int random = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Material, Integer> entry : luckyDrops.entrySet()) {
            random -= entry.getValue();
            if (random < 0) return entry.getKey();
        }
        return null;
    }

    /**
     * breaks the leaves the server would let decay sooner or later: not placed by a player and without a log nearby
     */
    private void decay(int from, int leavesPerTick) {
        int to = Math.min(leaves.size(), from + leavesPerTick);
        boolean rustled = false;
        boolean particles = Effects.particles(ConfigData.Keys.PARTICLES_LEAVES);
        for (Block leaf : leaves.subList(from, to)) {
            BlockData data = leaf.getBlockData();
            if (!decayLeaf(leaf, null, true) || !animated) continue;
            Location center = leaf.getLocation().add(0.5, 0.5, 0.5);
            if (particles) leaf.getWorld().spawnParticle(Particle.BLOCK, center, 6, 0.3, 0.3, 0.3, data);
            // one sound for the leaves of a tick
            if (!rustled) Effects.play(center, Effects.LEAVES, 0);
            rustled = true;
        }
        if (to < leaves.size()) TaskManager.regionLater(block.getLocation(), () -> decay(to, leavesPerTick), 1);
    }

    /**
     * breaks the leaf like the server lets it decay, with its drops and the luck of a forester
     *
     * @param tree          the falling tree the items drop at, null to drop them where the leaf is
     * @param checkDistance false if it is already known that no log is within the reach of the leaf
     * @return true if the leaf has decayed
     */
    private boolean decayLeaf(Block leaf, @Nullable FallingTree tree, boolean checkDistance) {
        if (TaskManager.isFolia() && !TaskManager.owns(leaf)) return false;
        if (leaf.getBlockData() instanceof Leaves data) {
            if (data.isPersistent()) return false;
            if (checkDistance && data.getDistance() < LEAF_DISTANCE) return false;
            LeavesDecayEvent event = new LeavesDecayEvent(leaf);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
        } else {
            // blocks of the setting 'extra-leaves' don't decay on their own: they stay while a log is close
            if (!TreeBlocks.isLeaves(leaf.getType()) || checkDistance && hasLogNearby(leaf)) return false;
        }
        Material lucky = foresterLuck > 0 && ThreadLocalRandom.current().nextDouble(100) < foresterLuck ? luckyDrop() : null;
        if (tree == null) {
            leaf.breakNaturally();
            if (lucky != null) leaf.getWorld().dropItemNaturally(leaf.getLocation().add(0.5, 0.5, 0.5), new ItemStack(lucky));
            return true;
        }
        for (ItemStack item : leaf.getDrops()) tree.drop(leaf, item);
        if (lucky != null) tree.drop(leaf, new ItemStack(lucky));
        leaf.setType(Material.AIR);
        return true;
    }

    /**
     * @return true if a log is at most three blocks away from the block
     */
    private boolean hasLogNearby(Block target) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block next = target.getRelative(x, y, z);
                    if (isInRange(next) && TreeBlocks.isAnyLog(next.getType())) return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the leaves of the cut tree that have no log within their reach any more, so the server would let them
     * decay: the logs of other trees and logs a protection plugin did not let the player break keep their leaves
     */
    private List<Block> crown() {
        if (leaves.isEmpty()) return List.of();
        // a leaf stays if a log is at most 6 leaves away (the leaf next to a log has the distance 1), so the logs
        // that keep leaves of the tree are next to a leaf at most 5 leaves away from them
        Set<Block> keepers = new HashSet<>();
        Map<Block, Integer> steps = new HashMap<>();
        Deque<Block> queue = new ArrayDeque<>();
        for (Block leaf : leaves) {
            steps.put(leaf, 0);
            queue.add(leaf);
        }
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            int step = steps.get(current) + 1;
            for (BlockFace face : FACES) {
                Block next = current.getRelative(face);
                // the log the player has broken is removed by the server after the event
                if (next.equals(block) || steps.containsKey(next) || !isInRange(next)) continue;
                Material type = next.getType();
                if (TreeBlocks.isAnyLog(type)) {
                    keepers.add(next);
                } else if (step < LEAF_DISTANCE - 1 && TreeBlocks.isLeaves(type)) {
                    steps.put(next, step);
                    queue.add(next);
                }
            }
        }

        // the distances of the leaves from these logs, like the server counts them
        Map<Block, Integer> distances = new HashMap<>();
        for (Block log : keepers) {
            for (BlockFace face : FACES) {
                Block next = log.getRelative(face);
                if (!distances.containsKey(next) && isInRange(next) && TreeBlocks.isLeaves(next.getType())) {
                    distances.put(next, 1);
                    queue.add(next);
                }
            }
        }
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            int distance = distances.get(current) + 1;
            if (distance >= LEAF_DISTANCE) continue;
            for (BlockFace face : FACES) {
                Block next = current.getRelative(face);
                if (distances.containsKey(next) || !isInRange(next) || !TreeBlocks.isLeaves(next.getType())) continue;
                distances.put(next, distance);
                queue.add(next);
            }
        }

        List<Block> crown = new ArrayList<>();
        for (Block leaf : leaves) {
            if (!distances.containsKey(leaf)) crown.add(leaf);
        }
        return crown;
    }

}
