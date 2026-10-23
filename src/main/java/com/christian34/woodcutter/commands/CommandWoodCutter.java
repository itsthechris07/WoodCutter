package com.christian34.woodcutter.commands;

import com.christian34.woodcutter.WoodCutter;
import com.christian34.woodcutter.enchantments.WoodCutterEnchantment;
import com.christian34.woodcutter.listeners.BlockBreakListener;
import com.christian34.woodcutter.menu.Menu;
import com.christian34.woodcutter.skill.Skill;
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
 * The command /woodcutter: players switch WoodCutter on and off for themselves and look at their woodcutting level, admins reload the files, enchant
 * the axe in their main hand or give enchanted axes and books to players (also from the console, e.g. as a reward).
 *
 * @author Christian34
 */
public class CommandWoodCutter implements TabExecutor {
    public static final String PERMISSION_ADMIN = "woodcutter.admin";
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("reload", "enchant", "give");
    private static final List<String> SKILL_SUBCOMMANDS = List.of("level", "top");
    /**
     * the amount of players /woodcutter top shows
     */
    private static final int TOP_ENTRIES = 10;
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
        if (subcommand.equals("menu") && args.length == 1) {
            menu(sender);
            return true;
        }
        if (subcommand.equals("level") && args.length <= 2) {
            level(sender, args.length == 2 ? args[1] : null);
            return true;
        }
        if (subcommand.equals("top") && args.length == 1) {
            top(sender);
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

    /**
     * shows the woodcutting level of the sender with their perks, or the level of another player (also offline)
     */
    private void level(CommandSender sender, @Nullable String name) {
        Skill skill = instance.getSkill();
        if (!skill.isEnabled()) {
            Message.SKILL_DISABLED.send(sender);
            return;
        }
        if (name == null && !(sender instanceof Player)) {
            Message.PLAYERS_ONLY.send(sender);
            return;
        }
        Skill.Entry entry = name == null ? new Skill.Entry(sender.getName(), skill.xp(((Player) sender).getUniqueId())) : skill.find(name);
        if (entry == null) {
            Message.SKILL_NO_DATA.send(sender, "%player%", name);
            return;
        }
        int level = skill.levelOf(entry.xp());
        if (level >= skill.maxLevel()) {
            Message.SKILL_INFO_MAX.send(sender, "%player%", entry.name(), "%level%", Integer.toString(level), "%xp%", Long.toString(entry.xp()));
        } else {
            long reached = skill.xpForLevel(level);
            Message.SKILL_INFO.send(sender, "%player%", entry.name(), "%level%", Integer.toString(level),
                    "%current%", Long.toString(entry.xp() - reached), "%needed%", Long.toString(skill.xpForLevel(level + 1) - reached));
        }
        if (name == null && sender instanceof Player player) skill.describePerks(player).forEach(player::sendMessage);
    }

    private void menu(CommandSender sender) {
        if (sender instanceof Player player) {
            new Menu(instance, player, Menu.Page.MAIN).open();
            Statistics.recordMenu("opened");
        } else {
            Message.PLAYERS_ONLY.send(sender);
        }
    }

    private void top(CommandSender sender) {
        Skill skill = instance.getSkill();
        if (!skill.isEnabled()) {
            Message.SKILL_DISABLED.send(sender);
            return;
        }
        Message.SKILL_TOP_HEADER.send(sender);
        int rank = 1;
        for (Skill.Entry entry : skill.top(TOP_ENTRIES)) {
            Message.SKILL_TOP_ENTRY.send(sender, "%rank%", Integer.toString(rank++), "%player%", entry.name(),
                    "%level%", Integer.toString(skill.levelOf(entry.xp())), "%xp%", Long.toString(entry.xp()));
        }
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
        // a click runs the command, the ones that change something or need arguments are only written into the chat
        Message.HELP_TOGGLE.sendCommand(sender, "/woodcutter toggle", true);
        Message.HELP_MENU.sendCommand(sender, "/woodcutter menu", true);
        if (instance.getSkill().isEnabled()) {
            Message.HELP_LEVEL.sendCommand(sender, "/woodcutter level", true);
            Message.HELP_TOP.sendCommand(sender, "/woodcutter top", true);
        }
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            Message.HELP_RELOAD.sendCommand(sender, "/woodcutter reload", true);
            Message.HELP_ENCHANT.sendCommand(sender, "/woodcutter enchant ", false);
            Message.HELP_GIVE.sendCommand(sender, "/woodcutter give ", false);
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
            options.add("menu");
            if (instance.getSkill().isEnabled()) options.addAll(SKILL_SUBCOMMANDS);
            if (admin) options.addAll(ADMIN_SUBCOMMANDS);
        } else if (subcommand.equals("level") && args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) options.add(player.getName());
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
