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
import java.util.Random;

public class RandomInspectionTPCommand implements CommandExecutor, TabCompleter {

    private final StaffModeManager staffModeManager;

    public RandomInspectionTPCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("staffmode.randomtp")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (!staffModeManager.isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You must be in staff mode to use this command.");
            return true;
        }

        List<Player> possibleTargets = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                possibleTargets.add(online);
            }
        }

        if (possibleTargets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No other online players to teleport to.");
            return true;
        }

        Player target = possibleTargets.get(new Random().nextInt(possibleTargets.size()));
        player.teleport(target.getLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to random player: " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
