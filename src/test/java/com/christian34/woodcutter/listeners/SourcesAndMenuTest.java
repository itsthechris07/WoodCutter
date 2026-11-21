package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.menu.Menu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the books of librarians and chests and the menu.
 *
 * @author Christian34
 */
class SourcesAndMenuTest extends PluginTestBase {

    private MerchantRecipe acquire(Villager.Profession profession) {
        Villager villager = world.spawn(new Location(world, 0, 70, 0), Villager.class);
        villager.setProfession(profession);
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.BOOKSHELF), 12);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 9));
        VillagerAcquireTradeEvent event = new VillagerAcquireTradeEvent(villager, recipe);
        server.getPluginManager().callEvent(event);
        return event.getRecipe();
    }

    @Test
    void librariansSellTheBook() {
        setConfig(ConfigData.Keys.SOURCES_LIBRARIAN, 100);
        setConfig(ConfigData.Keys.SOURCES_PRICE, 20);
        setConfig(ConfigData.Keys.LEVELS_MAX, 1);
        MerchantRecipe recipe = acquire(Villager.Profession.LIBRARIAN);
        assertEquals(Material.ENCHANTED_BOOK, recipe.getResult().getType());
        assertTrue(WoodCutterEnchantment.has(recipe.getResult()));
        assertEquals(new ItemStack(Material.EMERALD, 20), recipe.getIngredients().get(0));
        assertEquals(new ItemStack(Material.BOOK), recipe.getIngredients().get(1));

        // other villagers keep their trades
        assertEquals(Material.BOOKSHELF, acquire(Villager.Profession.FARMER).getResult().getType());
        setConfig(ConfigData.Keys.SOURCES_LIBRARIAN, 0);
        assertEquals(Material.BOOKSHELF, acquire(Villager.Profession.LIBRARIAN).getResult().getType());
    }

    @Test
    void higherLevelsCostMore() {
        setConfig(ConfigData.Keys.SOURCES_PRICE, 20);
        SourcesListener listener = new SourcesListener(plugin);
        assertEquals(40, listener.trade(2).getIngredients().get(0).getAmount());
        assertEquals(2, WoodCutterEnchantment.level(listener.trade(2).getResult()));
        // not more than a stack
        setConfig(ConfigData.Keys.SOURCES_PRICE, 30);
        assertEquals(64, listener.trade(3).getIngredients().get(0).getAmount());
    }

    private static LootTable lootTable(String key) {
        return new LootTable() {
            @Override
            public @NotNull Collection<ItemStack> populateLoot(Random random, @NotNull LootContext context) {
                return List.of();
            }

            @Override
            public void fillInventory(@NotNull Inventory inventory, Random random, @NotNull LootContext context) {
            }

            @Override
            public @NotNull NamespacedKey getKey() {
                return NamespacedKey.minecraft(key);
            }
        };
    }

    private List<ItemStack> generate(LootTable table) {
        Location location = new Location(world, 0, 70, 0);
        List<ItemStack> loot = new ArrayList<>(List.of(new ItemStack(Material.BREAD)));
        LootGenerateEvent event = new LootGenerateEvent(world, null, null, table,
                new LootContext.Builder(location).build(), loot, false);
        server.getPluginManager().callEvent(event);
        return event.getLoot();
    }

    @Test
    void chestsContainTheBook() {
        setConfig(ConfigData.Keys.SOURCES_LOOT, 100);
        List<ItemStack> loot = generate(lootTable("chests/simple_dungeon"));
        assertEquals(2, loot.size());
        assertTrue(WoodCutterEnchantment.has(loot.get(1)));
        // not the loot of mobs
        assertEquals(1, generate(lootTable("entities/zombie")).size());
    }

    @Test
    void lootWithoutATableIsIgnored() {
        // Paper returns no loot table for chests of copied maps whose table it can't map to a Bukkit one
        setConfig(ConfigData.Keys.SOURCES_LOOT, 100);
        List<ItemStack> loot = assertDoesNotThrow(() -> generate(null));
        assertEquals(List.of(new ItemStack(Material.BREAD)), loot);
    }

    @Test
    void menuSwitchesWoodCutterOnAndOff() {
        PlayerMock player = addPlayer("Guest", "woodcutter.use");
        player.performCommand("woodcutter menu");
        assertInstanceOf(Menu.class, player.getOpenInventory().getTopInventory().getHolder());
        Menu menu = (Menu) player.getOpenInventory().getTopInventory().getHolder();
        assertEquals(Material.DIAMOND_AXE, menu.getInventory().getItem(Menu.SLOT_TOGGLE).getType());
        assertEquals(Material.EXPERIENCE_BOTTLE, menu.getInventory().getItem(Menu.SLOT_SKILL).getType());
        // the settings are for admins
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(Menu.SLOT_PAGE).getType());

        player.simulateInventoryClick(Menu.SLOT_TOGGLE);
        server.getScheduler().performTicks(2);
        assertFalse(BlockBreakListener.isEnabled(player));
        assertEquals(Material.BARRIER, menu.getInventory().getItem(Menu.SLOT_TOGGLE).getType());

        menu.click(Menu.SLOT_PAGE);
        assertSame(menu, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void adminsChangeSettingsInTheMenu() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        Menu menu = new Menu(plugin, admin, Menu.Page.MAIN);
        menu.open();
        assertEquals(Material.COMPARATOR, menu.getInventory().getItem(Menu.SLOT_PAGE).getType());
        menu.click(Menu.SLOT_PAGE);
        Menu settings = (Menu) admin.getOpenInventory().getTopInventory().getHolder();
        assertNotSame(menu, settings);

        int slot = Menu.settingSlot(Menu.SETTINGS.indexOf(ConfigData.Keys.PROTECT_AXE));
        assertEquals(Material.SHIELD, settings.getInventory().getItem(slot).getType());
        // the dye below the icon shows the state, both can be clicked
        slot += 9;
        assertEquals(Material.LIME_DYE, settings.getInventory().getItem(slot).getType());
        settings.click(slot);
        assertFalse(plugin.getConfigData().getBoolean(ConfigData.Keys.PROTECT_AXE));
        assertEquals(Material.GRAY_DYE, settings.getInventory().getItem(slot).getType());
        plugin.reload();
        assertFalse(plugin.getConfigData().getBoolean(ConfigData.Keys.PROTECT_AXE));
        settings.click(Menu.SLOT_BACK);
        assertEquals(Menu.SLOT_PAGE + 1, admin.getOpenInventory().getTopInventory().getSize());
    }

}
