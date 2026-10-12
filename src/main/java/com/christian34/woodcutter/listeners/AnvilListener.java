package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;

/**
 * WoodCutter 2026.
 * <p>
 * The enchantment in anvils and grindstones. An anvil transfers it from an enchanted book to an axe or another book
 * and combines two axes - two equal levels become the next one. A grindstone removes it.
 *
 * @author Christian34
 */
public class AnvilListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        ItemStack first = e.getInventory().getItem(0);
        ItemStack second = e.getInventory().getItem(1);
        if (first == null || second == null) return;
        int firstLevel = WoodCutterEnchantment.level(first);
        int secondLevel = WoodCutterEnchantment.level(second);
        if (secondLevel == 0) return;
        int level = WoodCutterEnchantment.combine(firstLevel, secondLevel);
        // the result is a copy of the first item, which keeps its level anyway
        if (level == firstLevel) return;

        boolean firstIsBook = first.getType() == Material.ENCHANTED_BOOK;
        boolean secondIsBook = second.getType() == Material.ENCHANTED_BOOK;
        if (firstIsBook) {
            if (!secondIsBook) return;
        } else {
            if (!WoodCutterEnchantment.isAxe(first)) return;
            if (!secondIsBook && second.getType() != first.getType()) return;
        }

        ItemStack result = e.getResult();
        if (result == null || result.getType().isAir()) {
            // the second item has nothing else the anvil could transfer
            result = first.clone();
            // results without costs can't be taken out
            if (e.getView().getRepairCost() < 1) e.getView().setRepairCost(level);
        }
        WoodCutterEnchantment.apply(result, level);
        e.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        ItemStack upper = e.getInventory().getItem(0);
        ItemStack lower = e.getInventory().getItem(1);
        if (!WoodCutterEnchantment.has(upper) && !WoodCutterEnchantment.has(lower)) return;

        ItemStack result = e.getResult();
        if (result == null || result.getType().isAir()) {
            // the grindstone finds nothing to remove from an item that only has WoodCutter
            if (upper != null && lower != null) return;
            ItemStack item = upper != null ? upper : lower;
            result = item.getType() == Material.ENCHANTED_BOOK ? new ItemStack(Material.BOOK, item.getAmount()) : item.clone();
        }
        WoodCutterEnchantment.remove(result);
        e.setResult(result);
    }

}
