package com.example.staffmode.gui;

import com.example.staffmode.hours.StaffHoursManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.example.staffmode.util.ItemUtil.menuItem;

public class StaffHoursGui implements Listener {

    private final JavaPlugin plugin;
    private final StaffHoursManager staffHoursManager;

    private final Map<UUID, List<UUID>> staffHoursMenuTargets = new HashMap<>();
    private final Map<UUID, UUID> staffHoursSelectedTarget = new HashMap<>();
    private final Map<UUID, Integer> staffHoursSelectedMonth = new HashMap<>();

    public StaffHoursGui(JavaPlugin plugin, StaffHoursManager staffHoursManager) {
        this.plugin = plugin;
        this.staffHoursManager = staffHoursManager;
    }

    public void openStaffHoursPlayersMenu(Player admin) {
        Inventory menu = Bukkit.createInventory(null, 54, "Staff Hours");
        File[] files = staffHoursManager.getStaffHoursFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        List<UUID> staffUuids = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    String uuidText = file.getName().replace(".yml", "");
                    staffUuids.add(UUID.fromString(uuidText));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        staffUuids.sort(Comparator.comparing(uuid -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(staffHoursManager.getStaffHoursFile(uuid));
            return config.getString("name", "Unknown").toLowerCase();
        }));

        staffHoursMenuTargets.put(admin.getUniqueId(), staffUuids);

        int slot = 0;

        for (UUID uuid : staffUuids) {
            if (slot >= 54) {
                break;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(staffHoursManager.getStaffHoursFile(uuid));
            String name = config.getString("name", offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ChatColor.GREEN + name);

            long totalMillis = staffHoursManager.getTrackedMillis(uuid, null, null);

            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Total Staff Hours: " + ChatColor.WHITE + staffHoursManager.formatDuration(totalMillis),
                    ChatColor.YELLOW + "Click to view monthly hours"
            ));

            head.setItemMeta(meta);
            menu.setItem(slot, head);
            slot++;
        }

        if (staffUuids.isEmpty()) {
            menu.setItem(22, menuItem(Material.BARRIER, ChatColor.RED + "No staff hours recorded yet"));
        }

        admin.openInventory(menu);
    }

    private void openStaffHoursMonthsMenu(Player admin, UUID targetUuid) {
        int year = LocalDate.now().getYear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(staffHoursManager.getStaffHoursFile(targetUuid));
        String name = config.getString("name", "Unknown");
        Inventory menu = Bukkit.createInventory(null, 27, "Staff Months: " + name);

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);

            Instant start = yearMonth.atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Instant end = yearMonth.plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            long millis = staffHoursManager.getTrackedMillis(targetUuid, start, end);
            String monthName = java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.US);

            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.AQUA + monthName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Year: " + ChatColor.WHITE + year,
                    ChatColor.GRAY + "Hours: " + ChatColor.WHITE + staffHoursManager.formatDuration(millis),
                    ChatColor.YELLOW + "Click to view weeks"
            ));

            item.setItemMeta(meta);
            menu.setItem(month - 1, item);
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = weekStartDate.plusDays(7);

        Instant weekStart = weekStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekEnd = weekEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long currentWeekMillis = staffHoursManager.getTrackedMillis(targetUuid, weekStart, weekEnd);

        ItemStack currentWeek = new ItemStack(Material.NETHER_STAR);
        ItemMeta currentWeekMeta = currentWeek.getItemMeta();

        currentWeekMeta.setDisplayName(ChatColor.GOLD + "Current Week");
        currentWeekMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Dates: " + ChatColor.WHITE + weekStartDate + " - " + weekEndDate.minusDays(1),
                ChatColor.GRAY + "Hours: " + ChatColor.WHITE + staffHoursManager.formatDuration(currentWeekMillis),
                ChatColor.YELLOW + "Click to view current week"
        ));

        currentWeek.setItemMeta(currentWeekMeta);
        menu.setItem(13, currentWeek);
        menu.setItem(22, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));
        admin.openInventory(menu);
    }

    private void openStaffHoursCurrentWeekMenu(Player admin, UUID targetUuid) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(staffHoursManager.getStaffHoursFile(targetUuid));
        String name = config.getString("name", "Unknown");
        Inventory menu = Bukkit.createInventory(null, 27, "Current Week: " + name);

        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = weekStartDate.plusDays(7);

        Instant weekStart = weekStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekEnd = weekEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long weekMillis = staffHoursManager.getTrackedMillis(targetUuid, weekStart, weekEnd);

        ItemStack currentWeek = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = currentWeek.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Current Week Hours");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Staff: " + ChatColor.WHITE + name,
                ChatColor.GRAY + "Dates: " + ChatColor.WHITE + weekStartDate + " - " + weekEndDate.minusDays(1),
                ChatColor.GRAY + "Hours: " + ChatColor.WHITE + staffHoursManager.formatDuration(weekMillis)
        ));

        currentWeek.setItemMeta(meta);
        menu.setItem(13, currentWeek);
        menu.setItem(22, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));
        admin.openInventory(menu);
    }

    private void openStaffHoursWeeksMenu(Player admin, UUID targetUuid, int month) {
        int year = LocalDate.now().getYear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(staffHoursManager.getStaffHoursFile(targetUuid));
        String name = config.getString("name", "Unknown");
        String monthName = java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.US);
        Inventory menu = Bukkit.createInventory(null, 27, "Staff Weeks: " + monthName);

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        int slot = 0;

        for (int week = 1; week <= 5; week++) {
            int startDay = ((week - 1) * 7) + 1;

            if (startDay > daysInMonth) {
                break;
            }

            int endDay = Math.min(startDay + 6, daysInMonth);

            Instant start = yearMonth.atDay(startDay).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant end = yearMonth.atDay(endDay).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            long millis = staffHoursManager.getTrackedMillis(targetUuid, start, end);

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.GREEN + "Week " + week);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Staff: " + ChatColor.WHITE + name,
                    ChatColor.GRAY + "Dates: " + ChatColor.WHITE + monthName + " " + startDay + " - " + endDay,
                    ChatColor.GRAY + "Hours: " + ChatColor.WHITE + staffHoursManager.formatDuration(millis)
            ));

            item.setItemMeta(meta);
            menu.setItem(slot, item);
            slot++;
        }

        long monthMillis = staffHoursManager.getMonthTotalMillis(targetUuid, year, month);

        ItemStack total = new ItemStack(Material.EMERALD);
        ItemMeta totalMeta = total.getItemMeta();

        totalMeta.setDisplayName(ChatColor.GREEN + "Monthly Total");
        totalMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Total: " + ChatColor.WHITE + staffHoursManager.formatDuration(monthMillis)
        ));

        total.setItemMeta(totalMeta);
        menu.setItem(18, total);
        menu.setItem(22, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));
        admin.openInventory(menu);
    }

    @EventHandler
    public void onStaffHoursClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) {
            return;
        }

        if (event.getView().getTitle().equals("Staff Hours")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            int slot = event.getRawSlot();
            List<UUID> targets = staffHoursMenuTargets.get(staff.getUniqueId());

            if (targets == null || slot < 0 || slot >= targets.size()) {
                return;
            }

            UUID targetUuid = targets.get(slot);
            staffHoursSelectedTarget.put(staff.getUniqueId(), targetUuid);
            openStaffHoursMonthsMenu(staff, targetUuid);
            return;
        }

        if (event.getView().getTitle().startsWith("Staff Months")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (clicked.getType() == Material.ARROW) {
                openStaffHoursPlayersMenu(staff);
                return;
            }

            int slot = event.getRawSlot();
            UUID targetUuid = staffHoursSelectedTarget.get(staff.getUniqueId());

            if (targetUuid == null) {
                staff.sendMessage(ChatColor.RED + "No staff member selected.");
                return;
            }

            if (slot == 13) {
                openStaffHoursCurrentWeekMenu(staff, targetUuid);
                return;
            }

            if (slot < 0 || slot > 11) {
                return;
            }

            int month = slot + 1;
            staffHoursSelectedMonth.put(staff.getUniqueId(), month);
            openStaffHoursWeeksMenu(staff, targetUuid, month);
            return;
        }

        if (event.getView().getTitle().startsWith("Current Week")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (clicked.getType() == Material.ARROW) {
                UUID targetUuid = staffHoursSelectedTarget.get(staff.getUniqueId());

                if (targetUuid != null) {
                    openStaffHoursMonthsMenu(staff, targetUuid);
                }
            }

            return;
        }

        if (event.getView().getTitle().startsWith("Staff Weeks")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (clicked.getType() == Material.ARROW) {
                UUID targetUuid = staffHoursSelectedTarget.get(staff.getUniqueId());

                if (targetUuid != null) {
                    openStaffHoursMonthsMenu(staff, targetUuid);
                }
            }
        }
    }
}
