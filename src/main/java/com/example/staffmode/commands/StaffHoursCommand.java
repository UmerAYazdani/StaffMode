package com.example.staffmode.commands;

import com.example.staffmode.gui.StaffHoursGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StaffHoursCommand implements CommandExecutor, TabCompleter {

    private final StaffHoursGui staffHoursGui;

    public StaffHoursCommand(StaffHoursGui staffHoursGui) {
        this.staffHoursGui = staffHoursGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("staffmode.hours")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        staffHoursGui.openStaffHoursPlayersMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
