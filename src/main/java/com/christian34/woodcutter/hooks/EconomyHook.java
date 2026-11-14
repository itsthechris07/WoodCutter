package com.christian34.woodcutter.hooks;

import com.christian34.woodcutter.utils.Debug;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * WoodCutter 2026.
 * <p>
 * The economy of Vault (e.g. EssentialsX or CMI): money for cut trees and trees that cost money. The economy is
 * found through the services of the server by the name of its class, so WoodCutter needs no classes of Vault and
 * works without it. The economy plugin can be enabled after WoodCutter, so it is looked up whenever it is needed.
 *
 * @author Christian34
 */
public final class EconomyHook {
    private static final String ECONOMY = "net.milkbowl.vault.economy.Economy";
    private static volatile boolean warned;

    private EconomyHook() {
    }

    /**
     * @return true if an economy plugin is installed
     */
    public static boolean isAvailable() {
        return provider() != null;
    }

    public static boolean has(OfflinePlayer player, double amount) {
        Object result = call("has", player, amount);
        return result instanceof Boolean has && has;
    }

    /**
     * @return true if the money has been taken from the player
     */
    public static boolean withdraw(OfflinePlayer player, double amount) {
        return succeeded(call("withdrawPlayer", player, amount));
    }

    /**
     * @return true if the player has got the money
     */
    public static boolean deposit(OfflinePlayer player, double amount) {
        return succeeded(call("depositPlayer", player, amount));
    }

    /**
     * @return the amount as the economy shows it, e.g. $1.50
     */
    public static String format(double amount) {
        Object provider = provider();
        if (provider != null) {
            try {
                Object text = economyClass(provider).getMethod("format", double.class).invoke(provider, amount);
                if (text instanceof String formatted) return formatted;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // the number then
            }
        }
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(amount);
    }

    @Nullable
    private static Object provider() {
        for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
            if (!service.getName().equals(ECONOMY)) continue;
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(service);
            return registration == null ? null : registration.getProvider();
        }
        return null;
    }

    @Nullable
    private static Object call(String name, OfflinePlayer player, double amount) {
        Object provider = provider();
        if (provider == null) return null;
        try {
            Method method = economyClass(provider).getMethod(name, OfflinePlayer.class, double.class);
            return method.invoke(provider, player, amount);
        } catch (InvocationTargetException ex) {
            warn(name, ex.getCause());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            warn(name, ex);
        }
        return null;
    }

    /**
     * the methods are called through the interface, the class of the economy plugin may not be public
     */
    private static Class<?> economyClass(Object provider) {
        for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
            if (service.getName().equals(ECONOMY) && service.isInstance(provider)) return service;
        }
        return provider.getClass();
    }

    /**
     * the EconomyResponse of Vault
     */
    private static boolean succeeded(@Nullable Object response) {
        if (response == null) return false;
        if (response instanceof Boolean success) return success;
        try {
            return Boolean.TRUE.equals(response.getClass().getMethod("transactionSuccess").invoke(response));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private static void warn(String method, Throwable ex) {
        if (warned) return;
        warned = true;
        Debug.warn("The economy plugin failed (" + method + "): " + ex);
    }

}
