package com.example.staffmode.listeners;

import com.example.staffmode.hours.StaffHoursManager;
import com.example.staffmode.offenses.OffenseManager;
import com.example.staffmode.staff.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class StaffModeListener implements Listener {

    private final JavaPlugin plugin;
    private final StaffModeManager staffModeManager;
    private final StaffHoursManager staffHoursManager;
    private final OffenseManager offenseManager;

    public StaffModeListener(JavaPlugin plugin,
                             StaffModeManager staffModeManager,
                             StaffHoursManager staffHoursManager,
                             OffenseManager offenseManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.staffHoursManager = staffHoursManager;
        this.offenseManager = offenseManager;
    }

    @EventHandler
    public void onStaffQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        staffHoursManager.stopTracking(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        if (staffModeManager.isInStaffMode(joined)) {
            staffHoursManager.startTrackingNow(joined);
        }

        offenseManager.sendJoinAlertIfNeeded(joined);
    }

    @EventHandler
    public void onStaffMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        staffHoursManager.markStaffActive(player, staffModeManager);
    }

    @EventHandler
    public void onStaffInteract(PlayerInteractEvent event) {
        staffHoursManager.markStaffActive(event.getPlayer(), staffModeManager);
    }

    @EventHandler
    public void onStaffCommand(PlayerCommandPreprocessEvent event) {
        staffHoursManager.markStaffActive(event.getPlayer(), staffModeManager);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && staffModeManager.isInStaffMode(player)) {
                player.setGameMode(GameMode.CREATIVE);
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        }, 2L);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        if (!staffModeManager.canBuildInThisWorld(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot build in this world while in staff mode.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        if (!staffModeManager.canBuildInThisWorld(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks in this world while in staff mode.");
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        if (!player.hasPermission("staffmode.drop")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop items in staff mode.");
            return;
        }

        if (!staffModeManager.isDropEnabled(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Item dropping is disabled. Use /staffmode drop to enable it.");
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        if (!player.hasPermission("staffmode.pickup")) {
            event.setCancelled(true);
            return;
        }

        if (!staffModeManager.isPickupEnabled(player)) {
            event.setCancelled(true);
        }
    }
}
