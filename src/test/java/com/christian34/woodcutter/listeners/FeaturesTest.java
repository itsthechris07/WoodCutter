package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
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
        assertNull(player.nextActionBar());

        plugin.getDelays().put(player.getUniqueId(), System.currentTimeMillis());
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertNotNull(player.nextActionBar());
        // not for blocks that would not cut a tree
        Block stone = world.getBlockAt(X + 5, Y, Z);
        stone.setType(Material.STONE);
        server.getPluginManager().callEvent(new BlockDamageEvent(player, stone, axe, false));
        player.setSneaking(false);
        server.getPluginManager().callEvent(new BlockDamageEvent(player, root, axe, false));
        assertNull(player.nextActionBar());
    }

    @Test
    void treeTipsOver() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_MODE, "topple");
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 7));
        // cut at once, a display for every log shows the falling tree
        assertEquals(1, logs(X, 7));
        assertEquals(7, world.getEntitiesByClass(BlockDisplay.class).size());
        server.getScheduler().performTicks(40);
        assertEquals(0, world.getEntitiesByClass(BlockDisplay.class).size());
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

}
