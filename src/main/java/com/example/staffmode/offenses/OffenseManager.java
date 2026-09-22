package com.example.staffmode.offenses;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Stores moderation offenses in YAML files, removes offenses, and alerts staff when a player with past offenses joins.
 */
public class OffenseManager {

    private final File offensesFolder;

    // Stores the plugin reference and prepares access to the offenses folder.
    public OffenseManager(JavaPlugin plugin) {
        this.offensesFolder = new File(plugin.getDataFolder(), "offenses");
    }

    // Creates this feature's data folder if it does not already exist.
    public void createFolders() {
        if (!offensesFolder.exists()) {
            offensesFolder.mkdirs();
        }
    }

    // Returns the YAML file containing the selected player's stored offenses.
    public File getOffenseFile(OfflinePlayer target) {
        return new File(offensesFolder, target.getUniqueId() + ".yml");
    }

    // Appends a new punishment record to the target player's offense history.
    public void addOffense(Player staff, OfflinePlayer target, String type, String reason, String duration) {
        File file = getOffenseFile(target);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        int id = config.getInt("count", 0) + 1;

        config.set("count", id);
        config.set("offenses." + id + ".type", type);
        config.set("offenses." + id + ".duration", duration);
        config.set("offenses." + id + ".reason", reason);
        config.set("offenses." + id + ".staff", staff.getName());
        config.set("offenses." + id + ".time", new Date().toString());

        save(config, file);
    }

    // Deletes one offense and shifts later offense IDs down so the list stays sequential.
    public void removeOffense(OfflinePlayer target, int offenseId) {
        File file = getOffenseFile(target);

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = config.getInt("count", 0);

        if (offenseId <= 0 || offenseId > count) {
            return;
        }

        for (int i = offenseId; i < count; i++) {
            config.set("offenses." + i, config.get("offenses." + (i + 1)));
        }

        config.set("offenses." + count, null);
        config.set("count", count - 1);
        save(config, file);
    }

    // Alerts staff with the proper permission when a joining player has stored offenses.
    public void sendJoinAlertIfNeeded(Player joined) {
        File file = getOffenseFile(joined);

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = config.getInt("count", 0);

        if (count <= 0) {
            return;
        }

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("staffmode.alerts")) {
                staff.sendMessage(ChatColor.RED + "[Staff Alert] "
                        + ChatColor.YELLOW + joined.getName()
                        + ChatColor.RED + " joined with "
                        + ChatColor.YELLOW + count
                        + ChatColor.RED + " past offense(s).");
            }
        }
    }

    // Writes the supplied YAML configuration to disk and logs an error if saving fails.
    private void save(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
