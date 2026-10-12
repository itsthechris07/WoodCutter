package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.events.WoodCutterEvent;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.woodcutting.Cutter;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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

    // after protection plugins, so trees in protected areas are not cut
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!Tag.LOGS.isTagged(block.getType())) return;
        Player player = e.getPlayer();
        // the logs of the tree that is being cut
        if (Cutter.isCutting(player)) return;
        ConfigData config = instance.getConfigData();
        if (!config.getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES) && PlacedLogs.isPlaced(block)) return;

        if (config.getBoolean(ConfigData.Keys.REQUIRE_SNEAKING) && !player.isSneaking()) return;
        if (!player.hasPermission(PERMISSION_USE) || !isEnabled(player)) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        if (!WoodCutterEnchantment.isAxe(axe)) return;
        int level = WoodCutterEnchantment.level(axe);
        boolean enchanted = level > 0;
        if (!enchanted && !hasGoodie(player)) return;

        String world = block.getWorld().getName();
        for (String disabled : config.getList(ConfigData.Keys.DISABLED_WORLDS)) {
            if (matchesWorld(world, disabled)) return;
        }

        Map<UUID, Long> delays = instance.getDelays();
        long now = System.currentTimeMillis();
        boolean delayed = config.getBoolean(ConfigData.Keys.DELAY) && !player.hasPermission(PERMISSION_BYPASS_DELAY);
        if (delayed) {
            long delay = config.getInt(ConfigData.Keys.DELAY_TIME) * 1000L;
            delays.values().removeIf(lastUsage -> lastUsage + delay <= now);
            Long lastUsage = delays.get(player.getUniqueId());
            if (lastUsage != null) {
                String message = config.getString(ConfigData.Keys.DELAY_MSG);
                if (!message.isEmpty()) {
                    long remaining = Math.max(1, (lastUsage + delay - now + 999) / 1000);
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message.replace("%time%", Long.toString(remaining))));
                }
                return;
            }
        }

        boolean pays = player.getGameMode() != GameMode.CREATIVE;
        int hunger = pays ? Math.max(0, config.getInt(ConfigData.Keys.COSTS_HUNGER)) : 0;
        int experience = pays ? Math.max(0, config.getInt(ConfigData.Keys.COSTS_EXPERIENCE)) : 0;
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
        if (delayed) delays.put(player.getUniqueId(), now);
        Statistics.recordTree(block.getType(), axe.getType(), logs, level);
    }

    /**
     * broken logs are no longer placed by a player, whoever breaks them
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent e) {
        if (Tag.LOGS.isTagged(e.getBlock().getType())) PlacedLogs.unmark(e.getBlock());
    }

    /**
     * puts the drops of a cut tree into the inventory of the player, what does not fit is dropped
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        if (!instance.getConfigData().getBoolean(ConfigData.Keys.DROPS_TO_INVENTORY)) return;
        Player player = e.getPlayer();
        if (!Cutter.isCutBy(player, e.getBlock())) return;
        Iterator<Item> items = e.getItems().iterator();
        while (items.hasNext()) {
            Item item = items.next();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItemStack());
            if (leftover.isEmpty()) items.remove();
            else item.setItemStack(leftover.values().iterator().next());
        }
    }

}
