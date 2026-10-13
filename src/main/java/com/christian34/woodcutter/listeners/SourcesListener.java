package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Statistics;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * WoodCutter 2026.
 * <p>
 * Other ways to get a book with the enchantment than the enchanting table: librarians and wandering traders sell it,
 * and chests with loot contain it.
 *
 * @author Christian34
 */
public class SourcesListener implements Listener {
    private static final int MAX_USES = 12;
    private final WoodCutter instance;

    public SourcesListener(WoodCutter instance) {
        this.instance = instance;
    }

    private static ItemStack book(int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        WoodCutterEnchantment.apply(book, level);
        return book;
    }

    /**
     * @return a trade like the ones of librarians for enchanted books: emeralds and a book for the enchanted book
     */
    MerchantRecipe trade(int level) {
        int price = Math.max(1, instance.getConfigData().getInt(ConfigData.Keys.SOURCES_PRICE)) * level;
        MerchantRecipe recipe = new MerchantRecipe(book(level), MAX_USES);
        recipe.addIngredient(new ItemStack(Material.EMERALD, Math.min(Material.EMERALD.getMaxStackSize(), price)));
        recipe.addIngredient(new ItemStack(Material.BOOK));
        return recipe;
    }

    /**
     * a new trade of a librarian (also when they reach the next level) or of a wandering trader is replaced
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(VillagerAcquireTradeEvent e) {
        String chance;
        if (e.getEntity() instanceof Villager villager && villager.getProfession() == Villager.Profession.LIBRARIAN) {
            chance = ConfigData.Keys.SOURCES_LIBRARIAN;
        } else if (e.getEntity() instanceof WanderingTrader) {
            chance = ConfigData.Keys.SOURCES_WANDERING_TRADER;
        } else {
            return;
        }
        if (!EnchantListener.roll(instance.getConfigData().getDouble(chance))) return;
        int level = WoodCutterEnchantment.rollLevel();
        e.setRecipe(trade(level));
        Statistics.recordEnchantment("trade", level);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent e) {
        // not the loot of fishing, mobs or archaeology
        if (!e.getLootTable().getKey().getKey().startsWith("chests/")) return;
        if (!EnchantListener.roll(instance.getConfigData().getDouble(ConfigData.Keys.SOURCES_LOOT))) return;
        int level = WoodCutterEnchantment.rollLevel();
        List<ItemStack> loot = new ArrayList<>(e.getLoot());
        loot.add(book(level));
        e.setLoot(loot);
        Statistics.recordEnchantment("loot", level);
    }

}
