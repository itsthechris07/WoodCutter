package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * WoodCutter 2026.
 * <p>
 * Adds the enchantment to axes and books that are enchanted at an enchanting table with the configured level.
 *
 * @author Christian34
 */
public class EnchantListener implements Listener {
    private final WoodCutter instance;

    public EnchantListener(WoodCutter instance) {
        this.instance = instance;
    }

    /**
     * @param chance the chance in percent
     */
    static boolean roll(double chance) {
        return ThreadLocalRandom.current().nextDouble(100) < chance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        if (!WoodCutterEnchantment.canEnchantItem(e.getItem())) return;
        if (!e.getEnchanter().hasPermission(BlockBreakListener.PERMISSION_USE)) return;
        ConfigData config = instance.getConfigData();
        if (e.getExpLevelCost() != config.getInt(ConfigData.Keys.MIN_LEVEL)) return;
        if (!roll(config.getDouble(ConfigData.Keys.CHANCE))) return;

        int level = WoodCutterEnchantment.rollLevel();
        WoodCutterEnchantment.apply(e.getItem(), level);
        Statistics.recordEnchantment("table", level);
        // a book is replaced by an enchanted book after the event, which has to keep the enchantment
        Inventory inventory = e.getInventory();
        TaskManager.run(e.getEnchanter(), () -> {
            ItemStack result = inventory.getItem(0);
            if (result != null && result.getType() == Material.ENCHANTED_BOOK && !WoodCutterEnchantment.has(result)) {
                WoodCutterEnchantment.apply(result, level);
                inventory.setItem(0, result);
            }
        });
    }

}
