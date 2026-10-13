package com.christian34.woodcutter;

import com.christian34.woodcutter.commands.CommandWoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.files.ConfigData;
import com.christian34.woodcutter.files.MessageData;
import com.christian34.woodcutter.hooks.PlaceholderHook;
import com.christian34.woodcutter.hooks.WorldGuardHook;
import com.christian34.woodcutter.listeners.AnvilListener;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.listeners.BlockPlaceListener;
import com.christian34.woodcutter.listeners.EnchantListener;
import com.christian34.woodcutter.utils.Debug;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.utils.TaskManager;
import com.christian34.woodcutter.utils.Updater;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WoodCutter 2026.
 * <p>
 * Main class of the plugin: loads the files, listeners and the command, and holds the delays of the players.
 *
 * @author Christian34
 */
public class WoodCutter extends JavaPlugin {
    private static final List<String> RELATED_PLUGINS = List.of("WorldGuard", "GriefPrevention", "Towny", "Lands", "Residence",
            "PlotSquared", "CoreProtect", "mcMMO", "Jobs", "AuraSkills", "ItemsAdder", "Oraxen", "Nexo", "EasyPrefix", "ViaVersion", "PlaceholderAPI");
    private static WoodCutter instance = null;
    /**
     * the time a player has cut a tree the last time
     */
    private final Map<UUID, Long> delays = new ConcurrentHashMap<>();
    private ConfigData configData;
    private MessageData messageData;
    private Updater updater;
    private WorldGuardHook worldGuard;

    public static WoodCutter getInstance() {
        return instance;
    }

    /**
     * -Dwoodcutter.offline=true disables everything that sends data to the internet (bStats, update check),
     * used by the tests so they don't report to the live statistics
     */
    public static boolean isOffline() {
        return Boolean.getBoolean("woodcutter.offline");
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public Updater getUpdater() {
        return updater;
    }

    public Map<UUID, Long> getDelays() {
        return delays;
    }

    /**
     * @return the seconds until the player can cut the next tree, 0 if they can right away
     */
    public long remainingDelay(Player player) {
        if (!configData.getBoolean(ConfigData.Keys.DELAY) || player.hasPermission(BlockBreakListener.PERMISSION_BYPASS_DELAY)) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long delay = configData.getInt(ConfigData.Keys.DELAY_TIME) * 1000L;
        delays.values().removeIf(lastUsage -> lastUsage + delay <= now);
        Long lastUsage = delays.get(player.getUniqueId());
        return lastUsage == null ? 0 : Math.max(1, (lastUsage + delay - now + 999) / 1000);
    }

    /**
     * @return null if WorldGuard is not installed
     */
    @Nullable
    public WorldGuardHook getWorldGuard() {
        return worldGuard;
    }

    @Override
    public void onLoad() {
        // the classes of the hooks are only loaded if their plugin is installed
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                this.worldGuard = WorldGuardHook.register();
            } catch (LinkageError | RuntimeException ex) {
                getLogger().warning("Couldn't hook into WorldGuard: " + ex);
            }
        }
    }

    @Override
    public void onEnable() {
        WoodCutter.instance = this;

        Debug.init(this);
        this.configData = new ConfigData(this);
        this.messageData = new MessageData(this);
        this.updater = new Updater(this);
        registerEvents();

        PluginCommand command = getCommand("woodcutter");
        if (command != null) {
            CommandWoodCutter executor = new CommandWoodCutter(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        hookPlugins();
        hookMetrics();
        Debug.log("If you like the plugin or you have suggestions, please write a review on spigotmc.org!");
    }

    public void reload() {
        Debug.recordAction("Reloading Plugin");
        this.configData.load();
        this.messageData.load();
        this.updater.check();
    }

    private void registerEvents() {
        // the listeners read the config on every event, so they stay registered on a reload
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(new BlockPlaceListener(), this);
        pluginManager.registerEvents(new EnchantListener(this), this);
        pluginManager.registerEvents(new AnvilListener(), this);
        pluginManager.registerEvents(updater, this);
    }

    /**
     * optional plugins (softdepend in the plugin.yml, so they are enabled first)
     */
    private void hookPlugins() {
        if (worldGuard != null) Debug.log("Hooked into WorldGuard: region flag '" + WorldGuardHook.FLAG + "'");
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                if (new PlaceholderHook(this).register()) Debug.log("Hooked into PlaceholderAPI");
            } catch (LinkageError | RuntimeException ex) {
                Debug.warn("Couldn't hook into PlaceholderAPI: " + ex);
            }
        }
    }

    /**
     * anonymous statistics on bstats.org - custom charts only show up after they have been added to the plugin page
     * there with the same id
     */
    private void hookMetrics() {
        if (isOffline()) return;
        Metrics metrics = new Metrics(this, 3789);
        // existing charts
        metrics.addCustomChart(new SimplePie("using_easyprefix", () -> Bukkit.getPluginManager().isPluginEnabled("EasyPrefix") ? "installed" : "not installed"));
        metrics.addCustomChart(new SimplePie("destroy_fake_trees", () -> Boolean.toString(getConfigData().getBoolean(ConfigData.Keys.DESTROY_FAKE_TREES))));

        // settings
        metrics.addCustomChart(new SimplePie("require_sneaking", () -> enabled(ConfigData.Keys.REQUIRE_SNEAKING)));
        metrics.addCustomChart(new SimplePie("delay", () -> !getConfigData().getBoolean(ConfigData.Keys.DELAY) ? "disabled"
                : Statistics.range(getConfigData().getInt(ConfigData.Keys.DELAY_TIME), 10, 30, 60, 300) + "s"));
        metrics.addCustomChart(new SimplePie("delay_message", () -> getConfigData().getString(ConfigData.Keys.DELAY_MSG).isEmpty() ? "disabled" : "enabled"));
        metrics.addCustomChart(new SimplePie("max_blocks", () -> Statistics.range(getConfigData().getInt(ConfigData.Keys.MAX_BLOCKS), 50, 100, 120, 250, 500)));
        metrics.addCustomChart(new SimplePie("max_level", () -> Integer.toString(WoodCutterEnchantment.maxLevel())));
        metrics.addCustomChart(new SimplePie("enchanting_level", () -> Integer.toString(getConfigData().getInt(ConfigData.Keys.MIN_LEVEL))));
        metrics.addCustomChart(new SimplePie("enchanting_chance", () -> Statistics.range((int) Math.ceil(getConfigData().getDouble(ConfigData.Keys.CHANCE)), 10, 25, 50, 75, 99) + "%"));
        metrics.addCustomChart(new SimplePie("disabled_worlds", () -> Statistics.range(getConfigData().getList(ConfigData.Keys.DISABLED_WORLDS).size(), 1, 2, 5, 10)));
        metrics.addCustomChart(new SimplePie("goodie", () -> getConfigData().getString(ConfigData.Keys.GOODIE).toLowerCase(Locale.ROOT)));
        metrics.addCustomChart(new SimplePie("animation", () -> !getConfigData().getBoolean(ConfigData.Keys.ANIMATION) ? "disabled"
                : Statistics.range(getConfigData().getInt(ConfigData.Keys.ANIMATION_BLOCKS_PER_TICK), 1, 2, 4, 8) + " per tick"));
        metrics.addCustomChart(new SimplePie("fast_leaf_decay", () -> enabled(ConfigData.Keys.FAST_LEAF_DECAY)));
        metrics.addCustomChart(new SimplePie("replant", () -> !getConfigData().getBoolean(ConfigData.Keys.REPLANT) ? "disabled"
                : getConfigData().getBoolean(ConfigData.Keys.REPLANT_FREE) ? "free" : "from inventory"));
        metrics.addCustomChart(new SimplePie("drops_to_inventory", () -> enabled(ConfigData.Keys.DROPS_TO_INVENTORY)));
        metrics.addCustomChart(new SimplePie("bonus_drops", () -> Statistics.range((int) Math.ceil(getConfigData().getDouble(ConfigData.Keys.BONUS_DROPS_CHANCE)), 5, 10, 25, 50) + "%"));
        metrics.addCustomChart(new SimplePie("costs", () -> {
            boolean hunger = getConfigData().getInt(ConfigData.Keys.COSTS_HUNGER) > 0;
            boolean experience = getConfigData().getInt(ConfigData.Keys.COSTS_EXPERIENCE) > 0;
            return hunger && experience ? "hunger and experience" : hunger ? "hunger" : experience ? "experience" : "none";
        }));
        metrics.addCustomChart(new SimplePie("folia", () -> TaskManager.isFolia() ? "yes" : "no"));

        // usage since the last report
        metrics.addCustomChart(new SingleLineChart("trees_cut", Statistics::takeTrees));
        metrics.addCustomChart(new SingleLineChart("logs_cut", Statistics::takeLogs));
        metrics.addCustomChart(new SingleLineChart("enchantments", Statistics::takeEnchantments));
        metrics.addCustomChart(new AdvancedPie("tree_types", Statistics::takeTreeTypes));
        metrics.addCustomChart(new AdvancedPie("axes", Statistics::takeAxes));
        metrics.addCustomChart(new AdvancedPie("enchantment_sources", Statistics::takeEnchantmentSources));
        metrics.addCustomChart(new AdvancedPie("enchantment_levels", Statistics::takeEnchantmentLevels));
        metrics.addCustomChart(new AdvancedPie("usage", Statistics::takeUsage));

        // other plugins that matter for the compatibility
        metrics.addCustomChart(new AdvancedPie("related_plugins", () -> {
            Map<String, Integer> plugins = new HashMap<>();
            for (String name : RELATED_PLUGINS) {
                if (getServer().getPluginManager().isPluginEnabled(name)) plugins.put(name, 1);
            }
            return plugins;
        }));
    }

    private String enabled(String key) {
        return getConfigData().getBoolean(key) ? "enabled" : "disabled";
    }

}
