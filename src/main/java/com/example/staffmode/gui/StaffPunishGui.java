package com.example.staffmode.gui;

import com.example.staffmode.hours.StaffHoursManager;
import com.example.staffmode.offenses.OffenseManager;
import com.example.staffmode.staff.StaffModeManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.staffmode.util.ItemUtil.menuItem;

public class StaffPunishGui implements Listener {

    private final JavaPlugin plugin;
    private final StaffModeManager staffModeManager;
    private final StaffHoursManager staffHoursManager;
    private final OffenseManager offenseManager;
    private final Map<UUID, UUID> selectedTargets = new HashMap<>();

    public StaffPunishGui(JavaPlugin plugin,
                          StaffModeManager staffModeManager,
                          StaffHoursManager staffHoursManager,
                          OffenseManager offenseManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.staffHoursManager = staffHoursManager;
        this.offenseManager = offenseManager;
    }

    public void openStaffMainMenu(Player staff, OfflinePlayer target) {
        selectedTargets.put(staff.getUniqueId(), target.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 27, "Staff Menu: " + target.getName());

        ItemStack inspect = menuItem(Material.CHEST, ChatColor.YELLOW + "Inspect Inventory");
        ItemStack enderchest = menuItem(Material.ENDER_CHEST, ChatColor.LIGHT_PURPLE + "View Ender Chest");
        ItemStack mute = menuItem(Material.PAPER, ChatColor.GOLD + "Mute Player");
        ItemStack ban = menuItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Ban Player");
        ItemStack kick = menuItem(Material.IRON_BOOTS, ChatColor.RED + "Kick Player");
        ItemStack offenses = menuItem(Material.BOOK, ChatColor.AQUA + "Past Offenses");
        ItemStack info = menuItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Player Info");

        if (staff.hasPermission("staffmode.gui.inspect")) menu.setItem(10, inspect);
        if (staff.hasPermission("staffmode.gui.enderchest")) menu.setItem(11, enderchest);
        if (staff.hasPermission("staffmode.gui.mute")) menu.setItem(12, mute);
        if (staff.hasPermission("staffmode.gui.ban")) menu.setItem(13, ban);
        if (staff.hasPermission("staffmode.gui.kick")) menu.setItem(14, kick);
        if (staff.hasPermission("staffmode.gui.offenses")) menu.setItem(15, offenses);
        if (staff.hasPermission("staffmode.gui.info")) menu.setItem(16, info);

        staff.openInventory(menu);
    }

    @EventHandler
    public void onPunchPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player staff)) {
            return;
        }

        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (!staffModeManager.isInStaffMode(staff)) {
            return;
        }

        staffHoursManager.markStaffActive(staff, staffModeManager);

        if (!staff.hasPermission("staffmode.gui")) {
            return;
        }

        event.setCancelled(true);
        openStaffMainMenu(staff, target);
    }

    @EventHandler
    public void onStaffMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) {
            return;
        }

        staffHoursManager.markStaffActive(staff, staffModeManager);

        if (event.getView().getTitle().startsWith("Staff Menu: ")) {
            handleStaffMainMenuClick(event, staff);
        }

        if (event.getView().getTitle().startsWith("Mute: ")) {
            handleMuteClick(event, staff);
        }

        if (event.getView().getTitle().startsWith("Ban: ")) {
            handleBanClick(event, staff);
        }

        if (event.getView().getTitle().startsWith("Offenses: ")) {
            handleOffensesClick(event, staff);
        }
    }

    private void handleStaffMainMenuClick(InventoryClickEvent event, Player staff) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        OfflinePlayer target = getSelectedTarget(staff);

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Target not found.");
            staff.closeInventory();
            return;
        }

        switch (clicked.getType()) {
            case CHEST:
                if (!staff.hasPermission("staffmode.gui.inspect")) break;
                if (!target.isOnline()) {
                    staff.sendMessage(ChatColor.RED + "Player must be online to inspect inventory.");
                    break;
                }
                if (Bukkit.getPluginManager().getPlugin("InvSeePlusPlus") == null) {
                    staff.sendMessage(ChatColor.RED + "InvSee++ is not installed.");
                    break;
                }
                staff.performCommand("invsee " + target.getName());
                break;

            case ENDER_CHEST:
                if (!staff.hasPermission("staffmode.gui.enderchest")) break;
                if (!target.isOnline()) {
                    staff.sendMessage(ChatColor.RED + "Player must be online to view their ender chest.");
                    break;
                }
                Plugin invsee = Bukkit.getPluginManager().getPlugin("InvSeePlusPlus");
                if (invsee == null || !invsee.isEnabled()) {
                    staff.sendMessage(ChatColor.RED + "InvSee++ is not installed.");
                    break;
                }
                staff.performCommand("endersee " + target.getName());
                break;

            case PAPER:
                if (!staff.hasPermission("staffmode.gui.mute")) break;
                openMuteMenu(staff, target);
                break;

            case REDSTONE_BLOCK:
                if (!staff.hasPermission("staffmode.gui.ban")) break;
                openBanMenu(staff, target);
                break;

            case IRON_BOOTS:
                if (!staff.hasPermission("staffmode.gui.kick")) break;
                promptReason(staff, target, "Kick", "kick " + target.getName(), "Instant");
                break;

            case BOOK:
                if (!staff.hasPermission("staffmode.gui.offenses")) break;
                openOffensesMenu(staff, target);
                break;

            case PLAYER_HEAD:
                if (!staff.hasPermission("staffmode.gui.info")) break;
                sendPlayerInfo(staff, target);
                staff.closeInventory();
                break;

            default:
                break;
        }
    }

    private void handleMuteClick(InventoryClickEvent event, Player staff) {
        event.setCancelled(true);
        OfflinePlayer target = getSelectedTarget(staff);

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Target not found.");
            staff.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case BARRIER:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unmute " + target.getName());
                staff.sendMessage(ChatColor.GREEN + "Unmuted " + target.getName());
                staff.closeInventory();
                break;
            case ARROW:
                openStaffMainMenu(staff, target);
                break;
            case REDSTONE:
                promptReason(staff, target, "Mute", "mute " + target.getName(), "Permanent");
                break;
            case CLOCK:
                promptReason(staff, target, "Mute", "mute " + target.getName() + " 5m", "5 minutes");
                break;
            case COMPASS:
                promptReason(staff, target, "Mute", "mute " + target.getName() + " 30m", "30 minutes");
                break;
            case WRITABLE_BOOK:
                promptReason(staff, target, "Mute", "mute " + target.getName() + " 1w", "1 week");
                break;
            case PAPER:
                promptReason(staff, target, "Mute", "mute " + target.getName() + " 30d", "1 month");
                break;
            default:
                break;
        }
    }

    private void handleBanClick(InventoryClickEvent event, Player staff) {
        event.setCancelled(true);
        OfflinePlayer target = getSelectedTarget(staff);

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Target not found.");
            staff.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case ARROW:
                openStaffMainMenu(staff, target);
                break;
            case REDSTONE_BLOCK:
                promptReason(staff, target, "Ban", "ban " + target.getName(), "Permanent");
                break;
            case CLOCK:
                promptReason(staff, target, "Ban", "tempban " + target.getName() + " 1d", "1 day");
                break;
            case COMPASS:
                promptReason(staff, target, "Ban", "tempban " + target.getName() + " 1w", "1 week");
                break;
            case MAP:
                promptReason(staff, target, "Ban", "tempban " + target.getName() + " 2w", "2 weeks");
                break;
            case PAPER:
                promptReason(staff, target, "Ban", "tempban " + target.getName() + " 30d", "1 month");
                break;
            default:
                break;
        }
    }

    private void handleOffensesClick(InventoryClickEvent event, Player staff) {
        event.setCancelled(true);
        OfflinePlayer target = getSelectedTarget(staff);

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Target not found.");
            staff.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.ARROW) {
            openStaffMainMenu(staff, target);
        }

        if (clicked.getType() == Material.PAPER) {
            if (event.getClick() != ClickType.DOUBLE_CLICK) {
                staff.sendMessage(ChatColor.YELLOW + "Double-click this offense to remove it.");
                return;
            }

            int offenseId = event.getRawSlot() + 1;
            offenseManager.removeOffense(target, offenseId);
            staff.sendMessage(ChatColor.GREEN + "Removed offense #" + offenseId + " from " + target.getName());
            openOffensesMenu(staff, target);
        }
    }

    private OfflinePlayer getSelectedTarget(Player staff) {
        UUID targetUuid = selectedTargets.get(staff.getUniqueId());

        if (targetUuid == null) {
            return null;
        }

        return Bukkit.getOfflinePlayer(targetUuid);
    }

    private void sendPlayerInfo(Player staff, OfflinePlayer target) {
        long firstPlayed = target.getFirstPlayed();
        long lastPlayed = target.getLastPlayed();

        staff.sendMessage(ChatColor.GREEN + "Player Info: " + target.getName());
        staff.sendMessage(ChatColor.YELLOW + "UUID: " + target.getUniqueId());
        staff.sendMessage(ChatColor.YELLOW + "First joined: " + new Date(firstPlayed));
        staff.sendMessage(ChatColor.YELLOW + "Last played: " + new Date(lastPlayed));

        if (target.isOnline()) {
            Player online = target.getPlayer();
            long playTicks = online.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long playMinutes = playTicks / 20 / 60;
            staff.sendMessage(ChatColor.YELLOW + "Time played: " + playMinutes + " minutes");
        }
    }

    private void openMuteMenu(Player staff, OfflinePlayer target) {
        Inventory menu = Bukkit.createInventory(null, 27, "Mute: " + target.getName());

        menu.setItem(10, menuItem(Material.REDSTONE, ChatColor.RED + "Permanent Mute"));
        menu.setItem(11, menuItem(Material.CLOCK, ChatColor.YELLOW + "5 Minute Mute"));
        menu.setItem(12, menuItem(Material.COMPASS, ChatColor.YELLOW + "30 Minute Mute"));
        menu.setItem(13, menuItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "1 Week Mute"));
        menu.setItem(14, menuItem(Material.PAPER, ChatColor.GOLD + "1 Month Mute"));
        menu.setItem(15, menuItem(Material.BARRIER, ChatColor.GREEN + "Unmute"));
        menu.setItem(22, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));

        staff.openInventory(menu);
    }

    private void openBanMenu(Player staff, OfflinePlayer target) {
        Inventory menu = Bukkit.createInventory(null, 27, "Ban: " + target.getName());

        menu.setItem(10, menuItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Permanent Ban"));
        menu.setItem(11, menuItem(Material.CLOCK, ChatColor.YELLOW + "1 Day Ban"));
        menu.setItem(12, menuItem(Material.COMPASS, ChatColor.GOLD + "1 Week Ban"));
        menu.setItem(13, menuItem(Material.MAP, ChatColor.GOLD + "2 Week Ban"));
        menu.setItem(14, menuItem(Material.PAPER, ChatColor.RED + "1 Month Ban"));
        menu.setItem(22, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));

        staff.openInventory(menu);
    }

    private void promptReason(Player staff, OfflinePlayer target, String type, String command, String duration) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            new AnvilGUI.Builder()
                    .plugin(plugin)
                    .title(type + " Reason")
                    .text("Enter reason")
                    .onClick((slot, stateSnapshot) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return Collections.emptyList();
                        }

                        String reason = stateSnapshot.getText();

                        if (reason == null || reason.trim().isEmpty() || reason.equalsIgnoreCase("Enter reason")) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Reason required"));
                        }

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command + " " + reason);
                            offenseManager.addOffense(staff, target, type, reason, duration);
                            staff.sendMessage(ChatColor.GREEN + type + " issued. Reason: " + reason);
                        });

                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    })
                    .open(staff);
        }, 1L);
    }

    private void openOffensesMenu(Player staff, OfflinePlayer target) {
        Inventory menu = Bukkit.createInventory(null, 54, "Offenses: " + target.getName());
        File file = offenseManager.getOffenseFile(target);

        if (!file.exists()) {
            menu.setItem(22, menuItem(Material.BARRIER, ChatColor.YELLOW + "No stored offenses"));
            menu.setItem(49, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));
            staff.openInventory(menu);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = config.getInt("count", 0);
        int slot = 0;

        for (int i = 1; i <= count && slot < 45; i++) {
            String type = config.getString("offenses." + i + ".type", "Unknown");
            String duration = config.getString("offenses." + i + ".duration", "Unknown");
            String reason = config.getString("offenses." + i + ".reason", "No reason");
            String staffName = config.getString("offenses." + i + ".staff", "Unknown");
            String time = config.getString("offenses." + i + ".time", "Unknown");

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();

            meta.setDisplayName(ChatColor.RED + "#" + i + " " + type);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Duration: " + ChatColor.WHITE + duration,
                    ChatColor.GRAY + "Staff: " + ChatColor.WHITE + staffName,
                    ChatColor.GRAY + "Time: " + ChatColor.WHITE + time,
                    ChatColor.GRAY + "Reason:",
                    ChatColor.WHITE + reason,
                    ChatColor.RED + "Double-click to remove this offense"
            ));

            paper.setItemMeta(meta);
            menu.setItem(slot, paper);
            slot++;
        }

        menu.setItem(49, menuItem(Material.ARROW, ChatColor.GRAY + "Back"));
        staff.openInventory(menu);
    }
}
