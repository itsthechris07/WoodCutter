package com.christian34.woodcutter.commands;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.utils.Message;
import com.christian34.woodcutter.utils.Statistics;
import com.christian34.woodcutter.utils.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WoodCutter 2026.
 * <p>
 * The command /woodcutter: players switch WoodCutter on and off for themselves, admins reload the files, enchant
 * the axe in their main hand or give enchanted axes and books to players (also from the console, e.g. as a reward).
 *
 * @author Christian34
 */
public class CommandWoodCutter implements TabExecutor {
    public static final String PERMISSION_ADMIN = "woodcutter.admin";
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("reload", "enchant", "give");
    private final WoodCutter instance;

    public CommandWoodCutter(WoodCutter instance) {
        this.instance = instance;
    }

    /**
     * @return an enchanted book for 'book' or an enchanted axe for the name of an axe (e.g. diamond_axe)
     */
    @Nullable
    static ItemStack createItem(String name, int level) {
        Material material = name.equalsIgnoreCase("book") ? Material.ENCHANTED_BOOK : Material.matchMaterial(name);
        if (material == null || material != Material.ENCHANTED_BOOK && !Tag.ITEMS_AXES.isTagged(material)) return null;
        ItemStack item = new ItemStack(material);
        return WoodCutterEnchantment.apply(item, level) ? item : null;
    }

    /**
     * @return the level, 0 if it is not a level of the enchantment
     */
    private static int parseLevel(String input) {
        try {
            int level = Integer.parseInt(input);
            return level >= 1 && level <= WoodCutterEnchantment.maxLevel() ? level : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("toggle") && args.length == 1) {
            toggle(sender);
            return true;
        }
        if (!ADMIN_SUBCOMMANDS.contains(subcommand)) {
            sendHelp(sender);
            return true;
        }
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Message.NO_PERMISSION.send(sender);
            return true;
        }

        if (subcommand.equals("reload") && args.length == 1) {
            instance.reload();
            Message.RELOADED.send(sender);
        } else if (subcommand.equals("enchant") && args.length <= 2) {
            enchant(sender, args.length == 2 ? args[1] : "1");
        } else if (subcommand.equals("give") && args.length >= 2 && args.length <= 4) {
            give(sender, args[1], args.length >= 3 ? args[2] : "book", args.length == 4 ? args[3] : "1");
        } else {
            sendHelp(sender);
        }
        return true;
    }

    private void toggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Message.PLAYERS_ONLY.send(sender);
            return;
        }
        if (!player.hasPermission(BlockBreakListener.PERMISSION_USE)) {
            Message.NO_PERMISSION.send(player);
            return;
        }
        boolean enabled = !BlockBreakListener.isEnabled(player);
        BlockBreakListener.setEnabled(player, enabled);
        (enabled ? Message.TOGGLE_ON : Message.TOGGLE_OFF).send(player);
    }

    private void enchant(CommandSender sender, String levelInput) {
        if (!(sender instanceof Player player)) {
            Message.PLAYERS_ONLY.send(sender);
            return;
        }
        int level = parseLevel(levelInput);
        if (level == 0) {
            Message.INVALID_LEVEL.send(sender, "%max%", Integer.toString(WoodCutterEnchantment.maxLevel()));
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (WoodCutterEnchantment.isAxe(item) && WoodCutterEnchantment.apply(item, level)) {
            player.getInventory().setItemInMainHand(item);
            Statistics.recordEnchantment("command", level);
            Message.ENCHANT_SUCCESS.send(player);
        } else {
            Message.ENCHANT_FAILED.send(player);
        }
    }

    private void give(CommandSender sender, String name, String itemName, String levelInput) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Message.PLAYER_NOT_ONLINE.send(sender, "%player%", name);
            return;
        }
        int level = parseLevel(levelInput);
        if (level == 0) {
            Message.INVALID_LEVEL.send(sender, "%max%", Integer.toString(WoodCutterEnchantment.maxLevel()));
            return;
        }
        ItemStack item = createItem(itemName, level);
        if (item == null) {
            Message.NO_AXE.send(sender, "%item%", itemName);
            return;
        }
        // the console does not own the player on Folia
        TaskManager.runNowOrLater(target, () -> {
            for (ItemStack leftover : target.getInventory().addItem(item).values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }
            (item.getType() == Material.ENCHANTED_BOOK ? Message.GIVE_RECEIVED_BOOK : Message.GIVE_RECEIVED_AXE).send(target);
        });
        Statistics.recordEnchantment("give", level);
        Message.GIVE_SENT.send(sender, "%player%", target.getName());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8----------=== §9Wood§7Cutter §8===----------");
        sender.sendMessage(" ");
        Message.HELP_TOGGLE.send(sender);
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            Message.HELP_RELOAD.send(sender);
            Message.HELP_ENCHANT.send(sender);
            Message.HELP_GIVE.send(sender);
        }
        sender.sendMessage(" ");
        sender.sendMessage("§8-------------------------------------");
        sender.sendMessage(" ");
        sender.sendMessage("§7Version: " + instance.getPluginMeta().getVersion());
        sender.sendMessage("§7WoodCutter by §9§lChristian34");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean admin = sender.hasPermission(PERMISSION_ADMIN);
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("toggle");
            if (admin) options.addAll(ADMIN_SUBCOMMANDS);
        } else if (admin && subcommand.equals("give") && args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) options.add(player.getName());
        } else if (admin && subcommand.equals("give") && args.length == 3) {
            options.add("book");
            for (Material axe : Tag.ITEMS_AXES.getValues()) options.add(axe.name().toLowerCase(Locale.ROOT));
        } else if (admin && (subcommand.equals("give") && args.length == 4 || subcommand.equals("enchant") && args.length == 2)) {
            for (int level = 1; level <= WoodCutterEnchantment.maxLevel(); level++) options.add(Integer.toString(level));
        }
        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input)).sorted().toList();
    }

}
