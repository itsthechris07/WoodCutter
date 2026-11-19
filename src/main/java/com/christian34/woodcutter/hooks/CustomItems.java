package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.files.ConfigData;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WoodCutter 2026.
 * <p>
 * The items of other plugins that are axes for WoodCutter (setting 'custom-axes'): itemsadder:&lt;namespace&gt;:&lt;id&gt;,
 * nexo:&lt;id&gt;, oraxen:&lt;id&gt; and mmoitems:&lt;type&gt;:&lt;id&gt;. The plugins are asked through their api by
 * reflection, so none of them is needed to build or run WoodCutter.
 *
 * @author Christian34
 */
public final class CustomItems {
    /**
     * the methods of the apis, null if the plugin is not installed - looked up once
     */
    private static final Map<String, Lookup> LOOKUPS = new ConcurrentHashMap<>();

    private CustomItems() {
    }

    /**
     * @return the id of the item in the format of the setting, e.g. nexo:onyx_axe - null if it is no item of these
     * plugins
     */
    @Nullable
    public static String id(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String id = call("Nexo", "com.nexomc.nexo.api.NexoItems", "idFromItem", item);
        if (id != null) return "nexo:" + id;
        id = call("Oraxen", "io.th0rgal.oraxen.api.OraxenItems", "getIdByItem", item);
        if (id != null) return "oraxen:" + id;
        id = itemsAdder(item);
        if (id != null) return "itemsadder:" + id;
        String type = call("MMOItems", "net.Indyuce.mmoitems.MMOItems", "getTypeName", item);
        id = call("MMOItems", "net.Indyuce.mmoitems.MMOItems", "getID", item);
        if (type != null && id != null) return "mmoitems:" + type + ":" + id;
        return null;
    }

    /**
     * @return 1 if the item is one of the setting 'custom-axes', -1 if only these items are axes (custom-axes.only)
     * and it is none of them, 0 if the setting does not decide it
     */
    public static int isCustomAxe(@Nullable ItemStack item) {
        WoodCutter instance = WoodCutter.getInstance();
        if (instance == null || instance.getConfigData() == null) return 0;
        ConfigData config = instance.getConfigData();
        List<String> axes = config.getList(ConfigData.Keys.CUSTOM_AXES);
        boolean only = config.getBoolean(ConfigData.Keys.CUSTOM_AXES_ONLY);
        if (axes.isEmpty()) return only ? -1 : 0;
        String id = id(item);
        // items of Minecraft by their name, e.g. mace or minecraft:mace
        String vanilla = item == null || item.getType().isAir() ? null : item.getType().getKey().getKey();
        for (String axe : axes) {
            String name = axe.trim();
            if (id != null && name.equalsIgnoreCase(id)) return 1;
            if (id == null && vanilla != null && name.replaceFirst("(?i)^minecraft:", "").equalsIgnoreCase(vanilla)) return 1;
        }
        return only ? -1 : 0;
    }

    @Nullable
    private static String itemsAdder(ItemStack item) {
        Lookup lookup = lookup("ItemsAdder", "dev.lone.itemsadder.api.CustomStack", "byItemStack");
        if (lookup == null) return null;
        try {
            Object stack = lookup.method().invoke(null, item);
            if (stack == null) return null;
            Object id = stack.getClass().getMethod("getNamespacedID").invoke(stack);
            return id == null ? null : id.toString().toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    @Nullable
    private static String call(String plugin, String type, String method, ItemStack item) {
        Lookup lookup = lookup(plugin, type, method);
        if (lookup == null) return null;
        try {
            Object result = lookup.method().invoke(null, item);
            return result == null ? null : result.toString().toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    @Nullable
    private static Lookup lookup(String plugin, String type, String method) {
        if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) return null;
        Lookup lookup = LOOKUPS.computeIfAbsent(type + "#" + method, key -> {
            try {
                ClassLoader loader = Bukkit.getPluginManager().getPlugin(plugin).getClass().getClassLoader();
                return new Lookup(Class.forName(type, true, loader).getMethod(method, ItemStack.class));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                return new Lookup(null);
            }
        });
        return lookup.method() == null ? null : lookup;
    }

    private record Lookup(@Nullable Method method) {
    }

}
