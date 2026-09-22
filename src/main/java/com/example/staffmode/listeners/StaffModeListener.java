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

/**
 * Listens for player/server events that affect staff mode, activity tracking, building, item dropping/pickup, teleporting, joining, and quitting.
 */
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
    // Ends active tracking and clears temporary activity state when a staff member leaves the server.
    public void onStaffQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        staffHoursManager.stopTracking(player);
    }

    @EventHandler
    // Restarts staff tracking when needed and checks whether the joining player has past offenses.
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        if (staffModeManager.isInStaffMode(joined)) {
            staffHoursManager.startTrackingNow(joined);
        }

        offenseManager.sendJoinAlertIfNeeded(joined);
    }

    @EventHandler
    // Counts real block-to-block movement as staff activity so active staff do not become AFK.
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
    // Counts player interaction as staff activity.
    public void onStaffInteract(PlayerInteractEvent event) {
        staffHoursManager.markStaffActive(event.getPlayer(), staffModeManager);
    }

    @EventHandler
    // Counts running a command as staff activity.
    public void onStaffCommand(PlayerCommandPreprocessEvent event) {
        staffHoursManager.markStaffActive(event.getPlayer(), staffModeManager);
    }

    @EventHandler
    // Re-applies creative mode and flight after a staff-mode teleport.
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
    // Cancels block placement in staff mode unless the build toggle and permission allow it.
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
    // Cancels block breaking in staff mode unless the build toggle and permission allow it.
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
    // Blocks item dropping in staff mode unless the player has permission and has enabled the drop toggle.
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
    // Blocks item pickup in staff mode unless the player has permission and has enabled the pickup toggle.
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
