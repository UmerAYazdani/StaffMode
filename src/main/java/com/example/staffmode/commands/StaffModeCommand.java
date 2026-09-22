package com.example.staffmode.commands;

import com.example.staffmode.staff.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles /staffmode and /sm, including self toggle, build/drop/pickup subcommands, other-player toggles, invalid-command handling, and tab completion.
 */
public class StaffModeCommand implements CommandExecutor, TabCompleter {

    private final StaffModeManager staffModeManager;

    // Stores the staff-mode manager used by /sm.
    public StaffModeCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    // Handles execution of this command and validates sender, permissions, and arguments.
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("staffmode.use")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            staffModeManager.toggleSelf(player);
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("drop")) {
                return staffModeManager.toggleDrop(player);
            }

            if (args[0].equalsIgnoreCase("pickup")) {
                return staffModeManager.togglePickup(player);
            }

            if (args[0].equalsIgnoreCase("build")) {
                return staffModeManager.toggleBuild(player);
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "Not a valid staff mode command.");
                player.sendMessage(ChatColor.YELLOW + "Valid commands: /sm, /sm build");
                return true;
            }

            if (!player.hasPermission("staffmode.others")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            staffModeManager.toggleOther(player, target);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Not a valid staff mode command.");
        player.sendMessage(ChatColor.YELLOW + "Valid commands: /sm, /sm build");
        return true;
    }

    // Returns tab-completion suggestions for this command.
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 1) {
            if (player.hasPermission("staffmode.drop")) {
                completions.add("drop");
            }

            if (player.hasPermission("staffmode.pickup")) {
                completions.add("pickup");
            }

            if (player.hasPermission("staffmode.others")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            }

            if (player.hasPermission("staffmode.build.*")
                    || player.hasPermission("staffmode.build." + player.getWorld().getName())) {
                completions.add("build");
            }
        }

        return completions;
    }
}
