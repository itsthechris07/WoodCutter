package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.PluginTestBase;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.menu.Menu;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoodCutter 2026.
 * <p>
 * Tests the page of the menu that changes the animation.
 *
 * @author Christian34
 */
class AnimationMenuTest extends PluginTestBase {

    private static int slot(String key) {
        return Menu.ANIMATION.stream().filter(option -> option.key().equals(key)).findFirst().orElseThrow().slot();
    }

    private Menu openAnimation(PlayerMock admin) {
        new Menu(plugin, admin, Menu.Page.SETTINGS).open();
        Menu settings = (Menu) admin.getOpenInventory().getTopInventory().getHolder();
        assertEquals(Material.OAK_LOG, settings.getInventory().getItem(Menu.SLOT_ANIMATION).getType());
        settings.click(Menu.SLOT_ANIMATION);
        Menu animation = (Menu) admin.getOpenInventory().getTopInventory().getHolder();
        assertNotSame(settings, animation);
        return animation;
    }

    @Test
    void adminsChangeTheModeAndItsSettings() {
        Menu menu = openAnimation(addPlayer("Admin", "woodcutter.admin"));
        int mode = slot(ConfigData.Keys.ANIMATION_MODE);
        int blocks = slot(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK);
        int physics = slot(ConfigData.Keys.TOPPLE_PHYSICS);
        // with 'layers' only its own settings are shown, the amount shows the number
        assertEquals(Material.OAK_LOG, menu.getInventory().getItem(mode).getType());
        assertEquals(2, menu.getInventory().getItem(blocks + 9).getAmount());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(physics).getType());

        menu.click(blocks);
        assertEquals(3, plugin.getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));
        menu.click(blocks + 9, true);
        menu.click(blocks + 9, true);
        assertEquals(1, plugin.getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));
        // not below 1
        menu.click(blocks, true);
        assertEquals(1, plugin.getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));

        menu.click(mode);
        assertEquals("topple", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
        assertEquals(Material.ANVIL, menu.getInventory().getItem(physics).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(blocks).getType());
        // a hidden setting can't be clicked
        menu.click(blocks);
        assertEquals(1, plugin.getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));

        // the values go round, a right click goes back
        menu.click(physics);
        assertEquals("fall-apart", plugin.getConfigData().getString(ConfigData.Keys.TOPPLE_PHYSICS));
        menu.click(physics);
        assertEquals("simple", plugin.getConfigData().getString(ConfigData.Keys.TOPPLE_PHYSICS));
        menu.click(physics, true);
        assertEquals("fall-apart", plugin.getConfigData().getString(ConfigData.Keys.TOPPLE_PHYSICS));

        int timber = slot(ConfigData.Keys.TOPPLE_TIMBER);
        assertEquals(Material.GRAY_DYE, menu.getInventory().getItem(timber + 9).getType());
        menu.click(timber);
        assertTrue(plugin.getConfigData().getBoolean(ConfigData.Keys.TOPPLE_TIMBER));
        assertEquals(Material.LIME_DYE, menu.getInventory().getItem(timber + 9).getType());

        int damage = slot(ConfigData.Keys.TOPPLE_DAMAGE);
        assertEquals(Material.GRAY_DYE, menu.getInventory().getItem(damage + 9).getType());
        menu.click(damage);
        menu.click(damage);
        assertEquals(2, plugin.getConfigData().getDouble(ConfigData.Keys.TOPPLE_DAMAGE));
        assertEquals(Material.LIME_DYE, menu.getInventory().getItem(damage + 9).getType());
        assertEquals(2, menu.getInventory().getItem(damage + 9).getAmount());

        // the changes are in the file
        plugin.reload();
        assertEquals("topple", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
        assertEquals("fall-apart", plugin.getConfigData().getString(ConfigData.Keys.TOPPLE_PHYSICS));
        menu.click(mode);
        assertEquals("layers", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
    }

    private static String plain(String text) {
        return text.replaceAll("§.", "");
    }

    @Test
    void settingsHaveANameAndADescription() {
        setConfig(ConfigData.Keys.ANIMATION_MODE, "topple");
        Menu menu = openAnimation(addPlayer("Admin", "woodcutter.admin"));
        ItemMeta knockback = menu.getInventory().getItem(slot(ConfigData.Keys.TOPPLE_KNOCKBACK)).getItemMeta();
        assertEquals("Knockback", plain(knockback.getDisplayName()));
        List<String> lore = knockback.getLore().stream().map(AnimationMenuTest::plain).toList();
        assertEquals(List.of("Mobs and players the tree falls on are", "pushed away", " ", "Click to switch it on or off",
                ConfigData.Keys.TOPPLE_KNOCKBACK), lore);

        // the values of a choice, each in its own line
        ItemMeta physics = menu.getInventory().getItem(slot(ConfigData.Keys.TOPPLE_PHYSICS)).getItemMeta();
        assertEquals("Physics: rigid", plain(physics.getDisplayName()));
        assertTrue(physics.getLore().get(0).startsWith("§7simple: "));
        assertTrue(physics.getLore().stream().anyMatch(line -> line.startsWith("§7rigid: ")));
    }

    @Test
    void theSettingsHaveANameAndADescriptionToo() {
        Menu settings = new Menu(plugin, addPlayer("Admin", "woodcutter.admin"), Menu.Page.SETTINGS);
        int slot = Menu.settingSlot(Menu.SETTINGS.indexOf(ConfigData.Keys.PROTECT_AXE));
        for (int at : new int[]{slot, slot + 9}) {
            ItemMeta meta = settings.getInventory().getItem(at).getItemMeta();
            assertEquals("§aProtect axe", meta.getDisplayName());
            assertEquals(List.of("Cutting stops before the hit that", "would break the axe", " ", "Click to switch it on or off",
                    ConfigData.Keys.PROTECT_AXE), meta.getLore().stream().map(AnimationMenuTest::plain).toList());
        }
        // a right click switches it as well
        settings.click(slot, true);
        assertFalse(plugin.getConfigData().getBoolean(ConfigData.Keys.PROTECT_AXE));
        assertEquals("§cProtect axe", settings.getInventory().getItem(slot).getItemMeta().getDisplayName());
    }

    @Test
    void theResetSetsThePageBackToItsDefaultsAfterAConfirmation() throws Exception {
        // in the file, which every change in the menu loads again
        plugin.getConfigData().set(Map.of(ConfigData.Keys.ANIMATION_MODE, "topple", ConfigData.Keys.TOPPLE_DAMAGE, 4,
                ConfigData.Keys.PROTECT_AXE, false));
        Menu menu = openAnimation(addPlayer("Admin", "woodcutter.admin"));
        // the changed settings of the page, not the one of the other page
        List<String> lore = menu.getInventory().getItem(Menu.SLOT_RESET).getItemMeta().getLore().stream()
                .map(AnimationMenuTest::plain).toList();
        assertTrue(lore.contains(" - Mode"));
        assertTrue(lore.contains(" - Damage"));
        assertFalse(lore.contains(" - Protect axe"));

        // the first click only asks to click again, another click in between cancels it
        menu.click(Menu.SLOT_RESET);
        assertEquals("topple", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
        assertTrue(menu.getInventory().getItem(Menu.SLOT_RESET).getItemMeta().getLore().contains("§cClick again to reset"));
        menu.click(slot(ConfigData.Keys.TOPPLE_KNOCKBACK));
        menu.click(Menu.SLOT_RESET);
        assertEquals("topple", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));

        menu.click(Menu.SLOT_RESET);
        assertEquals("layers", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
        assertEquals(0, plugin.getConfigData().getDouble(ConfigData.Keys.TOPPLE_DAMAGE));
        assertTrue(plugin.getConfigData().getBoolean(ConfigData.Keys.TOPPLE_KNOCKBACK));
        assertFalse(plugin.getConfigData().getBoolean(ConfigData.Keys.PROTECT_AXE));
        assertEquals(List.of("§7All settings have their default values"),
                menu.getInventory().getItem(Menu.SLOT_RESET).getItemMeta().getLore());
        plugin.reload();
        assertEquals("layers", plugin.getConfigData().getString(ConfigData.Keys.ANIMATION_MODE));
    }

    @Test
    void rightClicksArePassedOn() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        openAnimation(admin);
        admin.simulateInventoryClick(admin.getOpenInventory(), org.bukkit.event.inventory.ClickType.RIGHT,
                slot(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));
        server.getScheduler().performTicks(2);
        assertEquals(1, plugin.getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK));
    }

    @Test
    void backToTheSettings() {
        PlayerMock admin = addPlayer("Admin", "woodcutter.admin");
        openAnimation(admin).click(Menu.SLOT_BACK);
        Menu settings = (Menu) admin.getOpenInventory().getTopInventory().getHolder();
        assertEquals(Material.OAK_LOG, settings.getInventory().getItem(Menu.SLOT_ANIMATION).getType());
    }

    @Test
    void playersDontSeeTheAnimation() {
        PlayerMock player = addPlayer("Guest", "woodcutter.use");
        Menu menu = new Menu(plugin, player, Menu.Page.ANIMATION);
        assertEquals(Menu.SLOT_PAGE + 1, menu.getInventory().getSize());
    }

}
