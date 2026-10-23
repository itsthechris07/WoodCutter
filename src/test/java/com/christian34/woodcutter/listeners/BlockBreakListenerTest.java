package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.events.WoodCutterEvent;
import com.christian34.woodcutter.files.ConfigData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests cutting trees: a trunk of logs is built in the mocked world and its lowest log is broken.
 *
 * @author Christian34
 */
class BlockBreakListenerTest extends PluginTestBase {
    private static final int X = 0, Y = 70, Z = 0;

    /**
     * builds a trunk and returns its lowest log
     */
    private Block tree(int x, int height) {
        for (int y = 0; y < height; y++) {
            world.getBlockAt(x, Y + y, Z).setType(Material.OAK_LOG);
        }
        return world.getBlockAt(x, Y, Z);
    }

    /**
     * @return the logs that are left of the trunk at x (the broken block is removed by the server, not by the event)
     */
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

    @Test
    void playersAreToldOnceToSneak() {
        PlayerMock player = addCutter("Cutter");
        player.setSneaking(false);
        breakBlock(player, tree(X, 6));
        // just the log is broken
        assertEquals(6, logs(X, 6));
        Component hint = player.nextActionBar();
        assertNotNull(hint);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(hint).contains("Sneak"));
        breakBlock(player, world.getBlockAt(X, Y + 1, Z));
        assertNull(player.nextActionBar());
    }

    @Test
    void noSneakHintWithoutTheEnchantment() {
        PlayerMock player = addPlayer("Player", "woodcutter.use");
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
        breakBlock(player, tree(X, 6));
        assertNull(player.nextActionBar());
    }

    @Test
    void cutsTheWholeTree() {
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(1, logs(X, 6));
    }

    @Test
    void cutsBranchesWithGaps() {
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 3);
        world.getBlockAt(X + 2, Y + 4, Z + 2).setType(Material.ACACIA_LOG);
        breakBlock(player, root);
        assertEquals(Material.AIR, world.getBlockAt(X + 2, Y + 4, Z + 2).getType());
    }

    @Test
    void logsBelowAreKept() {
        PlayerMock player = addCutter("Cutter");
        tree(X, 6);
        breakBlock(player, world.getBlockAt(X, Y + 3, Z));
        // the three logs below and the broken one
        assertEquals(4, logs(X, 6));
    }

    @Test
    void needsEnchantedAxe() {
        PlayerMock player = addCutter("Cutter");
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));
    }

    @Test
    void needsPermission() {
        // everyone has the permission by default, so the plugin works without a permission plugin
        PlayerMock player = addCutter("Guest");
        player.addAttachment(plugin, "woodcutter.use", false);
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));
    }

    @Test
    void goodieNeedsNoEnchantment() {
        PlayerMock player = addCutter("Cutter");
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
        Block root = tree(X, 6);

        setConfig(ConfigData.Keys.GOODIE, "permission");
        breakBlock(player, root);
        assertEquals(6, logs(X, 6));
        player.addAttachment(plugin, "woodcutter.goodie", true);
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));
    }

    @Test
    void goodieForEveryone() {
        setConfig(ConfigData.Keys.GOODIE, "everyone");
        PlayerMock player = addCutter("Cutter");
        player.getInventory().setItemInMainHand(new ItemStack(Material.WOODEN_AXE));
        breakBlock(player, tree(X, 6));
        assertEquals(1, logs(X, 6));

        // but only with an axe
        setConfig(ConfigData.Keys.DELAY, false);
        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        breakBlock(player, tree(X + 10, 6));
        assertEquals(6, logs(X + 10, 6));
    }

    @Test
    void playersCanSwitchItOff() {
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        player.performCommand("woodcutter toggle");
        breakBlock(player, root);
        assertEquals(6, logs(X, 6));
        player.performCommand("woodcutter toggle");
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));
    }

    @Test
    void permissionRaisesMaxBlocks() {
        setConfig(ConfigData.Keys.MAX_BLOCKS, 3);
        PlayerMock player = addCutter("Cutter");
        player.addAttachment(plugin, "woodcutter.maxblocks.5", true);
        player.addAttachment(plugin, "woodcutter.maxblocks.8", true);
        breakBlock(player, tree(X, 12));
        assertEquals(12 - 8, logs(X, 12));
    }

    @Test
    void protectedLogsAreKept() {
        // a protection plugin denies one log of the tree
        Block root = tree(X, 6);
        Block denied = world.getBlockAt(X, Y + 2, Z);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onBreak(BlockBreakEvent e) {
                if (e.getBlock().equals(denied)) e.setCancelled(true);
            }
        }, plugin);
        breakBlock(addCutter("Cutter"), root);
        assertEquals(Material.OAK_LOG, denied.getType());
        assertEquals(2, logs(X, 6));
    }

    @Test
    void everyLogIsBrokenByThePlayer() {
        // logging plugins (e.g. CoreProtect) record the logs with the player
        List<Block> broken = new ArrayList<>();
        PlayerMock player = addCutter("Cutter");
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onBreak(BlockBreakEvent e) {
                if (e.getPlayer().equals(player)) broken.add(e.getBlock());
            }
        }, plugin);
        breakBlock(player, tree(X, 6));
        assertEquals(6, broken.size());
    }

    @Test
    void replantsSapling() {
        setConfig(ConfigData.Keys.REPLANT, true);
        setConfig(ConfigData.Keys.REPLANT_FREE, true);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        Block root = tree(X, 6);
        breakBlock(addCutter("Cutter"), root);
        // the server removes the broken log
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.OAK_SAPLING, root.getType());
    }

    @Test
    void noSaplingOnOtherGround() {
        setConfig(ConfigData.Keys.REPLANT, true);
        world.getBlockAt(X, Y - 1, Z).setType(Material.STONE);
        Block root = tree(X, 6);
        breakBlock(addCutter("Cutter"), root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.AIR, root.getType());
    }

    @Test
    void protectedSaplingIsRemoved() {
        setConfig(ConfigData.Keys.REPLANT, true);
        world.getBlockAt(X, Y - 1, Z).setType(Material.GRASS_BLOCK);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlace(BlockPlaceEvent e) {
                e.setCancelled(true);
            }
        }, plugin);
        Block root = tree(X, 6);
        breakBlock(addCutter("Cutter"), root);
        root.setType(Material.AIR);
        server.getScheduler().performTicks(2);
        assertEquals(Material.AIR, root.getType());
    }

    @Test
    void leavesDecay() {
        Block root = tree(X, 4);
        Block leaf = world.getBlockAt(X, Y + 4, Z);
        Block farLeaf = world.getBlockAt(X + 1, Y + 4, Z);
        Block placedLeaf = world.getBlockAt(X, Y + 5, Z);
        leaf.setBlockData(leaves(false));
        farLeaf.setBlockData(leaves(false));
        placedLeaf.setBlockData(leaves(true));
        breakBlock(addCutter("Cutter"), root);
        assertEquals(Material.OAK_LEAVES, leaf.getType());
        server.getScheduler().performTicks(41);
        assertEquals(Material.AIR, leaf.getType());
        assertEquals(Material.AIR, farLeaf.getType());
        assertEquals(Material.OAK_LEAVES, placedLeaf.getType());
    }

    @Test
    void leavesStayIfDisabled() {
        setConfig(ConfigData.Keys.FAST_LEAF_DECAY, false);
        Block root = tree(X, 4);
        Block leaf = world.getBlockAt(X, Y + 4, Z);
        leaf.setBlockData(leaves(false));
        breakBlock(addCutter("Cutter"), root);
        server.getScheduler().performTicks(101);
        assertEquals(Material.OAK_LEAVES, leaf.getType());
    }

    /**
     * leaves without a log nearby (the mocked server does not update the distance itself)
     */
    private static Leaves leaves(boolean persistent) {
        Leaves data = (Leaves) Material.OAK_LEAVES.createBlockData();
        data.setDistance(7);
        data.setPersistent(persistent);
        return data;
    }

    @Test
    void needsSneakingIfConfigured() {
        PlayerMock player = addCutter("Cutter");
        player.setSneaking(false);
        Block root = tree(X, 6);
        breakBlock(player, root);
        assertEquals(6, logs(X, 6));

        setConfig(ConfigData.Keys.REQUIRE_SNEAKING, false);
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));
    }

    @Test
    void disabledWorldsAreIgnored() {
        setConfig(ConfigData.Keys.DISABLED_WORLDS, java.util.List.of("WORLD"));
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));
    }

    @Test
    void cancelledBreaksAreIgnored() {
        PlayerMock player = addCutter("Cutter");
        BlockBreakEvent event = new BlockBreakEvent(tree(X, 6), player);
        event.setCancelled(true);
        server.getPluginManager().callEvent(event);
        assertEquals(6, logs(X, 6));
    }

    @Test
    void maxBlocksLimitsTheTree() {
        setConfig(ConfigData.Keys.MAX_BLOCKS, 3);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 10));
        assertEquals(10 - 3, logs(X, 10));
    }

    @Test
    void delayBlocksTheNextTree() {
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        breakBlock(player, tree(X + 10, 6));
        assertEquals(6, logs(X + 10, 6));
        assertTrue(plugin.getDelays().containsKey(player.getUniqueId()));

        setConfig(ConfigData.Keys.DELAY, false);
        breakBlock(player, world.getBlockAt(X + 10, Y, Z));
        assertEquals(1, logs(X + 10, 6));
    }

    @Test
    void permissionBypassesTheDelay() {
        PlayerMock player = addCutter("Cutter");
        player.addAttachment(plugin, "woodcutter.bypass.delay", true);
        breakBlock(player, tree(X, 6));
        breakBlock(player, tree(X + 10, 6));
        assertEquals(1, logs(X + 10, 6));
        assertTrue(plugin.getDelays().isEmpty());
    }

    @Test
    void operatorsHaveTheDelay() {
        // the permission is optional: nobody has it unless it is given
        PlayerMock player = addCutter("Cutter");
        player.setOp(true);
        breakBlock(player, tree(X, 6));
        breakBlock(player, tree(X + 10, 6));
        assertEquals(6, logs(X + 10, 6));
    }

    @Test
    void singleLogsCauseNoDelay() {
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 1));
        assertTrue(plugin.getDelays().isEmpty());
    }

    @Test
    void placedLogsAreNoTree() {
        PlayerMock player = addCutter("Cutter");
        Block root = tree(X, 6);
        Block against = world.getBlockAt(X, Y - 1, Z);
        server.getPluginManager().callEvent(new BlockPlaceEvent(root, root.getState(), against, new ItemStack(Material.OAK_LOG), player, true, EquipmentSlot.HAND));
        breakBlock(player, root);
        assertEquals(6, logs(X, 6));

        setConfig(ConfigData.Keys.DESTROY_FAKE_TREES, true);
        breakBlock(player, root);
        assertEquals(1, logs(X, 6));
    }

    @Test
    void otherPluginsCanCancel() {
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onCut(WoodCutterEvent e) {
                e.setCancelled(true);
            }
        }, plugin);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));
    }

    @Test
    void stopsWhenTheAxeBreaks() {
        // the server damages the axe for every log (the mocked one does not), here it breaks at the third log
        PlayerMock player = addCutter("Cutter");
        Block third = world.getBlockAt(X, Y + 3, Z);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onBreak(BlockBreakEvent e) {
                if (e.getBlock().equals(third)) player.getInventory().setItemInMainHand(null);
            }
        }, plugin);
        breakBlock(player, tree(X, 8));
        assertEquals(8 - 3, logs(X, 8));
    }

    @Test
    void creativeKeepsTheAxe() {
        PlayerMock player = addCutter("Cutter");
        player.setGameMode(GameMode.CREATIVE);
        breakBlock(player, tree(X, 6));
        assertEquals(1, logs(X, 6));
        Damageable meta = (Damageable) player.getInventory().getItemInMainHand().getItemMeta();
        assertEquals(0, meta.getDamage());
    }

}
