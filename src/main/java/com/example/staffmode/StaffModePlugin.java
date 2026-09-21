package com.example.staffmode;

import com.example.staffmode.commands.PunishCommand;
import com.example.staffmode.commands.RandomInspectionTPCommand;
import com.example.staffmode.commands.StaffHoursCommand;
import com.example.staffmode.commands.StaffModeCommand;
import com.example.staffmode.gui.StaffHoursGui;
import com.example.staffmode.gui.StaffPunishGui;
import com.example.staffmode.hours.StaffHoursManager;
import com.example.staffmode.listeners.StaffModeListener;
import com.example.staffmode.offenses.OffenseManager;
import com.example.staffmode.staff.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class StaffModePlugin extends JavaPlugin {

    private StaffModeManager staffModeManager;
    private StaffHoursManager staffHoursManager;
    private OffenseManager offenseManager;
    private StaffPunishGui staffPunishGui;
    private StaffHoursGui staffHoursGui;

    @Override
    public void onEnable() {
        staffHoursManager = new StaffHoursManager(this);
        staffModeManager = new StaffModeManager(this, staffHoursManager);
        offenseManager = new OffenseManager(this);

        staffPunishGui = new StaffPunishGui(this, staffModeManager, staffHoursManager, offenseManager);
        staffHoursGui = new StaffHoursGui(this, staffHoursManager);

        staffHoursManager.setupConfig();
        staffModeManager.createFolders();
        staffHoursManager.createFolders();
        offenseManager.createFolders();

        registerCommands();
        registerListeners();

        staffHoursManager.startAfkChecker(staffModeManager);

        getLogger().info("StaffMode enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (staffModeManager.isInStaffMode(player)) {
                staffHoursManager.endStaffHourSession(player);
            }
        });

        getLogger().info("StaffMode disabled.");
    }

    private void registerCommands() {
        StaffModeCommand staffModeCommand = new StaffModeCommand(staffModeManager);
        registerCommand("staffmode", staffModeCommand, staffModeCommand);

        PunishCommand punishCommand = new PunishCommand(staffPunishGui);
        registerCommand("punish", punishCommand, punishCommand);

        RandomInspectionTPCommand randomInspectionTPCommand = new RandomInspectionTPCommand(staffModeManager);
        registerCommand("RandomInspectionTP", randomInspectionTPCommand, randomInspectionTPCommand);

        StaffHoursCommand staffHoursCommand = new StaffHoursCommand(staffHoursGui);
        registerCommand("staffhours", staffHoursCommand, staffHoursCommand);
    }

    private void registerCommand(String name, Object executor, Object tabCompleter) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
        command.setExecutor((org.bukkit.command.CommandExecutor) executor);
        command.setTabCompleter((org.bukkit.command.TabCompleter) tabCompleter);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new StaffModeListener(this, staffModeManager, staffHoursManager, offenseManager), this);
        Bukkit.getPluginManager().registerEvents(staffPunishGui, this);
        Bukkit.getPluginManager().registerEvents(staffHoursGui, this);
    }
}
