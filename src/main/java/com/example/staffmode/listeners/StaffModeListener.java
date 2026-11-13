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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles StaffMode-related server events.
 *
 * This includes:
 * - Staff hour activity
 * - Joining/quitting
 * - World switching
 * - Teleporting
 * - Building
 * - Item pickup/drop
 */
public class StaffModeListener
        implements Listener {

    private final JavaPlugin plugin;

    private final StaffModeManager staffModeManager;

    private final StaffHoursManager staffHoursManager;

    private final OffenseManager offenseManager;

    public StaffModeListener(
            JavaPlugin plugin,
            StaffModeManager staffModeManager,
            StaffHoursManager staffHoursManager,
            OffenseManager offenseManager
    ) {

        this.plugin = plugin;
        this.staffModeManager =
                staffModeManager;

        this.staffHoursManager =
                staffHoursManager;

        this.offenseManager =
                offenseManager;
    }

    /**
     * Stops the active staff-hour session
     * when a staff member leaves.
     *
     * They remain technically in StaffMode,
     * because their StaffMode data file still exists.
     */
    @EventHandler
    public void onStaffQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        staffHoursManager
                .stopTracking(player);
    }

    /**
     * Restores StaffMode when someone rejoins
     * while their StaffMode data file still exists.
     */
    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        if (staffModeManager
                .isInStaffMode(player)) {

            /*
             * Re-apply StaffMode after the player
             * has fully joined.
             */
            Bukkit
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> {

                                if (!player.isOnline()) {
                                    return;
                                }

                                if (!staffModeManager
                                        .isInStaffMode(player)) {

                                    return;
                                }

                                staffModeManager
                                        .reapplyStaffMode(player);

                            },
                            2L
                    );

            /*
             * Restart hours tracking.
             */
            staffHoursManager
                    .startTrackingNow(player);
        }

        /*
         * Notify staff about past offenses.
         */
        offenseManager
                .sendJoinAlertIfNeeded(player);
    }

    /**
     * Marks actual player movement as activity.
     */
    @EventHandler
    public void onStaffMove(
            PlayerMoveEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        if (event.getTo() == null) {
            return;
        }

        /*
         * Looking around does not count.
         *
         * Player must actually change blocks.
         */
        if (event.getFrom().getBlockX()
                == event.getTo().getBlockX()

                &&

                event.getFrom().getBlockY()
                        == event.getTo().getBlockY()

                &&

                event.getFrom().getBlockZ()
                        == event.getTo().getBlockZ()) {

            return;
        }

        staffHoursManager
                .markStaffActive(
                        player,
                        staffModeManager
                );
    }

    /**
     * Interaction counts as staff activity.
     */
    @EventHandler
    public void onStaffInteract(
            PlayerInteractEvent event
    ) {

        staffHoursManager
                .markStaffActive(
                        event.getPlayer(),
                        staffModeManager
                );
    }

    /**
     * Commands count as staff activity.
     */
    @EventHandler
    public void onStaffCommand(
            PlayerCommandPreprocessEvent event
    ) {

        staffHoursManager
                .markStaffActive(
                        event.getPlayer(),
                        staffModeManager
                );
    }

    /**
     * Normal teleport protection.
     *
     * Keeps Creative/flight enabled after teleporting.
     */
    @EventHandler
    public void onTeleport(
            PlayerTeleportEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        Bukkit
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            if (!staffModeManager
                                    .isInStaffMode(player)) {

                                return;
                            }

                            player.setGameMode(
                                    GameMode.CREATIVE
                            );

                            player.setAllowFlight(
                                    true
                            );

                            player.setFlying(
                                    true
                            );

                        },
                        2L
                );
    }

    /**
     * IMPORTANT:
     * Fixes Multiverse-Inventories restoring the
     * player's normal inventory while StaffMode
     * is still active.
     *
     * PlayerChangedWorldEvent fires after the
     * destination world has been entered.
     */
    @EventHandler
    public void onWorldChange(
            PlayerChangedWorldEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        /*
         * First re-check shortly after changing worlds.
         */
        Bukkit
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            if (!staffModeManager
                                    .isInStaffMode(player)) {

                                return;
                            }

                            staffModeManager
                                    .reapplyStaffMode(player);

                        },
                        2L
                );

        /*
         * Second safety check.
         *
         * Some inventory-management plugins may
         * apply their inventory a few ticks later.
         *
         * This makes sure StaffMode wins last.
         */
        Bukkit
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            if (!staffModeManager
                                    .isInStaffMode(player)) {

                                return;
                            }

                            staffModeManager
                                    .reapplyStaffMode(player);

                        },
                        5L
                );
    }

    /**
     * Prevents building unless StaffMode
     * build mode is enabled.
     */
    @EventHandler
    public void onBlockPlace(
            BlockPlaceEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        if (!staffModeManager
                .canBuildInThisWorld(player)) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED
                            + "You cannot build in this world while in staff mode."
            );
        }
    }

    /**
     * Prevents breaking blocks unless
     * StaffMode build mode is enabled.
     */
    @EventHandler
    public void onBlockBreak(
            BlockBreakEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        if (!staffModeManager
                .canBuildInThisWorld(player)) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED
                            + "You cannot break blocks in this world while in staff mode."
            );
        }
    }

    /**
     * Controls item dropping while in StaffMode.
     */
    @EventHandler
    public void onItemDrop(
            PlayerDropItemEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        if (!player.hasPermission(
                "staffmode.drop"
        )) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED
                            + "You cannot drop items in staff mode."
            );

            return;
        }

        if (!staffModeManager
                .isDropEnabled(player)) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED
                            + "Item dropping is disabled. Use /staffmode drop to enable it."
            );
        }
    }

    /**
     * Controls item pickup while in StaffMode.
     */
    @EventHandler
    public void onItemPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity()
                instanceof Player player)) {

            return;
        }

        if (!staffModeManager
                .isInStaffMode(player)) {

            return;
        }

        if (!player.hasPermission(
                "staffmode.pickup"
        )) {

            event.setCancelled(true);

            return;
        }

        if (!staffModeManager
                .isPickupEnabled(player)) {

            event.setCancelled(true);
        }
    }
}