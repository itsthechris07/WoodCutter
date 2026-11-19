package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the settings of the falling trees, the sounds, chopping and the blocks of trees.
 *
 * @author Christian34
 */
class SettingsTest extends PluginTestBase {

    @Test
    void soundsAreRead() {
        assertEquals(new Effects.Sound("minecraft:block.wood.break", 1.5f, 0.6f), Effects.parse("minecraft:block.wood.break 1.5 0.6"));
        assertEquals(new Effects.Sound("block.wood.break", 1, 1), Effects.parse("  Block.Wood.Break "));
        assertEquals(new Effects.Sound("block.wood.break", 2, 1), Effects.parse("block.wood.break 2"));
        assertNull(Effects.parse(""));
        assertNull(Effects.parse("block.wood.break loud"));
    }

    @Test
    void physicsDirectionAndGlowAreRead() {
        assertEquals(FallingTree.Physics.SIMPLE, FallingTree.Physics.of("Simple"));
        assertEquals(FallingTree.Physics.FALL_APART, FallingTree.Physics.of("fall_apart"));
        assertEquals(FallingTree.Physics.RIGID, FallingTree.Physics.of("anything"));
        assertEquals(FallingTree.Direction.SLOPE, FallingTree.Direction.of("downhill"));
        assertEquals(FallingTree.Direction.FACING, FallingTree.Direction.of("FACING"));
        assertEquals(FallingTree.Direction.AWAY, FallingTree.Direction.of(""));

        assertNull(FallingTree.Settings.color(""));
        assertNull(FallingTree.Settings.color("false"));
        assertNull(FallingTree.Settings.color("#12345"));
        assertNull(FallingTree.Settings.color("#gggggg"));
        assertEquals(Color.WHITE, FallingTree.Settings.color("true"));
        assertEquals(Color.fromRGB(0xff8800), FallingTree.Settings.color("#FF8800"));

        setConfig(ConfigData.Keys.TOPPLE_PHYSICS, "fall-apart");
        setConfig(ConfigData.Keys.TOPPLE_UPDATE_INTERVAL, 50);
        setConfig(ConfigData.Keys.TOPPLE_GLOW, "#00ff00");
        FallingTree.Settings settings = FallingTree.Settings.of(plugin.getConfigData());
        assertEquals(FallingTree.Physics.FALL_APART, settings.physics());
        assertEquals(10, settings.updateInterval());
        assertEquals(Color.LIME, settings.glow());
    }

    @Test
    void treesFallDownTheHill() {
        int y = 70;
        // the hill goes down to the north (negative z), the ground of the other sides is higher
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 20; z++) {
                world.getBlockAt(x, z < 0 ? y - 6 : y - 1, z).setType(Material.STONE);
            }
        }
        PlayerMock player = addPlayer("Cutter");
        // north of the tree, away from the player would be up the hill
        player.teleport(new Location(world, 0.5, y, -3.5));
        Vector direction = FallingTree.direction(FallingTree.Direction.SLOPE, world.getBlockAt(0, y, 0), player, 8);
        assertTrue(direction.getZ() < -0.5, direction::toString);
        // away from the player without the setting
        assertEquals(1, FallingTree.direction(FallingTree.Direction.AWAY, world.getBlockAt(0, y, 0), player, 8).getZ(), 0.001);

        // on even ground, away from the player
        player.teleport(new Location(world, -3.5, y, 8.5));
        Vector away = FallingTree.direction(FallingTree.Direction.SLOPE, world.getBlockAt(0, y, 8), player, 8);
        assertEquals(1, away.getX(), 0.001);
    }

    @Test
    void biggerTreesTakeLongerToChop() {
        assertEquals(1, Chopping.factor(20, 0, 6));
        assertEquals(2, Chopping.factor(20, 5, 6), 0.001);
        assertEquals(6, Chopping.factor(500, 5, 6), 0.001);
        // a maximum below 1 never makes it faster
        assertEquals(1, Chopping.factor(20, 5, 0.5), 0.001);
        assertEquals("§a" + "|".repeat(10) + "§8" + "|".repeat(10), Chopping.bar(0.5f));
    }

    @Test
    void extraBlocksAndSaplings() {
        assertFalse(TreeBlocks.isLog(Material.BONE_BLOCK));
        setConfig(ConfigData.Keys.EXTRA_LOGS, List.of("bone_block", "*_wood"));
        assertTrue(TreeBlocks.isLog(Material.BONE_BLOCK));
        assertTrue(TreeBlocks.isLog(Material.OAK_WOOD));
        assertTrue(TreeBlocks.isLog(Material.OAK_LOG));
        setConfig(ConfigData.Keys.EXCLUDED_LOGS, List.of("bone_block"));
        assertFalse(TreeBlocks.isLog(Material.BONE_BLOCK));
        assertTrue(TreeBlocks.isAnyLog(Material.BONE_BLOCK));

        setConfig(ConfigData.Keys.EXTRA_LEAVES, List.of("minecraft:Nether_Wart_Block"));
        assertTrue(TreeBlocks.isLeaves(Material.NETHER_WART_BLOCK));
        assertTrue(TreeBlocks.isAttached(Material.COCOA));

        assertEquals(Material.OAK_SAPLING, TreeBlocks.sapling(Material.OAK_LOG));
        assertEquals(Material.CRIMSON_FUNGUS, TreeBlocks.sapling(Material.CRIMSON_STEM));
        assertNull(TreeBlocks.sapling(Material.BONE_BLOCK));
        plugin.getConfig().createSection("config." + ConfigData.Keys.SAPLINGS, Map.of("bone_block", "dead_bush", "oak_log", "birch_sapling"));
        assertEquals(Material.DEAD_BUSH, TreeBlocks.sapling(Material.BONE_BLOCK));
        assertEquals(Material.BIRCH_SAPLING, TreeBlocks.sapling(Material.OAK_LOG));
    }

}
