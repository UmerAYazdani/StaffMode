package com.example.staffmode.managers;

import com.example.staffmode.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;

public class StaffHoursManager {

    private final JavaPlugin plugin;
    private final File staffHoursFolder;

    private final Map<UUID, Long> lastStaffActivity = new HashMap<>();
    private final Set<UUID> staffHoursAfk = new HashSet<>();

    public StaffHoursManager(JavaPlugin plugin, File staffHoursFolder) {
        this.plugin = plugin;
        this.staffHoursFolder = staffHoursFolder;
    }

    public File getStaffHoursFile(UUID uuid) {
        return new File(staffHoursFolder, uuid + ".yml");
    }

    public void markInitialActive(Player player) {
        lastStaffActivity.put(player.getUniqueId(), System.currentTimeMillis());
        staffHoursAfk.remove(player.getUniqueId());
    }

    public void clearTracking(Player player) {
        lastStaffActivity.remove(player.getUniqueId());
        staffHoursAfk.remove(player.getUniqueId());
    }

    public void startSession(Player player) {
        File file = getStaffHoursFile(player.getUniqueId());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.getLong("current-start", 0) > 0) {
            return;
        }

        config.set("name", player.getName());
        config.set("current-start", System.currentTimeMillis());
        save(file, config);
    }

    public void endSession(Player player) {
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
        save(file, config);
    }

    public void markActive(Player player, PlayerDataManager playerDataManager) {
        if (!playerDataManager.isInStaffMode(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        lastStaffActivity.put(uuid, System.currentTimeMillis());

        if (staffHoursAfk.contains(uuid)) {
            staffHoursAfk.remove(uuid);
            startSession(player);
            sendConfigMessage(player, "staff-hours.active-again-message");
        }
    }

    public void startAfkChecker(PlayerDataManager playerDataManager) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long timeoutMillis = plugin.getConfig().getLong("staff-hours.afk-timeout-seconds", 120) * 1000L;
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                if (!playerDataManager.isInStaffMode(player)) {
                    clearTracking(player);
                    continue;
                }

                long lastActive = lastStaffActivity.getOrDefault(uuid, now);

                if (now - lastActive >= timeoutMillis && !staffHoursAfk.contains(uuid)) {
                    endSession(player);
                    staffHoursAfk.add(uuid);
                    sendConfigMessage(player, "staff-hours.afk-message");
                }
            }
        }, 20L * 10, 20L * 10);
    }

    public List<UUID> getTrackedStaffUuids() {
        File[] files = staffHoursFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        List<UUID> staffUuids = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    staffUuids.add(UUID.fromString(file.getName().replace(".yml", "")));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        staffUuids.sort(Comparator.comparing(uuid -> getName(uuid).toLowerCase()));
        return staffUuids;
    }

    public String getName(UUID uuid) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(getStaffHoursFile(uuid));
        return config.getString("name", Bukkit.getOfflinePlayer(uuid).getName() != null ? Bukkit.getOfflinePlayer(uuid).getName() : uuid.toString());
    }

    public long getMonthTotalMillis(UUID uuid, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Instant start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return getTrackedMillis(uuid, start, end);
    }

    public long getCurrentWeekMillis(UUID uuid) {
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = weekStartDate.plusDays(7);
        return getTrackedMillis(uuid,
                weekStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                weekEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public LocalDate getCurrentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    public LocalDate getCurrentWeekEndInclusive() {
        return getCurrentWeekStart().plusDays(6);
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

        player.sendMessage(MessageUtil.color(message));
    }

    private void save(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
