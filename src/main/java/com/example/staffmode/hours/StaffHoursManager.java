package com.example.staffmode.hours;

import com.example.staffmode.staff.StaffModeManager;
import com.example.staffmode.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles staff-hour tracking.
 *
 * This manager:
 * - Starts and stops staff-hour sessions
 * - Saves staff hours to YAML files
 * - Detects AFK staff
 * - Pauses hours while AFK
 * - Resumes hours when staff become active
 * - Calculates total staff hours
 */
public class StaffHoursManager {

    private final JavaPlugin plugin;
    private final File staffHoursFolder;

    private final Map<UUID, Long> lastStaffActivity = new HashMap<>();
    private final Set<UUID> staffHoursAfk = new HashSet<>();

    /**
     * Creates the staff hours manager.
     */
    public StaffHoursManager(JavaPlugin plugin) {
        this.plugin = plugin;

        staffHoursFolder = new File(
                plugin.getDataFolder(),
                "staffhours"
        );
    }

    /**
     * Creates the staffhours folder if needed.
     */
    public void createFolders() {
        if (!staffHoursFolder.exists()) {
            staffHoursFolder.mkdirs();
        }
    }

    /**
     * Creates the default staff-hour configuration.
     */
    public void setupConfig() {

        plugin.getConfig().addDefault(
                "staff-hours.afk-timeout-seconds",
                120
        );

        plugin.getConfig().addDefault(
                "staff-hours.afk-message",
                "&cYou are now marked as AFK. Staff hours are no longer being logged."
        );

        plugin.getConfig().addDefault(
                "staff-hours.active-again-message",
                "&aYou are active again. Staff hours are now being logged."
        );

        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    /**
     * Returns the staff hours folder.
     */
    public File getStaffHoursFolder() {
        return staffHoursFolder;
    }

    /**
     * Returns one staff member's hour file.
     */
    public File getStaffHoursFile(UUID uuid) {
        return new File(
                staffHoursFolder,
                uuid + ".yml"
        );
    }

    /**
     * Starts counting staff hours.
     */
    public void startStaffHourSession(Player player) {

        File file = getStaffHoursFile(
                player.getUniqueId()
        );

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        // A session is already running.
        if (config.getLong("current-start", 0) > 0) {
            return;
        }

        config.set("name", player.getName());
        config.set(
                "current-start",
                System.currentTimeMillis()
        );

        save(config, file);
    }

    /**
     * Ends the currently running staff-hour session.
     */
    public void endStaffHourSession(Player player) {

        File file = getStaffHoursFile(
                player.getUniqueId()
        );

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        long start =
                config.getLong("current-start", 0);

        if (start <= 0) {
            return;
        }

        long end = System.currentTimeMillis();

        int count =
                config.getInt("sessions.count", 0) + 1;

        config.set("sessions.count", count);

        config.set(
                "sessions." + count + ".start",
                start
        );

        config.set(
                "sessions." + count + ".end",
                end
        );

        config.set("current-start", null);
        config.set("name", player.getName());

        save(config, file);
    }

    /**
     * Starts tracking a staff member immediately.
     */
    public void startTrackingNow(Player player) {

        UUID uuid = player.getUniqueId();

        lastStaffActivity.put(
                uuid,
                System.currentTimeMillis()
        );

        staffHoursAfk.remove(uuid);

        startStaffHourSession(player);
    }

    /**
     * Completely stops tracking the player.
     */
    public void stopTracking(Player player) {

        endStaffHourSession(player);

        UUID uuid = player.getUniqueId();

        lastStaffActivity.remove(uuid);
        staffHoursAfk.remove(uuid);
    }

    /**
     * Marks a staff member as active.
     *
     * If they were AFK, their hour tracking resumes.
     */
    public void markStaffActive(
            Player player,
            StaffModeManager staffModeManager
    ) {

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        lastStaffActivity.put(
                uuid,
                System.currentTimeMillis()
        );

        if (staffHoursAfk.contains(uuid)) {

            staffHoursAfk.remove(uuid);

            startStaffHourSession(player);

            sendConfigMessage(
                    player,
                    "staff-hours.active-again-message"
            );
        }
    }

    /**
     * Checks regularly for staff who have gone AFK.
     */
    public void startAfkChecker(
            StaffModeManager staffModeManager
    ) {

        Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {

                    long timeoutMillis =
                            plugin.getConfig().getLong(
                                    "staff-hours.afk-timeout-seconds",
                                    120
                            ) * 1000L;

                    long now = System.currentTimeMillis();

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        UUID uuid = player.getUniqueId();

                        // Player is not in staff mode anymore.
                        if (!staffModeManager.isInStaffMode(player)) {

                            lastStaffActivity.remove(uuid);
                            staffHoursAfk.remove(uuid);

                            continue;
                        }

                        long lastActive =
                                lastStaffActivity.getOrDefault(
                                        uuid,
                                        now
                                );

                        // Player has passed the AFK timeout.
                        if (now - lastActive >= timeoutMillis
                                && !staffHoursAfk.contains(uuid)) {

                            endStaffHourSession(player);

                            staffHoursAfk.add(uuid);

                            sendConfigMessage(
                                    player,
                                    "staff-hours.afk-message"
                            );
                        }
                    }
                },

                // Start after 10 seconds.
                20L * 10,

                // Repeat every 10 seconds.
                20L * 10
        );
    }

    /**
     * Gets the total tracked time for a specific month.
     */
    public long getMonthTotalMillis(
            UUID uuid,
            int year,
            int month
    ) {

        YearMonth yearMonth =
                YearMonth.of(year, month);

        Instant start =
                yearMonth
                        .atDay(1)
                        .atStartOfDay(
                                ZoneId.systemDefault()
                        )
                        .toInstant();

        Instant end =
                yearMonth
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay(
                                ZoneId.systemDefault()
                        )
                        .toInstant();

        return getTrackedMillis(
                uuid,
                start,
                end
        );
    }

    /**
     * Gets tracked staff time.
     *
     * If rangeStart and rangeEnd are null,
     * this returns all-time hours.
     */
    public long getTrackedMillis(
            UUID uuid,
            Instant rangeStart,
            Instant rangeEnd
    ) {

        File file = getStaffHoursFile(uuid);

        if (!file.exists()) {
            return 0;
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        long total = 0;

        int count =
                config.getInt("sessions.count", 0);

        // Completed sessions.
        for (int i = 1; i <= count; i++) {

            long start =
                    config.getLong(
                            "sessions." + i + ".start",
                            0
                    );

            long end =
                    config.getLong(
                            "sessions." + i + ".end",
                            0
                    );

            if (start <= 0 || end <= 0) {
                continue;
            }

            total += getOverlapMillis(
                    start,
                    end,
                    rangeStart,
                    rangeEnd
            );
        }

        // Currently active session.
        long currentStart =
                config.getLong("current-start", 0);

        if (currentStart > 0) {

            total += getOverlapMillis(
                    currentStart,
                    System.currentTimeMillis(),
                    rangeStart,
                    rangeEnd
            );
        }

        return total;
    }

    /**
     * Calculates how much of a session falls
     * inside the requested time range.
     */
    private long getOverlapMillis(
            long sessionStartMillis,
            long sessionEndMillis,
            Instant rangeStart,
            Instant rangeEnd
    ) {

        // No range means use the whole session.
        if (rangeStart == null || rangeEnd == null) {

            return Math.max(
                    0,
                    sessionEndMillis - sessionStartMillis
            );
        }

        long rangeStartMillis =
                rangeStart.toEpochMilli();

        long rangeEndMillis =
                rangeEnd.toEpochMilli();

        long start =
                Math.max(
                        sessionStartMillis,
                        rangeStartMillis
                );

        long end =
                Math.min(
                        sessionEndMillis,
                        rangeEndMillis
                );

        return Math.max(
                0,
                end - start
        );
    }

    /**
     * Converts milliseconds to an easy-to-read format.
     *
     * Example:
     * 3h 42m
     */
    public String formatDuration(long millis) {

        long totalMinutes =
                millis / 1000 / 60;

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours + "h " + minutes + "m";
    }

    /**
     * Sends a configurable message to a staff member.
     */
    private void sendConfigMessage(
            Player player,
            String path
    ) {

        String message =
                plugin.getConfig().getString(
                        path,
                        ""
                );

        if (message == null || message.isBlank()) {
            return;
        }

        player.sendMessage(
                ItemUtil.color(message)
        );
    }

    /**
     * Saves a YAML configuration file.
     */
    private void save(
            YamlConfiguration config,
            File file
    ) {

        try {
            config.save(file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}