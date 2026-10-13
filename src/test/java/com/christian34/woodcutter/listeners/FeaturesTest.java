package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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

        setConfig(ConfigData.Keys.DISABLED_WORLDS, List.of("wor*"));
        breakBlock(addCutter("Cutter"), tree(X, 6));
        assertEquals(6, logs(X, 6));
    }

}
