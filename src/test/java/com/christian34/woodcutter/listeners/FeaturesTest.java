package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.woodcutting.Chopping;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import com.christian34.woodcutter.woodcutting.PlantedSaplings;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.mockbukkit.mockbukkit.attribute.AttributeInstanceMock;
import org.bukkit.entity.Bee;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.data.type.Leaves;
import com.christian34.woodcutter.skill.Skill;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import com.christian34.woodcutter.woodcutting.Cutter;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the optional features: levels, animation, costs, bonus drops, saplings from the inventory, placed logs and
 * wildcards of disabled worlds.
 *
 * @author Christian34
 */
class FeaturesTest extends PluginTestBase {
    private static final int X = 0, Y = 70, Z = 0;

    private Block tree(int x, int height) {
        for (int y = 0; y < height; y++) {
            world.getBlockAt(x, Y + y, Z).setType(Material.OAK_LOG);
        }
        return world.getBlockAt(x, Y, Z);
    }

    private int logs(int x, int height) {
        int logs = 0;
        for (int y = 0; y < height; y++) {
            if (world.getBlockAt(x, Y + y, Z).getType() == Material.OAK_LOG) logs++;
        }
        return logs;
    }

    private void breakBlock(PlayerMock player, Block block) {
        server.getPluginManager().callEvent(new BlockBreakEvent(block, player));
    }

    private void place(PlayerMock player, Block block) {
        block.setType(Material.OAK_LOG);
        server.getPluginManager().callEvent(new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                new ItemStack(Material.OAK_LOG), player, true, EquipmentSlot.HAND));
    }

    @Test
    void higherLevelsCutBiggerTrees() {
        setConfig(ConfigData.Keys.MAX_BLOCKS, 3);
        setConfig(ConfigData.Keys.LEVELS_ADDITIONAL_BLOCKS, 2);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 12));
        assertEquals(12 - 3, logs(X, 12));

        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        WoodCutterEnchantment.apply(axe, 3);
        player.getInventory().setItemInMainHand(axe);
        breakBlock(player, tree(X + 10, 12));
        assertEquals(12 - 7, logs(X + 10, 12));
    }

    @Test
    void animationCutsTheTreeFromTheBottom() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 2);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 7));
        assertEquals(7 - 2, logs(X, 7));
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 1, Z).getType());
        assertEquals(Material.OAK_LOG, world.getBlockAt(X, Y + 6, Z).getType());
        server.getScheduler().performTicks(1);
        assertEquals(7 - 4, logs(X, 7));
        server.getScheduler().performTicks(5);
        assertEquals(1, logs(X, 7));
    }

    @Test
    void animationCutsBranchesFromTheTrunkOutwards() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 1);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 2);
        Block tip = world.getBlockAt(X + 2, Y + 1, Z);
        Block branch = world.getBlockAt(X + 1, Y + 1, Z);
        tip.setType(Material.OAK_LOG);
        branch.setType(Material.OAK_LOG);
        breakBlock(player, root);
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 1, Z).getType());
        assertEquals(Material.OAK_LOG, branch.getType());
        server.getScheduler().performTicks(1);
        assertEquals(Material.AIR, branch.getType());
        assertEquals(Material.OAK_LOG, tip.getType());
        server.getScheduler().performTicks(1);
        assertEquals(Material.AIR, tip.getType());
    }

    @Test
    void animationLetsTheLeavesDecayFromTheBottom() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 1);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 2);
        Leaves data = (Leaves) Material.OAK_LEAVES.createBlockData();
        data.setDistance(7);
        // a column of leaves on top of the tree, four of them decay in a tick
        for (int y = 2; y < 8; y++) world.getBlockAt(X, Y + y, Z).setBlockData(data);
        breakBlock(player, root);
        server.getScheduler().performTicks(39);
        assertEquals(Material.OAK_LEAVES, world.getBlockAt(X, Y + 2, Z).getType());
        server.getScheduler().performTicks(1);
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 5, Z).getType());
        assertEquals(Material.OAK_LEAVES, world.getBlockAt(X, Y + 6, Z).getType());
        server.getScheduler().performTicks(1);
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 7, Z).getType());
    }

    @Test
    void animationStopsWithoutTheAxe() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 1);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 7));
        player.getInventory().setItemInMainHand(null);
        server.getScheduler().performTicks(10);
        assertEquals(7 - 1, logs(X, 7));
    }

    /**
     * an axe that breaks with the given amount of hits - the mocked server does not damage it, so a listener does
     */
    private void almostBrokenAxe(PlayerMock player, int hitsLeft) {
        ItemStack axe = player.getInventory().getItemInMainHand();
        axe.editMeta(Damageable.class, meta -> meta.setDamage(axe.getType().getMaxDurability() - hitsLeft));
        player.getInventory().setItemInMainHand(axe);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onBreak(BlockBreakEvent e) {
                if (!Cutter.isCutting(player)) return;
                ItemStack used = player.getInventory().getItemInMainHand();
                used.editMeta(Damageable.class, meta -> meta.setDamage(meta.getDamage() + 1));
                player.getInventory().setItemInMainHand(used);
            }
        }, plugin);
    }

    private int hitsLeft(PlayerMock player) {
        ItemStack axe = player.getInventory().getItemInMainHand();
        return axe.getType().getMaxDurability() - ((Damageable) axe.getItemMeta()).getDamage();
    }

    @Test
    void cuttingStopsBeforeTheAxeBreaks() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        // five hits: the log the player breaks, three logs of the tree and one that is left for the axe
        almostBrokenAxe(player, 5);
        breakBlock(player, tree(X, 10));
        assertEquals(10 - 3, logs(X, 10));
        assertEquals(2, hitsLeft(player));

        // only the hit for the log of the player and the one for the axe are left
        breakBlock(player, tree(X + 10, 10));
        assertEquals(10, logs(X + 10, 10));
    }

    @Test
    void animationStopsBeforeTheAxeBreaks() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK, 1);
        PlayerMock player = addCutter("Cutter");
        almostBrokenAxe(player, 4);
        breakBlock(player, tree(X, 10));
        server.getScheduler().performTicks(10);
        assertEquals(1, hitsLeft(player));
        assertEquals(10 - 3, logs(X, 10));
    }

    @Test
    void axeProtectionCanBeDisabled() {
        setConfig(ConfigData.Keys.PROTECT_AXE, false);
        PlayerMock player = addCutter("Cutter");
        almostBrokenAxe(player, 3);
        breakBlock(player, tree(X, 10));
        assertEquals(1, logs(X, 10));
    }

    @Test
    void treesDropExperience() {
        setConfig(ConfigData.Keys.EXPERIENCE_DROPS_PER_LOG, 0.5);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        server.getScheduler().performTicks(2);
        List<ExperienceOrb> orbs = List.copyOf(world.getEntitiesByClass(ExperienceOrb.class));
        assertEquals(1, orbs.size());
        assertEquals(3, orbs.get(0).getExperience());

        setConfig(ConfigData.Keys.DROPS_TO_INVENTORY, true);
        breakBlock(player, tree(X + 10, 6));
        server.getScheduler().performTicks(2);
        assertEquals(1, world.getEntitiesByClass(ExperienceOrb.class).size());
        assertEquals(3, player.getTotalExperience());
    }

    @Test
    void logsWithoutLeavesAreNoTree() {
        setConfig(ConfigData.Keys.REQUIRE_LEAVES, true);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        breakBlock(player, root);
        assertEquals(6, logs(X, 6));
        // nothing happened, so there is no delay either
        assertEquals(0, plugin.getDelays().size());

        world.getBlockAt(X, Y + 6, Z).setType(Material.OAK_LEAVES);
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));

        // the trees of the Nether have wart blocks
        for (int y = 0; y < 4; y++) world.getBlockAt(X + 10, Y + y, Z).setType(Material.CRIMSON_STEM);
        world.getBlockAt(X + 11, Y + 3, Z).setType(Material.NETHER_WART_BLOCK);
        breakBlock(player, world.getBlockAt(X + 10, Y, Z));
        assertEquals(Material.AIR, world.getBlockAt(X + 10, Y + 3, Z).getType());
    }

    @Test
    void bigTreesGetFourSaplings() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, true);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                world.getBlockAt(X + x, Y - 1, Z + z).setType(Material.GRASS_BLOCK);
                for (int y = 0; y < 4; y++) world.getBlockAt(X + x, Y + y, Z + z).setType(Material.DARK_OAK_LOG);
            }
        }
        Block root = world.getBlockAt(X + 1, Y, Z);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(3);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                assertEquals(Material.DARK_OAK_SAPLING, world.getBlockAt(X + x, Y, Z + z).getType());
            }
        }
        // no saplings next to a normal tree
        world.getBlockAt(X + 10, Y - 1, Z).setType(Material.GRASS_BLOCK);
        world.getBlockAt(X + 11, Y - 1, Z).setType(Material.GRASS_BLOCK);
        breakBlock(player, tree(X + 10, 4));
        world.getBlockAt(X + 10, Y, Z).setType(Material.AIR);
        server.getScheduler().performTicks(3);
        assertEquals(Material.OAK_SAPLING, world.getBlockAt(X + 10, Y, Z).getType());
        assertEquals(Material.AIR, world.getBlockAt(X + 11, Y, Z).getType());
    }

    @Test
    void hugeMushroomsAreCut() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        for (int y = 0; y < 4; y++) world.getBlockAt(X, Y + y, Z).setType(Material.MUSHROOM_STEM);
        Block cap = world.getBlockAt(X + 1, Y + 4, Z);
        cap.setType(Material.RED_MUSHROOM_BLOCK);
        // a log next to the mushroom does not belong to it
        Block log = world.getBlockAt(X + 1, Y + 1, Z);
        log.setType(Material.OAK_LOG);

        setConfig(ConfigData.Keys.MUSHROOMS, false);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(Material.MUSHROOM_STEM, world.getBlockAt(X, Y + 3, Z).getType());

        setConfig(ConfigData.Keys.MUSHROOMS, true);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 3, Z).getType());
        assertEquals(Material.AIR, cap.getType());
        assertEquals(Material.OAK_LOG, log.getType());
    }

    @Test
    void ranksHaveAShorterDelay() {
        setConfig(ConfigData.Keys.DELAY, true);
        setConfig(ConfigData.Keys.DELAY_TIME, 30);
        PlayerMock player = addCutter("Cutter");
        PlayerMock vip = addCutter("Vip");
        vip.addAttachment(plugin, "woodcutter.delay.10", true);
        vip.addAttachment(plugin, "woodcutter.delay.20", true);
        long now = System.currentTimeMillis();
        plugin.getDelays().put(player.getUniqueId(), now);
        plugin.getDelays().put(vip.getUniqueId(), now);
        assertEquals(30, plugin.remainingDelay(player));
        assertEquals(10, plugin.remainingDelay(vip));
    }

    @Test
    void shorterDelayOfARankDoesNotEndTheDelayOfOthers() {
        setConfig(ConfigData.Keys.DELAY, true);
        setConfig(ConfigData.Keys.DELAY_TIME, 30);
        PlayerMock player = addCutter("Cutter");
        PlayerMock vip = addCutter("Vip");
        vip.addAttachment(plugin, "woodcutter.delay.10", true);
        long before = System.currentTimeMillis() - 15_000;
        plugin.getDelays().put(player.getUniqueId(), before);
        plugin.getDelays().put(vip.getUniqueId(), before);
        assertEquals(0, plugin.remainingDelay(vip));
        assertEquals(15, plugin.remainingDelay(player), 1);
    }

    @Test
    void delayIsShownWhenThePlayerStartsToBreakALog() {
        setConfig(ConfigData.Keys.DELAY, true);
        setConfig(ConfigData.Keys.DELAY_TIME, 30);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 4);
        ItemStack axe = player.getInventory().getItemInMainHand();
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertNull(actionBar(player));

        plugin.getDelays().put(player.getUniqueId(), System.currentTimeMillis());
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertNotNull(actionBar(player));
        // not for blocks that would not cut a tree
        Block stone = world.getBlockAt(X + 5, Y, Z);
        stone.setType(Material.STONE);
        server.getPluginManager().callEvent(new BlockDamageEvent(player, stone, axe, false));
        player.setSneaking(false);
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertNull(actionBar(player));
    }

    @Test
    void treeTipsOver() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_MODE, "topple");
        setConfig(ConfigData.Keys.TOPPLE_PHYSICS, "simple");
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 7));
        // cut at once, a display for every log shows the falling tree
        assertEquals(1, logs(X, 7));
        assertEquals(7, world.getEntitiesByClass(BlockDisplay.class).size());
        // the displays turn while the tree falls
        server.getScheduler().performTicks(12);
        for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) {
            assertNotEquals(new org.joml.Quaternionf(), display.getTransformation().getLeftRotation());
            assertTrue(display.getInterpolationDuration() > 0);
        }
        server.getScheduler().performTicks(40);
        assertEquals(0, world.getEntitiesByClass(BlockDisplay.class).size());
    }

    @Test
    void crownFallsWithTheTree() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_MODE, "topple");
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 7);
        for (int y = 4; y < 7; y++) {
            for (int[] side : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                world.getBlockAt(X + side[0], Y + y, Z + side[1]).setType(Material.OAK_LEAVES);
            }
        }
        world.getBlockAt(X, Y + 7, Z).setType(Material.OAK_LEAVES);
        // the leaves between the tree and another tree, they are kept by its logs
        tree(X + 4, 7);
        world.getBlockAt(X + 2, Y + 5, Z).setType(Material.OAK_LEAVES);
        world.getBlockAt(X + 3, Y + 5, Z).setType(Material.OAK_LEAVES);

        breakBlock(player, root);
        assertEquals(Material.AIR, world.getBlockAt(X - 1, Y + 5, Z).getType());
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 4, Z + 1).getType());
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 7, Z).getType());
        assertEquals(Material.OAK_LEAVES, world.getBlockAt(X + 1, Y + 5, Z).getType());
        assertEquals(Material.OAK_LEAVES, world.getBlockAt(X + 2, Y + 5, Z).getType());
        assertEquals(7, logs(X + 4, 7));
        // 7 logs, 3 leaves on one side, 6 on the others and one on the top
        assertEquals(17, world.getEntitiesByClass(BlockDisplay.class).size());
    }

    @Test
    void leavesDropTheConfiguredItems() {
        setConfig(ConfigData.Keys.SKILL_MAX_LEVEL, 1);
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 1);
        setConfig(ConfigData.Keys.SKILL_PERKS + Skill.PERK_FORESTER_LUCK, 100);
        setConfig(ConfigData.Keys.SKILL_LUCKY_DROPS, List.of("diamond:3", "sapling:0", "nothing:5"));
        PlayerMock player = addCutter("Cutter");
        plugin.getSkill().reward(player, 1);
        Leaves data = (Leaves) Material.OAK_LEAVES.createBlockData();
        data.setDistance(7);
        Block root = tree(X, 3);
        world.getBlockAt(X, Y + 3, Z).setBlockData(data);
        breakBlock(player, root);
        server.getScheduler().performTicks(41);
        assertTrue(world.getEntitiesByClass(Item.class).stream().anyMatch(item -> item.getItemStack().getType() == Material.DIAMOND));
    }

    @Test
    void treesCostHunger() {
        setConfig(ConfigData.Keys.COSTS_HUNGER, 4);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        player.setFoodLevel(5);
        breakBlock(player, tree(X, 6));
        assertEquals(1, logs(X, 6));
        assertEquals(1, player.getFoodLevel());

        breakBlock(player, tree(X + 10, 6));
        assertEquals(6, logs(X + 10, 6));
        assertEquals(1, player.getFoodLevel());
    }

    @Test
    void treesCostExperience() {
        setConfig(ConfigData.Keys.COSTS_EXPERIENCE, 2);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));

        player.setLevel(3);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(1, logs(X, 6));
        assertEquals(1, player.getLevel());
    }

    @Test
    void singleLogsAndCreativeCostNothing() {
        setConfig(ConfigData.Keys.COSTS_HUNGER, 4);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        player.setFoodLevel(10);
        breakBlock(player, tree(X, 1));
        assertEquals(10, player.getFoodLevel());

        player.setGameMode(GameMode.CREATIVE);
        player.setFoodLevel(0);
        breakBlock(player, tree(X + 10, 6));
        assertEquals(1, logs(X + 10, 6));
    }

    @Test
    void bonusDropsGoToTheInventory() {
        setConfig(ConfigData.Keys.BONUS_DROPS_CHANCE, 100);
        setConfig(ConfigData.Keys.DROPS_TO_INVENTORY, true);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        // one bonus log for each of the five logs above the broken one
        assertTrue(player.getInventory().containsAtLeast(new ItemStack(Material.OAK_LOG), 5));
    }

    @Test
    void noBonusDropsByDefault() {
        setConfig(ConfigData.Keys.DROPS_TO_INVENTORY, true);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertFalse(player.getInventory().contains(Material.OAK_LOG));
    }

    @Test
    void saplingIsTakenFromTheInventory() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, false);
        setConfig(ConfigData.Keys.DELAY, false);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        PlayerMock player = addCutter("Cutter");

        Block root = tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.AIR, root.getType());

        player.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 2));
        tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.OAK_SAPLING, root.getType());
        assertTrue(player.getInventory().containsAtLeast(new ItemStack(Material.OAK_SAPLING), 1));
        assertFalse(player.getInventory().containsAtLeast(new ItemStack(Material.OAK_SAPLING), 2));
    }

    @Test
    void buildingsNextToTreesAreKept() {
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block wall = world.getBlockAt(X + 1, Y + 2, Z);
        place(player, wall);
        assertTrue(PlacedLogs.isPlaced(wall));
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));
        assertEquals(Material.OAK_LOG, wall.getType());
    }

    @Test
    void brokenLogsAreForgotten() {
        PlayerMock player = addCutter("Cutter");
        Block wall = world.getBlockAt(X, Y, Z);
        place(player, wall);
        Block other = world.getBlockAt(X + 3, Y, Z);
        place(player, other);
        breakBlock(player, wall);
        assertFalse(PlacedLogs.isPlaced(wall));
        assertTrue(PlacedLogs.isPlaced(other));
    }

    @Test
    void pistonsMovePlacedLogs() {
        PlayerMock player = addCutter("Cutter");
        Block first = world.getBlockAt(X, Y, Z);
        Block second = world.getBlockAt(X + 1, Y, Z);
        place(player, first);
        place(player, second);
        Block piston = world.getBlockAt(X - 1, Y, Z);
        server.getPluginManager().callEvent(new BlockPistonExtendEvent(piston, List.of(first, second), BlockFace.EAST));
        assertFalse(PlacedLogs.isPlaced(first));
        assertTrue(PlacedLogs.isPlaced(second));
        assertTrue(PlacedLogs.isPlaced(world.getBlockAt(X + 2, Y, Z)));

        server.getPluginManager().callEvent(new BlockPistonRetractEvent(piston, List.of(second), BlockFace.WEST));
        assertTrue(PlacedLogs.isPlaced(first));
        assertFalse(PlacedLogs.isPlaced(second));
        assertTrue(PlacedLogs.isPlaced(world.getBlockAt(X + 2, Y, Z)));
    }

    @Test
    void burnedAndExplodedLogsAreForgotten() {
        PlayerMock player = addCutter("Cutter");
        Block burned = world.getBlockAt(X, Y, Z);
        Block exploded = world.getBlockAt(X + 1, Y, Z);
        place(player, burned);
        place(player, exploded);
        server.getPluginManager().callEvent(new BlockBurnEvent(burned, null));
        assertFalse(PlacedLogs.isPlaced(burned));
        // the explosion events have other constructors in each Minecraft version
        BlockPlaceListener.forget(List.of(exploded));
        assertFalse(PlacedLogs.isPlaced(exploded));
    }

    @Test
    void placedLogsOfOlderVersionsAreKept() {
        Block a = world.getBlockAt(X, Y + 5, Z);
        Block b = world.getBlockAt(X + 1, Y, Z + 2);
        // unsorted, like up to 3.1.1
        int[] legacy = {(Y + 5 + 2048) << 8, ((Y + 2048) << 8) | (1 << 4) | 2};
        a.getChunk().getPersistentDataContainer().set(new NamespacedKey(plugin, "placed_logs"), PersistentDataType.INTEGER_ARRAY, legacy);
        assertTrue(PlacedLogs.isPlaced(a));
        assertTrue(PlacedLogs.isPlaced(b));
        assertFalse(a.getChunk().getPersistentDataContainer().has(new NamespacedKey(plugin, "placed_logs")));

        PlacedLogs.mark(world.getBlockAt(X + 3, Y + 1, Z));
        PlacedLogs.unmark(a);
        assertFalse(PlacedLogs.isPlaced(a));
        assertTrue(PlacedLogs.isPlaced(b));
        assertTrue(PlacedLogs.isPlaced(world.getBlockAt(X + 3, Y + 1, Z)));
    }

    @Test
    void disabledWorldsSupportWildcards() {
        assertTrue(BlockBreakListener.matchesWorld("sg_1", "sg_*"));
        assertTrue(BlockBreakListener.matchesWorld("SG_lobby", "sg_*"));
        assertTrue(BlockBreakListener.matchesWorld("sg_", "sg_*"));
        assertFalse(BlockBreakListener.matchesWorld("sg", "sg_*"));
        assertFalse(BlockBreakListener.matchesWorld("my_sg_1", "sg_*"));
        assertTrue(BlockBreakListener.matchesWorld("world_nether", "*_nether"));
        assertTrue(BlockBreakListener.matchesWorld("anything", "*"));
        assertTrue(BlockBreakListener.matchesWorld("Lobby", "lobby"));
        // other characters have no special meaning
        assertFalse(BlockBreakListener.matchesWorld("worldX", "world."));

        PlayerMock player = addCutter("Cutter");
        setConfig(ConfigData.Keys.DELAY, false);
        setConfig(ConfigData.Keys.DISABLED_WORLDS, List.of("wor*"));
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));
        // a changed setting applies at once
        setConfig(ConfigData.Keys.DISABLED_WORLDS, List.of("other"));
        breakBlock(player, tree(X + 5, 6));
        assertEquals(1, logs(X + 5, 6));
    }

    @Test
    void cocoaAndVinesFallWithTheTree() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block cocoa = world.getBlockAt(X + 1, Y + 2, Z);
        cocoa.setType(Material.COCOA);
        for (int y = 1; y < 5; y++) world.getBlockAt(X - 1, Y + y, Z).setType(Material.VINE);
        // vines that hang below the tree, on nothing but the vine above
        for (int y = -2; y <= 0; y++) world.getBlockAt(X, Y + y, Z - 1).setType(Material.VINE);
        Block hanging = world.getBlockAt(X, Y - 2, Z - 1);
        // not next to the tree
        Block other = world.getBlockAt(X + 3, Y + 2, Z);
        other.setType(Material.COCOA);

        breakBlock(player, root);
        assertEquals(Material.AIR, cocoa.getType());
        for (int y = 1; y < 5; y++) assertEquals(Material.AIR, world.getBlockAt(X - 1, Y + y, Z).getType());
        assertEquals(Material.AIR, hanging.getType());
        assertEquals(Material.COCOA, other.getType());
    }

    @Test
    void attachedBlocksCanBeKept() {
        setConfig(ConfigData.Keys.DELAY, false);
        setConfig(ConfigData.Keys.ATTACHED_BLOCKS, List.of());
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block cocoa = world.getBlockAt(X + 1, Y + 2, Z);
        cocoa.setType(Material.COCOA);
        breakBlock(player, root);
        assertEquals(Material.COCOA, cocoa.getType());
    }

    @Test
    void beesOfTheNestAttack() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block nest = world.getBlockAt(X + 1, Y + 3, Z);
        nest.setType(Material.BEE_NEST);
        Bee bee = world.spawn(new Location(world, X + 3.5, Y + 3, Z + 0.5), Bee.class);
        bee.setHive(nest.getLocation());
        Bee stranger = world.spawn(new Location(world, X + 4.5, Y + 3, Z + 0.5), Bee.class);

        breakBlock(player, root);
        assertEquals(Material.AIR, nest.getType());
        assertTrue(bee.getAnger() > 0);
        assertEquals(player, bee.getTarget());
        assertEquals(0, stranger.getAnger());
        // the nest itself does not drop
        assertTrue(world.getEntitiesByClass(Item.class).stream().noneMatch(item -> item.getItemStack().getType() == Material.BEE_NEST));
    }

    @Test
    void beesCanStayCalm() {
        setConfig(ConfigData.Keys.DELAY, false);
        setConfig(ConfigData.Keys.BEES_ANGRY, false);
        setConfig(ConfigData.Keys.BEES_DROP_NEST, true);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block nest = world.getBlockAt(X + 1, Y + 3, Z);
        nest.setType(Material.BEE_NEST);
        Bee bee = world.spawn(new Location(world, X + 3.5, Y + 3, Z + 0.5), Bee.class);
        bee.setHive(nest.getLocation());
        breakBlock(player, root);
        assertEquals(0, bee.getAnger());
        assertTrue(world.getEntitiesByClass(Item.class).stream().anyMatch(item -> item.getItemStack().getType() == Material.BEE_NEST));
    }

    @Test
    void plantedSaplingsAreProtected() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, true);
        setConfig(ConfigData.Keys.REPLANT_PROTECT, true);
        setConfig(ConfigData.Keys.DELAY, false);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.OAK_SAPLING, root.getType());
        assertTrue(PlantedSaplings.isPlanted(root));

        PlayerMock other = addPlayer("Other");
        BlockBreakEvent event = new BlockBreakEvent(root, other);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertNotNull(actionBar(other));
        // players with the permission (and in creative mode) can
        other.addAttachment(plugin, BlockBreakListener.PERMISSION_REPLANT_BREAK, true);
        event = new BlockBreakEvent(root, other);
        server.getPluginManager().callEvent(event);
        assertFalse(event.isCancelled());
        assertFalse(PlantedSaplings.isPlanted(root));
    }

    @Test
    void saplingsAreNotProtectedByDefault() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, true);
        setConfig(ConfigData.Keys.DELAY, false);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        breakBlock(player, root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.OAK_SAPLING, root.getType());
        BlockBreakEvent event = new BlockBreakEvent(root, addPlayer("Other"));
        server.getPluginManager().callEvent(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void saplingIsPlantedNextToATakenStump() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, true);
        setConfig(ConfigData.Keys.DELAY, false);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        world.getBlockAt(X + 1, Y - 1, Z).setType(Material.GRASS_BLOCK);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        breakBlock(player, root);
        // something is on the stump
        root.setType(Material.COBBLESTONE);
        server.getScheduler().performTicks(2);
        assertEquals(Material.COBBLESTONE, root.getType());
        assertEquals(Material.OAK_SAPLING, world.getBlockAt(X + 1, Y, Z).getType());
    }

    @Test
    void biggerTreesTakeLongerToChop() {
        setConfig(ConfigData.Keys.DELAY, false);
        setConfig(ConfigData.Keys.CHOPPING_SLOWDOWN, 5);
        Attribute attribute = Chopping.breakSpeed();
        assertNotNull(attribute);
        // the mocked player has no such attribute
        PlayerMock player = new PlayerMock(server, "Cutter") {
            {
                attributes.putIfAbsent(attribute, new AttributeInstanceMock(attribute, 1));
            }
        };
        server.addPlayer(player);
        player.addAttachment(plugin, "woodcutter.use", true);
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(enchantedAxe());
        Block root = tree(X, 10);
        ItemStack axe = player.getInventory().getItemInMainHand();
        AttributeInstance speed = player.getAttribute(attribute);
        assertNotNull(speed);
        double before = speed.getValue();

        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertTrue(plugin.getChopping().isChopping(player));
        // 10 logs with 5 % each: it takes 1.5 times as long
        assertEquals(before / 1.5, speed.getValue(), 0.001);
        server.getPluginManager().callEvent(new BlockDamageAbortEvent(player, root, axe));
        assertFalse(plugin.getChopping().isChopping(player));
        assertEquals(before, speed.getValue(), 0.001);

        // breaking the log ends it as well
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertTrue(plugin.getChopping().isChopping(player));
        breakBlock(player, root);
        assertFalse(plugin.getChopping().isChopping(player));
        assertEquals(before, speed.getValue(), 0.001);
    }

    @Test
    void choppingIsOffByDefault() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 10);
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, player.getInventory().getItemInMainHand(), false));
        assertFalse(plugin.getChopping().isChopping(player));
    }

    @Test
    void onlyTheConfiguredItemsAreAxes() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        assertTrue(WoodCutterEnchantment.isAxe(new ItemStack(Material.DIAMOND_AXE)));
        assertFalse(WoodCutterEnchantment.isAxe(new ItemStack(Material.MACE)));

        setConfig(ConfigData.Keys.CUSTOM_AXES, List.of("minecraft:mace"));
        assertTrue(WoodCutterEnchantment.isAxe(new ItemStack(Material.MACE)));
        assertTrue(WoodCutterEnchantment.isAxe(new ItemStack(Material.DIAMOND_AXE)));

        setConfig(ConfigData.Keys.CUSTOM_AXES_ONLY, true);
        assertFalse(WoodCutterEnchantment.isAxe(new ItemStack(Material.DIAMOND_AXE)));
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));

        ItemStack mace = new ItemStack(Material.MACE);
        assertTrue(WoodCutterEnchantment.apply(mace));
        player.getInventory().setItemInMainHand(mace);
        breakBlock(player, tree(X + 5, 6));
        assertEquals(1, logs(X + 5, 6));
    }

    @Test
    void extraLogsAreCut() {
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        for (int y = 0; y < 5; y++) world.getBlockAt(X, Y + y, Z).setType(Material.BONE_BLOCK);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(Material.BONE_BLOCK, world.getBlockAt(X, Y + 4, Z).getType());

        setConfig(ConfigData.Keys.EXTRA_LOGS, List.of("bone_block"));
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(Material.AIR, world.getBlockAt(X, Y + 4, Z).getType());
    }

}
