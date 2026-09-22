package com.example.staffmode.commands;

import com.example.staffmode.gui.NotesGui;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles /modnotes.
 */
public class ModNotesCommand
        implements CommandExecutor, TabCompleter {

    private final NotesGui notesGui;

    public ModNotesCommand(
            NotesGui notesGui
    ) {

        this.notesGui = notesGui;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Only players can use this command."
            );

            return true;
        }

        if (!player.hasPermission(
                "staffmode.notes.mod.view"
        )) {

            player.sendMessage(
                    ChatColor.RED
                            + "You do not have permission to view moderation notes."
            );

            return true;
        }

        notesGui.openModNotes(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        return new ArrayList<>();
    }
}