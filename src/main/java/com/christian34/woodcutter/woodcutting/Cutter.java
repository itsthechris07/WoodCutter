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
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
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
    private final List<String> luckyDrops;
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
        this.luckyDrops = config.getList(ConfigData.Keys.SKILL_LUCKY_DROPS);
        this.animated = config.getBoolean(ConfigData.Keys.ANIMATION);
        this.topple = animated && config.getString(ConfigData.Keys.ANIMATION_MODE).equalsIgnoreCase("topple");
        this.blocksPerTick = !animated || topple ? 0
                : Math.max(1, config.getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK)) + (int) Math.round(skill.perk(player, Skill.PERK_SPEED));
    }

    /**
     * @return true if a tree is cut when a player breaks the block: a log or the stem of a huge mushroom
     */
    public static boolean isTrunk(Material type, ConfigData config) {
        return Tag.LOGS.isTagged(type) || type == Material.MUSHROOM_STEM && config.getBoolean(ConfigData.Keys.MUSHROOMS);
    }

    /**
     * @return true for the blocks trees and huge mushrooms are made of, which are remembered when players place them
     */
    public static boolean isWood(Material type) {
        return Tag.LOGS.isTagged(type) || MUSHROOM.contains(type);
    }

    /**
     * @return true if the block belongs to the tree: a log, or a block of the huge mushroom
     */
    private boolean isLog(Material type) {
        return mushroom ? MUSHROOM.contains(type) : Tag.LOGS.isTagged(type);
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
                        if (!isInRange(next) || !visited.add(next) || !isLog(next.getType())) continue;
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
        // the server removes the broken log and drops its items after the event
        ORIGINS.put(player.getUniqueId(), block);
        TaskManager.regionLater(block.getLocation(), () -> {
            ORIGINS.remove(player.getUniqueId(), block);
            if (sapling != null) plant(block);
        }, 1);

        if (topple) {
            FallingTree tree = new FallingTree(block, player);
            tree.add(block, block.getBlockData());
            this.fallingTree = tree;
            breakLogs(0, logs.size());
            tree.start();
            finish();
        } else if (blocksPerTick == 0) {
            breakLogs(0, logs.size());
            finish();
        } else {
            // layer by layer, every layer from the trunk to the tips of its branches
            logs.sort(Comparator.comparingInt(Block::getY).thenComparingInt(this::distanceToTrunk));
            step(0);
        }
        return logs.size();
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
            if (Tag.LEAVES.isTagged(type) || CROWNS.contains(type.name())) return true;
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
                if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(target)) continue;
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
        // by its name, as the sounds became a registry in 1.21.3
        player.playSound(center, "minecraft:block.wood.break", 1, 0.7f + 0.5f * progress);
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
        if (experiencePerLog > 0) TaskManager.regionLater(block.getLocation(), this::dropExperience, 1);
        // the other saplings of a big tree, when its logs are gone
        if (!trunk.isEmpty()) TaskManager.regionLater(block.getLocation(), () -> trunk.forEach(this::plant), 2);
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
        if (dropsToInventory && player.isOnline() && Bukkit.isOwnedByCurrentRegion(player)) {
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
     * plants the sapling as the player: protection plugins can deny it, logging plugins record it
     */
    private void plant(Block block) {
        Block ground = block.getRelative(BlockFace.DOWN);
        if (!player.isOnline() || !block.getType().isAir() || !isGround(sapling, ground)) return;
        if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(block)) return;
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
     * @return what a leaf drops with the luck of a forester: one of the items of the config (item:weight, 'sapling'
     * is the sapling of the tree), null for nothing
     */
    @Nullable
    private Material luckyDrop() {
        Map<Material, Integer> weights = new HashMap<>();
        int total = 0;
        for (String entry : luckyDrops) {
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
            total += weight;
        }
        if (total == 0) return null;
        int random = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
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
        for (Block leaf : leaves.subList(from, to)) {
            if (TaskManager.isFolia() && !Bukkit.isOwnedByCurrentRegion(leaf)) continue;
            if (!(leaf.getBlockData() instanceof Leaves data) || data.isPersistent() || data.getDistance() < LEAF_DISTANCE) {
                continue;
            }
            LeavesDecayEvent event = new LeavesDecayEvent(leaf);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            leaf.breakNaturally();
            Material lucky = foresterLuck > 0 && ThreadLocalRandom.current().nextDouble(100) < foresterLuck ? luckyDrop() : null;
            if (lucky != null) leaf.getWorld().dropItemNaturally(leaf.getLocation().add(0.5, 0.5, 0.5), new ItemStack(lucky));
            if (!animated) continue;
            Location center = leaf.getLocation().add(0.5, 0.5, 0.5);
            leaf.getWorld().spawnParticle(Particle.BLOCK, center, 6, 0.3, 0.3, 0.3, data);
            // one sound for the leaves of a tick
            if (!rustled) leaf.getWorld().playSound(center, "minecraft:block.grass.break", 0.6f, 1);
            rustled = true;
        }
        if (to < leaves.size()) TaskManager.regionLater(block.getLocation(), () -> decay(to, leavesPerTick), 1);
    }

}
