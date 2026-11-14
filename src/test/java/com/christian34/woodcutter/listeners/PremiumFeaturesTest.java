package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the features of 3.2.0: drops where a falling tree lands, trees that hit mobs, money of an economy plugin,
 * counted trees, rewards for levels, excluded logs, MiniMessage and the languages.
 *
 * @author Christian34
 */
class PremiumFeaturesTest extends PluginTestBase {
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

    /**
     * a cutter west of the tree, so it falls to the east
     */
    private PlayerMock toppleCutter() {
        setConfig(ConfigData.Keys.ANIMATION, true);
        setConfig(ConfigData.Keys.ANIMATION_MODE, "topple");
        PlayerMock player = addCutter("Cutter");
        player.teleport(new Location(world, X - 3.5, Y, Z + 0.5));
        return player;
    }

    /**
     * the mocked server drops no items when a player breaks a block, so this listener does it like the server: it
     * calls the BlockDropItemEvent and keeps the items the event still contains
     */
    private void dropItemsLikeTheServer() {
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onBreak(BlockBreakEvent e) {
                Block block = e.getBlock();
                Item item = world.dropItem(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(block.getType()));
                List<Item> items = new ArrayList<>(List.of(item));
                server.getPluginManager().callEvent(new BlockDropItemEvent(block, block.getState(), e.getPlayer(), items));
                if (items.isEmpty()) item.remove();
            }
        }, plugin);
    }

    @Test
    void itemsDropWhereTheTreeLands() {
        PlayerMock player = toppleCutter();
        dropItemsLikeTheServer();
        breakBlock(player, tree(X, 8));
        // nothing drops at the trunk while the tree falls
        assertTrue(world.getEntitiesByClass(Item.class).stream().noneMatch(item -> item.getLocation().getX() > X + 2));
        server.getScheduler().performTicks(30);
        List<Item> items = world.getEntitiesByClass(Item.class).stream().filter(item -> item.getItemStack().getType() == Material.OAK_LOG).toList();
        assertFalse(items.isEmpty());
        // the top of the tree lies seven blocks to the east
        assertTrue(items.stream().anyMatch(item -> item.getLocation().getX() > X + 5), () -> items.stream().map(Item::getLocation).toList().toString());
    }

    @Test
    void itemsDropAtTheTrunkIfDisabled() {
        setConfig(ConfigData.Keys.TOPPLE_DROPS_WHERE_IT_LANDS, false);
        PlayerMock player = toppleCutter();
        dropItemsLikeTheServer();
        breakBlock(player, tree(X, 8));
        server.getScheduler().performTicks(30);
        // the log the player has broken as well
        assertEquals(8, world.getEntitiesByClass(Item.class).size());
        assertTrue(world.getEntitiesByClass(Item.class).stream().noneMatch(item -> item.getLocation().getX() > X + 2));
    }

    @Test
    void bonusDropsLandWithTheTree() {
        setConfig(ConfigData.Keys.BONUS_DROPS_CHANCE, 100);
        PlayerMock player = toppleCutter();
        breakBlock(player, tree(X, 8));
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty());
        server.getScheduler().performTicks(30);
        assertEquals(7, world.getEntitiesByClass(Item.class).size());
        assertTrue(world.getEntitiesByClass(Item.class).stream().anyMatch(item -> item.getLocation().getX() > X + 5));
    }

    @Test
    void itemsDropIfThePluginStopsWhileTheTreeFalls() {
        setConfig(ConfigData.Keys.BONUS_DROPS_CHANCE, 100);
        PlayerMock player = toppleCutter();
        breakBlock(player, tree(X, 8));
        server.getPluginManager().disablePlugin(plugin);
        assertEquals(7, world.getEntitiesByClass(Item.class).size());
    }

    @Test
    void fallingTreeHitsMobs() {
        setConfig(ConfigData.Keys.TOPPLE_DAMAGE, 4);
        PlayerMock player = toppleCutter();
        Zombie zombie = world.spawn(new Location(world, X + 4.5, Y, Z + 0.5), Zombie.class);
        double health = zombie.getHealth();
        breakBlock(player, tree(X, 8));
        server.getScheduler().performTicks(30);
        assertEquals(health - 4, zombie.getHealth(), 0.001);
        assertTrue(zombie.getVelocity().getX() > 0);
        // not the player who cut it
        assertEquals(20, player.getHealth(), 0.001);
    }

    @Test
    void treesEarnAndCostMoney() {
        FakeEconomy economy = new FakeEconomy();
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        setConfig(ConfigData.Keys.ECONOMY_REWARD_PER_LOG, 0.5);
        setConfig(ConfigData.Keys.ECONOMY_COSTS, 2);
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");

        // too poor
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));

        economy.balances.put(player.getUniqueId(), 3.0);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(1, logs(X, 6));
        // 3 - 2 + 6 * 0.5
        assertEquals(4, economy.balances.get(player.getUniqueId()), 0.001);
    }

    @Test
    void treesAreCountedAndLevelsGiveRewards() {
        setConfig(ConfigData.Keys.SKILL_XP_FIRST_LEVEL, 5);
        setConfig(ConfigData.Keys.SKILL_REWARDS, List.of("1: woodcutter give %player% book", "2: woodcutter give %player% axe", "x: nothing"));
        setConfig(ConfigData.Keys.DELAY, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        server.getScheduler().performTicks(1);
        assertEquals(1, plugin.getSkill().trees(player.getUniqueId()));
        assertTrue(player.getInventory().contains(Material.ENCHANTED_BOOK));
        assertFalse(player.getInventory().contains(Material.DIAMOND_AXE, 2));

        breakBlock(player, tree(X + 10, 2));
        assertEquals(2, plugin.getSkill().trees(player.getUniqueId()));
        assertEquals(2, plugin.getSkill().find("Cutter").trees());
    }

    @Test
    void treesAreCountedWithoutTheSkill() {
        setConfig(ConfigData.Keys.SKILL, false);
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(1, plugin.getSkill().trees(player.getUniqueId()));
        assertEquals(0, plugin.getSkill().xp(player.getUniqueId()));
    }

    /**
     * a trunk on dirt at the position
     */
    private void trunk(int x, int z, int height) {
        world.getBlockAt(x, Y - 1, z).setType(Material.DIRT);
        for (int y = 0; y < height; y++) world.getBlockAt(x, Y + y, z).setType(Material.OAK_LOG);
    }

    private int column(int x, int z, int height) {
        int logs = 0;
        for (int y = 0; y < height; y++) {
            if (world.getBlockAt(x, Y + y, z).getType() == Material.OAK_LOG) logs++;
        }
        return logs;
    }

    @Test
    void treesNextToTheTreeStay() {
        PlayerMock player = addCutter("Cutter");
        trunk(X, Z, 6);
        // two blocks away diagonally, one block of air between them, and touching diagonally
        trunk(X + 2, Z + 2, 6);
        trunk(X - 2, Z, 6);
        trunk(X - 1, Z - 1, 6);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(1, column(X, Z, 6));
        assertEquals(6, column(X + 2, Z + 2, 6));
        assertEquals(6, column(X - 2, Z, 6));
        assertEquals(6, column(X - 1, Z - 1, 6));
    }

    @Test
    void trunksOfTwoByTwoAndBranchesAreCut() {
        PlayerMock player = addCutter("Cutter");
        trunk(X, Z, 6);
        trunk(X + 1, Z, 6);
        trunk(X, Z + 1, 6);
        trunk(X + 1, Z + 1, 6);
        // a branch with a gap, in the air
        world.getBlockAt(X + 3, Y + 5, Z).setType(Material.OAK_LOG);
        world.getBlockAt(X + 4, Y + 6, Z).setType(Material.OAK_LOG);
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(1, column(X, Z, 6));
        assertEquals(0, column(X + 1, Z, 6) + column(X, Z + 1, 6) + column(X + 1, Z + 1, 6));
        assertEquals(Material.AIR, world.getBlockAt(X + 3, Y + 5, Z).getType());
        assertEquals(Material.AIR, world.getBlockAt(X + 4, Y + 6, Z).getType());
    }

    @Test
    void excludedLogsAreNeverCut() {
        setConfig(ConfigData.Keys.EXCLUDED_LOGS, List.of("OAK_*"));
        PlayerMock player = addCutter("Cutter");
        breakBlock(player, tree(X, 6));
        assertEquals(6, logs(X, 6));

        setConfig(ConfigData.Keys.EXCLUDED_LOGS, List.of("stripped_*"));
        breakBlock(player, world.getBlockAt(X, Y, Z));
        assertEquals(1, logs(X, 6));
    }

    @Test
    void textsWithMiniMessageAndHexColors() {
        assertEquals("§cHi", Message.format("<red>Hi"));
        assertEquals("§x§f§f§8§8§0§0Hi", Message.format("&#ff8800Hi"));
        // help lines with color codes keep their arguments
        assertEquals("§7give <player>", Message.format("&7give <player>"));
        assertEquals("", Message.format(""));
    }

    @Test
    void everyLanguageHasEveryText() throws Exception {
        YamlConfiguration english = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        for (String language : List.of("de", "fr", "es", "pl", "zh")) {
            plugin.getConfigData().set(ConfigData.Keys.LANGUAGE, language);
            plugin.reload();
            File file = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
            assertTrue(file.isFile(), language);
            assertEquals(english.getKeys(true), YamlConfiguration.loadConfiguration(file).getKeys(true), language);
            assertFalse(Message.RELOADED.getText().contains("Config has been reloaded"), language);
        }
    }

    /**
     * an economy plugin, every player starts without money
     */
    public static final class FakeEconomy implements Economy {
        final Map<UUID, Double> balances = new HashMap<>();

        private double balance(OfflinePlayer player) {
            return balances.getOrDefault(player.getUniqueId(), 0.0);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return balance(player) >= amount;
        }

        @Override
        public boolean withdrawPlayer(OfflinePlayer player, double amount) {
            if (!has(player, amount)) return false;
            balances.put(player.getUniqueId(), balance(player) - amount);
            return true;
        }

        @Override
        public boolean depositPlayer(OfflinePlayer player, double amount) {
            balances.put(player.getUniqueId(), balance(player) + amount);
            return true;
        }

        @Override
        public String format(double amount) {
            return "$" + amount;
        }
    }

}
