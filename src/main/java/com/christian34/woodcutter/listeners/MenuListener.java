package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.menu.Menu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * WoodCutter 2026.
 * <p>
 * Passes the clicks in the menu on to it - its items can't be taken or moved.
 *
 * @author Christian34
 */
public class MenuListener implements Listener {
    private final WoodCutter instance;

    public MenuListener(WoodCutter instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);
        // not the inventory of the player below the menu
        if (e.getRawSlot() >= e.getInventory().getSize() || !menu.getPlayer().equals(e.getWhoClicked())) return;
        // the menu opens another one or changes the config, which is not allowed while the click is handled
        int slot = e.getRawSlot();
        menu.getPlayer().getScheduler().run(instance, task -> menu.click(slot), null);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

}
