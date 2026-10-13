package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.events.WoodCutterEvent;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.hooks.WorldGuardHook;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.woodcutting.Cutter;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * WoodCutter 2026.
 * <p>
 * Cuts the whole tree when a player breaks a log with an enchanted axe - or with any axe, if they have WoodCutter as
 * a goodie.
 *
 * @author Christian34
 */
public class BlockBreakListener implements Listener {
    public static final String PERMISSION_USE = "woodcutter.use";
    public static final String PERMISSION_GOODIE = "woodcutter.goodie";
    public static final String PERMISSION_BYPASS_DELAY = "woodcutter.bypass.delay";
    /**
     * followed by the seconds of the delay, e.g. for ranks
     */
    public static final String PERMISSION_DELAY = "woodcutter.delay.";
    private final WoodCutter instance;

    public BlockBreakListener(WoodCutter instance) {
        this.instance = instance;
    }

    private static NamespacedKey disabledKey() {
        return new NamespacedKey(WoodCutter.getInstance(), "disabled");
    }

    /**
     * @return false if the player has switched WoodCutter off for themselves (/woodcutter toggle)
     */
    public static boolean isEnabled(Player player) {
        return !player.getPersistentDataContainer().has(disabledKey(), PersistentDataType.BYTE);
    }

    public static void setEnabled(Player player, boolean enabled) {
        if (enabled) player.getPersistentDataContainer().remove(disabledKey());
        else player.getPersistentDataContainer().set(disabledKey(), PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * @return true if the player may cut trees without the enchantment (setting 'goodie': none, permission, everyone)
     */
    public boolean hasGoodie(Player player) {
        String goodie = instance.getConfigData().getString(ConfigData.Keys.GOODIE);
        if (goodie.equalsIgnoreCase("everyone")) return true;
        return goodie.equalsIgnoreCase("permission") && player.hasPermission(PERMISSION_GOODIE);
    }

    /**
     * @param pattern the name of a world, * stands for any characters (e.g. sg_*), upper and lower case don't matter
     */
    static boolean matchesWorld(String world, String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String part : pattern.split("\\*", -1)) {
            if (!regex.isEmpty()) regex.append(".*");
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE).matcher(world).matches();
    }

    /**
     * @return the level of the enchantment the player cuts a tree with by breaking the block (0 with WoodCutter as a
     * goodie), -1 if they just break the block
     */
    private int cuttingLevel(Player player, Block block) {
        ConfigData config = instance.getConfigData();
        if (!Cutter.isTrunk(block.getType(), config)) return -1;
        // the logs of the tree that is being cut
        if (Cutter.isCutting(player)) return -1;
        if (!config.getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES) && PlacedLogs.isPlaced(block)) return -1;

        if (config.getBoolean(ConfigData.Keys.REQUIRE_SNEAKING) && !player.isSneaking()) return -1;
        if (!player.hasPermission(PERMISSION_USE) || !isEnabled(player)) return -1;
        ItemStack axe = player.getInventory().getItemInMainHand();
        if (!WoodCutterEnchantment.isAxe(axe)) return -1;
        int level = WoodCutterEnchantment.level(axe);
        if (level == 0 && !hasGoodie(player)) return -1;

        String world = block.getWorld().getName();
        for (String disabled : config.getList(ConfigData.Keys.DISABLED_WORLDS)) {
            if (matchesWorld(world, disabled)) return -1;
        }

        WorldGuardHook worldGuard = instance.getWorldGuard();
        if (worldGuard != null && !worldGuard.isAllowed(player, block)) return -1;
        return level;
    }

    /**
     * tells the player over the action bar how long they have to wait for the next tree
     *
     * @return false if they don't have to wait
     */
    private boolean showDelay(Player player) {
        long remaining = instance.remainingDelay(player);
        if (remaining <= 0) return false;
        String message = instance.getConfigData().getString(ConfigData.Keys.DELAY_MSG);
        if (!message.isEmpty()) {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message.replace("%time%", Long.toString(remaining))));
        }
        return true;
    }

    /**
     * the delay is shown when the player starts to break the log, so they can still wait for the whole tree
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent e) {
        if (cuttingLevel(e.getPlayer(), e.getBlock()) >= 0) showDelay(e.getPlayer());
    }

    // after protection plugins, so trees in protected areas are not cut
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        ConfigData config = instance.getConfigData();
        Player player = e.getPlayer();
        int level = cuttingLevel(player, block);
        if (level < 0) return;
        ItemStack axe = player.getInventory().getItemInMainHand();

        boolean delayed = config.getBoolean(ConfigData.Keys.DELAY) && !player.hasPermission(PERMISSION_BYPASS_DELAY);
        if (showDelay(player)) return;

        boolean pays = player.getGameMode() != GameMode.CREATIVE;
        // the woodcutting skill makes trees cheaper
        double costs = pays ? Math.max(0, 1 - instance.getSkill().perk(player, Skill.PERK_COSTS) / 100) : 0;
        int hunger = (int) Math.round(Math.max(0, config.getInt(ConfigData.Keys.COSTS_HUNGER)) * costs);
        int experience = (int) Math.round(Math.max(0, config.getInt(ConfigData.Keys.COSTS_EXPERIENCE)) * costs);
        if (player.getFoodLevel() < hunger) {
            Message.NOT_ENOUGH_FOOD.sendActionBar(player);
            return;
        }
        if (player.getLevel() < experience) {
            Message.NOT_ENOUGH_EXPERIENCE.sendActionBar(player, "%levels%", Integer.toString(experience));
            return;
        }

        WoodCutterEvent woodCutterEvent = new WoodCutterEvent(e);
        Bukkit.getPluginManager().callEvent(woodCutterEvent);
        if (woodCutterEvent.isCancelled()) return;
        int logs = new Cutter(instance, player, block, Cutter.maxBlocks(player, config, level)).cut();
        // a single log is no tree: it costs nothing and the player may use WoodCutter right away
        if (logs == 0) return;
        if (hunger > 0) player.setFoodLevel(player.getFoodLevel() - hunger);
        if (experience > 0) player.setLevel(player.getLevel() - experience);
        if (delayed) instance.getDelays().put(player.getUniqueId(), System.currentTimeMillis());
        Statistics.recordTree(block.getType(), axe.getType(), logs, level);
        instance.getSkill().reward(player, logs + 1);
    }

    /**
     * the perk 'axe-saver' of the woodcutting skill: some logs of a tree don't damage the axe
     */
    @EventHandler(ignoreCancelled = true)
    public void onAxeDamage(PlayerItemDamageEvent e) {
        Player player = e.getPlayer();
        if (!Cutter.isCuttingTree(player) || !WoodCutterEnchantment.isAxe(e.getItem())) return;
        double chance = instance.getSkill().perk(player, Skill.PERK_AXE_SAVER);
        if (chance > 0 && ThreadLocalRandom.current().nextDouble(100) < chance) e.setCancelled(true);
    }

    /**
     * broken logs are no longer placed by a player, whoever breaks them
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent e) {
        if (Cutter.isWood(e.getBlock().getType())) PlacedLogs.unmark(e.getBlock());
    }

    /**
     * puts the drops of a cut tree into the inventory of the player, what does not fit is dropped
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        Player player = e.getPlayer();
        if (!Cutter.isCutBy(player, e.getBlock()) || !Cutter.dropsToInventory(instance, player)) return;
        Iterator<Item> items = e.getItems().iterator();
        while (items.hasNext()) {
            Item item = items.next();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItemStack());
            if (leftover.isEmpty()) items.remove();
            else item.setItemStack(leftover.values().iterator().next());
        }
    }

}
