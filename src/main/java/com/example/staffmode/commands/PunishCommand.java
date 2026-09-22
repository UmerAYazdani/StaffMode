package com.example.staffmode.commands;

import com.example.staffmode.gui.StaffPunishGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles /punish <player> and opens the punishment GUI for an online or previously joined player.
 */
public class PunishCommand implements CommandExecutor, TabCompleter {

    private final StaffPunishGui staffPunishGui;

    // Stores the punishment GUI used by /punish.
    public PunishCommand(StaffPunishGui staffPunishGui) {
        this.staffPunishGui = staffPunishGui;
    }

    // Handles execution of this command and validates sender, permissions, and arguments.
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("staffmode.gui")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /punish <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        staffPunishGui.openStaffMainMenu(player, target);
        return true;
    }

    // Returns tab-completion suggestions for this command.
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
        }

        return completions;
    }
}
