package com.example.staffmode;

import com.example.staffmode.commands.ModNotesCommand;
import com.example.staffmode.commands.PunishCommand;
import com.example.staffmode.commands.RandomInspectionTPCommand;
import com.example.staffmode.commands.StaffHoursCommand;
import com.example.staffmode.commands.StaffModeCommand;
import com.example.staffmode.commands.StaffNotesCommand;

import com.example.staffmode.gui.NotesGui;
import com.example.staffmode.gui.StaffHoursGui;
import com.example.staffmode.gui.StaffPunishGui;

import com.example.staffmode.hours.StaffHoursManager;
import com.example.staffmode.listeners.StaffModeListener;
import com.example.staffmode.notes.StaffNotesManager;
import com.example.staffmode.offenses.OffenseManager;
import com.example.staffmode.staff.StaffModeManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main class for StaffMode.
 */
public class StaffModePlugin extends JavaPlugin {

    private StaffModeManager staffModeManager;
    private StaffHoursManager staffHoursManager;
    private OffenseManager offenseManager;

    private StaffNotesManager staffNotesManager;

    private StaffPunishGui staffPunishGui;
    private StaffHoursGui staffHoursGui;
    private NotesGui notesGui;

    @Override
    public void onEnable() {

        /*
         * Create managers.
         */
        staffHoursManager =
                new StaffHoursManager(this);

        staffModeManager =
                new StaffModeManager(
                        this,
                        staffHoursManager
                );

        offenseManager =
                new OffenseManager(this);

        staffNotesManager =
                new StaffNotesManager(this);

        /*
         * Create GUIs.
         */
        staffPunishGui =
                new StaffPunishGui(
                        this,
                        staffModeManager,
                        staffHoursManager,
                        offenseManager
                );

        staffHoursGui =
                new StaffHoursGui(
                        this,
                        staffHoursManager
                );

        notesGui =
                new NotesGui(
                        this,
                        staffNotesManager,
                        staffModeManager
                );

        /*
         * Create configuration defaults.
         */
        staffHoursManager.setupConfig();

        /*
         * Create plugin folders.
         */
        staffModeManager.createFolders();

        staffHoursManager.createFolders();

        offenseManager.createFolders();

        staffNotesManager.createFolders();

        /*
         * Register commands.
         */
        registerCommands();

        /*
         * Register event listeners.
         */
        registerListeners();

        /*
         * Start staff-hours AFK detection.
         */
        staffHoursManager.startAfkChecker(
                staffModeManager
        );

        getLogger().info(
                "StaffMode enabled."
        );
    }

    @Override
    public void onDisable() {

        /*
         * Save active staff-hour sessions
         * before the plugin shuts down.
         */
        if (staffModeManager != null
                && staffHoursManager != null) {

            Bukkit
                    .getOnlinePlayers()
                    .forEach(player -> {

                        if (staffModeManager
                                .isInStaffMode(player)) {

                            staffHoursManager
                                    .endStaffHourSession(player);
                        }
                    });
        }

        getLogger().info(
                "StaffMode disabled."
        );
    }

    /**
     * Registers all commands.
     */
    private void registerCommands() {

        /*
         * /staffmode
         * /sm
         */
        StaffModeCommand staffModeCommand =
                new StaffModeCommand(
                        staffModeManager
                );

        registerCommand(
                "staffmode",
                staffModeCommand,
                staffModeCommand
        );

        /*
         * /punish
         */
        PunishCommand punishCommand =
                new PunishCommand(
                        staffPunishGui
                );

        registerCommand(
                "punish",
                punishCommand,
                punishCommand
        );

        /*
         * /RandomInspectionTP
         * /rit
         */
        RandomInspectionTPCommand randomInspectionTPCommand =
                new RandomInspectionTPCommand(
                        staffModeManager
                );

        registerCommand(
                "RandomInspectionTP",
                randomInspectionTPCommand,
                randomInspectionTPCommand
        );

        /*
         * /staffhours
         * /sh
         */
        StaffHoursCommand staffHoursCommand =
                new StaffHoursCommand(
                        staffHoursGui
                );

        registerCommand(
                "staffhours",
                staffHoursCommand,
                staffHoursCommand
        );

        /*
         * /staffnotes
         */
        StaffNotesCommand staffNotesCommand =
                new StaffNotesCommand(
                        notesGui
                );

        registerCommand(
                "staffnotes",
                staffNotesCommand,
                staffNotesCommand
        );

        /*
         * /modnotes
         */
        ModNotesCommand modNotesCommand =
                new ModNotesCommand(
                        notesGui
                );

        registerCommand(
                "modnotes",
                modNotesCommand,
                modNotesCommand
        );
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {

        /*
         * General staff-mode events.
         */
        Bukkit
                .getPluginManager()
                .registerEvents(
                        new StaffModeListener(
                                this,
                                staffModeManager,
                                staffHoursManager,
                                offenseManager
                        ),
                        this
                );

        /*
         * Punishment GUI.
         */
        Bukkit
                .getPluginManager()
                .registerEvents(
                        staffPunishGui,
                        this
                );

        /*
         * Staff hours GUI.
         */
        Bukkit
                .getPluginManager()
                .registerEvents(
                        staffHoursGui,
                        this
                );

        /*
         * Staff/mod notes GUI.
         */
        Bukkit
                .getPluginManager()
                .registerEvents(
                        notesGui,
                        this
                );
    }

    /**
     * Helper for registering command
     * executors and tab completers.
     */
    private void registerCommand(
            String name,
            CommandExecutor executor,
            TabCompleter tabCompleter
    ) {

        PluginCommand command =
                Objects.requireNonNull(
                        getCommand(name),
                        "Missing command in plugin.yml: "
                                + name
                );

        command.setExecutor(
                executor
        );

        command.setTabCompleter(
                tabCompleter
        );
    }
}