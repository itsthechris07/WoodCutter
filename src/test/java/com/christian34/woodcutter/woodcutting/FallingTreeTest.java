package com.christian34.woodcutter.woodcutting;

import com.christian34.woodcutter.PluginTestBase;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WoodCutter 2026.
 * <p>
 * Tests where a tree that tips over lands.
 *
 * @author Christian34
 */
class FallingTreeTest extends PluginTestBase {

    @Test
    void fallsAwayFromThePlayer() {
        PlayerMock player = addPlayer("Cutter");
        // west of the tree, so it falls to the east
        player.teleport(new Location(world, -3.5, 70, 0.5));
        FallingTree tree = new FallingTree(world.getBlockAt(0, 70, 0), player);

        // the log five blocks above the stump lies five blocks next to it
        Location landing = tree.landing(new Location(world, 0, 75, 0));
        assertEquals(6, landing.getX(), 0.001);
        assertEquals(70, landing.getY(), 0.001);
        assertEquals(0.5, landing.getZ(), 0.001);

        // a branch to the east ends up below the ground level of the stump
        landing = tree.landing(new Location(world, 2, 73, 0));
        assertEquals(4, landing.getX(), 0.001);
        assertEquals(68, landing.getY(), 0.001);
    }

    @Test
    void fallsWhereThePlayerLooksFromTheStump() {
        PlayerMock player = addPlayer("Cutter");
        Location onStump = new Location(world, 0.5, 71, 0.5);
        // yaw 180 looks to the north (negative z)
        onStump.setYaw(180);
        player.teleport(onStump);
        FallingTree tree = new FallingTree(world.getBlockAt(0, 70, 0), player);
        Location landing = tree.landing(new Location(world, 0, 75, 0));
        assertEquals(0.5, landing.getX(), 0.001);
        assertEquals(-5, landing.getZ(), 0.001);
    }

}
