package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.menu.Menu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the books of librarians and the menu.
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
