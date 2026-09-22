package com.example.staffmode.notes;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages the two shared note logs:
 *
 * 1. General Staff Notes
 * 2. Moderation Notes
 */
public class StaffNotesManager {

    private final JavaPlugin plugin;

    private final File notesFolder;
    private final File staffNotesFile;
    private final File modNotesFile;

    public StaffNotesManager(JavaPlugin plugin) {

        this.plugin = plugin;

        this.notesFolder = new File(
                plugin.getDataFolder(),
                "notes"
        );

        this.staffNotesFile = new File(
                notesFolder,
                "staff-notes.yml"
        );

        this.modNotesFile = new File(
                notesFolder,
                "mod-notes.yml"
        );
    }

    /**
     * Creates the notes folder.
     */
    public void createFolders() {

        if (!notesFolder.exists()) {
            notesFolder.mkdirs();
        }
    }

    /**
     * Adds a general staff progress note.
     */
    public void addStaffNote(
            Player staff,
            String text
    ) {

        addNote(
                staffNotesFile,
                staff,
                text
        );
    }

    /**
     * Adds a moderation note.
     */
    public void addModNote(
            Player staff,
            String text
    ) {

        addNote(
                modNotesFile,
                staff,
                text
        );
    }

    /**
     * Loads the shared general staff notes.
     */
    public YamlConfiguration getStaffNotes() {

        return YamlConfiguration.loadConfiguration(
                staffNotesFile
        );
    }

    /**
     * Loads the shared moderation notes.
     */
    public YamlConfiguration getModNotes() {

        return YamlConfiguration.loadConfiguration(
                modNotesFile
        );
    }

    /**
     * Shared method used to save either type of note.
     */
    private void addNote(
            File file,
            Player staff,
            String text
    ) {

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        int id =
                config.getInt(
                        "count",
                        0
                ) + 1;

        String path =
                "entries." + id;

        config.set(
                "count",
                id
        );

        config.set(
                path + ".staff",
                staff.getName()
        );

        config.set(
                path + ".uuid",
                staff.getUniqueId().toString()
        );

        config.set(
                path + ".world",
                staff.getWorld().getName()
        );

        config.set(
                path + ".text",
                text
        );

        config.set(
                path + ".timestamp",
                System.currentTimeMillis()
        );

        config.set(
                path + ".date",
                new SimpleDateFormat(
                        "MM/dd/yyyy hh:mm a"
                ).format(new Date())
        );

        save(
                config,
                file
        );
    }

    /**
     * Saves a YAML file.
     */
    private void save(
            YamlConfiguration config,
            File file
    ) {

        try {

            config.save(file);

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Could not save notes file: "
                            + file.getName()
            );

            exception.printStackTrace();
        }
    }
}