package com.christian34.woodcutter.listeners;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.events.WoodCutterEvent;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.hooks.EconomyHook;
import com.christian34.woodcutter.hooks.WorldGuardHook;
import com.christian34.woodcutter.platform.Platform;
import com.christian34.woodcutter.skill.Skill;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.woodcutting.Cutter;
import com.christian34.woodcutter.woodcutting.PlacedLogs;
import com.christian34.woodcutter.woodcutting.PlantedSaplings;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
     * break the saplings WoodCutter has planted (with 'replant.protect')
     */
    public static final String PERMISSION_REPLANT_BREAK = "woodcutter.replant.break";
    /**
     * followed by the seconds of the delay, e.g. for ranks
     */
    public static final String PERMISSION_DELAY = "woodcutter.delay.";
    private final WoodCutter instance;
    /**
     * players who have been told to sneak
     */
    private final Set<UUID> hinted = ConcurrentHashMap.newKeySet();
    private volatile DisabledWorlds disabledWorlds = new DisabledWorlds(List.of(), List.of());

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
     * @param pattern the name of a world (or a WorldGuard region), * stands for any characters (e.g. sg_*), upper and
     *                lower case don't matter
     */
    public static boolean matchesWorld(String world, String pattern) {
        return worldPattern(pattern).matcher(world).matches();
    }

    private static Pattern worldPattern(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String part : pattern.split("\\*", -1)) {
            if (!regex.isEmpty()) regex.append(".*");
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * @return true if WoodCutter is disabled in the world by the setting 'disabled-worlds'
     */
    private boolean isDisabled(String world) {
        List<String> names = instance.getConfigData().getList(ConfigData.Keys.DISABLED_WORLDS);
        DisabledWorlds disabled = disabledWorlds;
        // compiled again only when the setting has changed
        if (!disabled.names().equals(names)) {
            disabled = new DisabledWorlds(names, names.stream().map(BlockBreakListener::worldPattern).toList());
            this.disabledWorlds = disabled;
        }
        for (Pattern pattern : disabled.patterns()) {
            if (pattern.matcher(world).matches()) return true;
        }
        return false;
    }

    private record DisabledWorlds(List<String> names, List<Pattern> patterns) {
    }

    /**
     * @param sneaking if the player sneaks (or would)
     * @return the level of the enchantment the player cuts a tree with by breaking the block (0 with WoodCutter as a
     * goodie), -1 if they just break the block
     */
    private int cuttingLevel(Player player, Block block, boolean sneaking) {
        ConfigData config = instance.getConfigData();
        if (!Cutter.isTrunk(block.getType(), config)) return -1;
        // the logs of the tree that is being cut
        if (Cutter.isCutting(player)) return -1;
        if (!config.getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES) && PlacedLogs.isPlaced(block)) return -1;

        if (config.getBoolean(ConfigData.Keys.REQUIRE_SNEAKING) && !sneaking) return -1;
        if (!player.hasPermission(PERMISSION_USE) || !isEnabled(player)) return -1;
        ItemStack axe = player.getInventory().getItemInMainHand();
        if (!WoodCutterEnchantment.isAxe(axe)) return -1;
        int level = WoodCutterEnchantment.level(axe);
        if (level == 0 && !hasGoodie(player)) return -1;

        if (isDisabled(block.getWorld().getName())) return -1;

        WorldGuardHook worldGuard = instance.getWorldGuard();
        if (worldGuard != null && !worldGuard.isAllowed(player, block, config.getString(ConfigData.Keys.WORLDGUARD_REGIONS_MODE),
                config.getList(ConfigData.Keys.WORLDGUARD_REGIONS))) {
            return -1;
        }
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
            Platform.get().sendActionBar(player, Message.format(message.replace("%time%", Long.toString(remaining))));
        }
        return true;
    }

    /**
     * tells players once (until the next restart) that they have to sneak, when they break a log of a tree they
     * could cut - breaking single logs without sneaking is still possible
     */
    private void showSneakHint(Player player, Block block) {
        if (player.isSneaking() || hinted.contains(player.getUniqueId())) return;
        if (cuttingLevel(player, block, true) < 0) return;
        hinted.add(player.getUniqueId());
        Message.SNEAK_HINT.sendActionBar(player);
    }

    /**
     * the delay is shown when the player starts to break the log, so they can still wait for the whole tree
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent e) {
        Player player = e.getPlayer();
        int level = cuttingLevel(player, e.getBlock(), player.isSneaking());
        if (level < 0 || showDelay(player) || e.getInstaBreak()) return;
        // bigger trees take longer to chop
        instance.getChopping().start(player, e.getBlock(), Cutter.maxBlocks(player, instance.getConfigData(), level));
    }

    /**
     * saplings WoodCutter has planted can't be broken until they grow (setting 'replant.protect')
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSaplingBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!PlantedSaplings.isPlanted(block)) return;
        Player player = e.getPlayer();
        if (instance.getConfigData().getBoolean(ConfigData.Keys.REPLANT_PROTECT) && !player.hasPermission(PERMISSION_REPLANT_BREAK)
                && player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            Message.SAPLING_PROTECTED.sendActionBar(player);
            return;
        }
        PlantedSaplings.unmark(block);
    }

    /**
     * a planted sapling that has grown is a tree
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent e) {
        PlantedSaplings.unmark(e.getLocation().getBlock());
    }

    /**
     * a sapling that was washed away or moved leaves its mark - a player who places a block there gets a normal one
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (PlantedSaplings.isPlanted(e.getBlockPlaced())) PlantedSaplings.unmark(e.getBlockPlaced());
    }

    // after protection plugins, so trees in protected areas are not cut
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        ConfigData config = instance.getConfigData();
        Player player = e.getPlayer();
        int level = cuttingLevel(player, block, player.isSneaking());
        if (level < 0) {
            showSneakHint(player, block);
            return;
        }
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
        // money of an economy plugin (Vault), nothing is paid without one
        boolean economy = EconomyHook.isAvailable();
        double money = economy ? Math.max(0, config.getDouble(ConfigData.Keys.ECONOMY_COSTS)) * costs : 0;
        if (money > 0 && !EconomyHook.has(player, money)) {
            Message.NOT_ENOUGH_MONEY.sendActionBar(player, "%money%", EconomyHook.format(money));
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
        if (money > 0) EconomyHook.withdraw(player, money);
        double reward = economy && pays ? (logs + 1) * Math.max(0, config.getDouble(ConfigData.Keys.ECONOMY_REWARD_PER_LOG)) : 0;
        if (reward > 0 && EconomyHook.deposit(player, reward)) {
            Message.ECONOMY_REWARD.send(player, "%money%", EconomyHook.format(reward), "%logs%", Integer.toString(logs + 1));
        }
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
        if (!Cutter.isCutBy(player, e.getBlock())) return;
        // the animation 'topple': the items drop where the tree hits the ground
        e.getItems().removeIf(item -> Cutter.dropWhereItLands(player, e.getBlock(), item.getItemStack()));
        if (!Cutter.dropsToInventory(instance, player)) return;
        Iterator<Item> items = e.getItems().iterator();
        while (items.hasNext()) {
            Item item = items.next();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItemStack());
            if (leftover.isEmpty()) items.remove();
            else item.setItemStack(leftover.values().iterator().next());
        }
    }

}
