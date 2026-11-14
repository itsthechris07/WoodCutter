package net.milkbowl.vault.economy;

import org.bukkit.OfflinePlayer;

/**
 * WoodCutter 2026.
 * <p>
 * The methods of the economy of Vault that WoodCutter calls - only for the tests, WoodCutter finds the economy by the
 * name of this interface. The real one returns an EconomyResponse instead of a boolean.
 *
 * @author Christian34
 */
public interface Economy {

    boolean has(OfflinePlayer player, double amount);

    boolean withdrawPlayer(OfflinePlayer player, double amount);

    boolean depositPlayer(OfflinePlayer player, double amount);

    String format(double amount);

}
