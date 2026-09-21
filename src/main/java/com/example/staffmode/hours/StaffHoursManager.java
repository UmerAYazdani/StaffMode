package com.example.staffmode.hours;

import com.example.staffmode.staff.StaffModeManager;
import com.example.staffmode.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
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

public class StaffHoursManager {

    private final JavaPlugin plugin;
    private final File staffHoursFolder;
    private final Map<UUID, Long> lastStaffActivity = new HashMap<>();
    private final Set<UUID> staffHoursAfk = new HashSet<>();

    public StaffHoursManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.staffHoursFolder = new File(plugin.getDataFolder(), "staffhours");
    }

    public void createFolders() {
        if (!staffHoursFolder.exists()) {
            staffHoursFolder.mkdirs();
        }
    }

    public void setupConfig() {
        plugin.getConfig().addDefault("staff-hours.afk-timeout-seconds", 120);
        plugin.getConfig().addDefault("staff-hours.afk-message", "&cYou are now marked as AFK. Staff hours are no longer being logged.");
        plugin.getConfig().addDefault("staff-hours.active-again-message", "&aYou are active again. Staff hours are now being logged.");
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public File getStaffHoursFolder() {
        return staffHoursFolder;
    }

    public File getStaffHoursFile(UUID uuid) {
        return new File(staffHoursFolder, uuid + ".yml");
    }

    public void startStaffHourSession(Player player) {
        File file = getStaffHoursFile(player.getUniqueId());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.getLong("current-start", 0) > 0) {
            return;
        }

        config.set("name", player.getName());
        config.set("current-start", System.currentTimeMillis());
        save(config, file);
    }

    public void endStaffHourSession(Player player) {
        File file = getStaffHoursFile(player.getUniqueId());

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        long start = config.getLong("current-start", 0);

        if (start <= 0) {
            return;
        }

        long end = System.currentTimeMillis();
        int count = config.getInt("sessions.count", 0) + 1;

        config.set("sessions.count", count);
        config.set("sessions." + count + ".start", start);
        config.set("sessions." + count + ".end", end);
        config.set("current-start", null);
        config.set("name", player.getName());

        save(config, file);
    }

    public void startTrackingNow(Player player) {
        lastStaffActivity.put(player.getUniqueId(), System.currentTimeMillis());
        staffHoursAfk.remove(player.getUniqueId());
        startStaffHourSession(player);
    }

    public void stopTracking(Player player) {
        endStaffHourSession(player);
        lastStaffActivity.remove(player.getUniqueId());
        staffHoursAfk.remove(player.getUniqueId());
    }

    public void markStaffActive(Player player, StaffModeManager staffModeManager) {
        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        lastStaffActivity.put(uuid, System.currentTimeMillis());

        if (staffHoursAfk.contains(uuid)) {
            staffHoursAfk.remove(uuid);
            startStaffHourSession(player);
            sendConfigMessage(player, "staff-hours.active-again-message");
        }
    }

    public void startAfkChecker(StaffModeManager staffModeManager) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long timeoutMillis = plugin.getConfig().getLong("staff-hours.afk-timeout-seconds", 120) * 1000L;
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                if (!staffModeManager.isInStaffMode(player)) {
                    lastStaffActivity.remove(uuid);
                    staffHoursAfk.remove(uuid);
                    continue;
                }

                long lastActive = lastStaffActivity.getOrDefault(uuid, now);

                if (now - lastActive >= timeoutMillis && !staffHoursAfk.contains(uuid)) {
                    endStaffHourSession(player);
                    staffHoursAfk.add(uuid);
                    sendConfigMessage(player, "staff-hours.afk-message");
                }
            }
        }, 20L * 10, 20L * 10);
    }

    public long getMonthTotalMillis(UUID uuid, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        Instant start = yearMonth.atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant end = yearMonth.plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        return getTrackedMillis(uuid, start, end);
    }

    public long getTrackedMillis(UUID uuid, Instant rangeStart, Instant rangeEnd) {
        File file = getStaffHoursFile(uuid);

        if (!file.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        long total = 0;
        int count = config.getInt("sessions.count", 0);

        for (int i = 1; i <= count; i++) {
            long start = config.getLong("sessions." + i + ".start", 0);
            long end = config.getLong("sessions." + i + ".end", 0);

            if (start <= 0 || end <= 0) {
                continue;
            }

            total += getOverlapMillis(start, end, rangeStart, rangeEnd);
        }

        long currentStart = config.getLong("current-start", 0);

        if (currentStart > 0) {
            total += getOverlapMillis(currentStart, System.currentTimeMillis(), rangeStart, rangeEnd);
        }

        return total;
    }

    private long getOverlapMillis(long sessionStartMillis, long sessionEndMillis, Instant rangeStart, Instant rangeEnd) {
        if (rangeStart == null || rangeEnd == null) {
            return Math.max(0, sessionEndMillis - sessionStartMillis);
        }

        long rangeStartMillis = rangeStart.toEpochMilli();
        long rangeEndMillis = rangeEnd.toEpochMilli();
        long start = Math.max(sessionStartMillis, rangeStartMillis);
        long end = Math.min(sessionEndMillis, rangeEndMillis);

        return Math.max(0, end - start);
    }

    public String formatDuration(long millis) {
        long totalMinutes = millis / 1000 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private void sendConfigMessage(Player player, String path) {
        String message = plugin.getConfig().getString(path, "");

        if (message == null || message.isBlank()) {
            return;
        }

        player.sendMessage(ItemUtil.color(message));
    }

    private void save(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
