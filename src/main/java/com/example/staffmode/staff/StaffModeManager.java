package com.example.staffmode.staff;

import com.example.staffmode.hours.StaffHoursManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the core StaffMode system.
 *
 * This class:
 * - Saves the player's normal state
 * - Enters StaffMode
 * - Restores the player when StaffMode ends
 * - Handles build/drop/pickup toggles
 * - Re-applies StaffMode after world changes
 */
public class StaffModeManager {

    private final JavaPlugin plugin;
    private final StaffHoursManager staffHoursManager;
    private final File playerDataFolder;

    private final Set<UUID> dropEnabled =
            new HashSet<>();

    private final Set<UUID> pickupEnabled =
            new HashSet<>();

    private final Set<UUID> buildEnabled =
            new HashSet<>();

    /**
     * Creates the StaffMode manager.
     */
    public StaffModeManager(
            JavaPlugin plugin,
            StaffHoursManager staffHoursManager
    ) {

        this.plugin = plugin;
        this.staffHoursManager = staffHoursManager;

        this.playerDataFolder =
                new File(
                        plugin.getDataFolder(),
                        "playerdata"
                );
    }

    /**
     * Creates the player data folder.
     */
    public void createFolders() {

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    /**
     * Returns the file containing the player's
     * normal state before entering StaffMode.
     */
    public File getPlayerFile(Player player) {

        return new File(
                playerDataFolder,
                player.getUniqueId() + ".yml"
        );
    }

    /**
     * Checks whether a player is currently
     * in StaffMode.
     */
    public boolean isInStaffMode(Player player) {

        return getPlayerFile(player).exists();
    }

    /**
     * Toggles StaffMode for yourself.
     */
    public void toggleSelf(Player player) {

        File file =
                getPlayerFile(player);

        /*
         * Already in StaffMode.
         * Turn it off.
         */
        if (file.exists()) {

            restorePlayer(
                    player,
                    file
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "Staff mode disabled."
            );

            return;
        }

        /*
         * Not in StaffMode.
         * Save normal player data first.
         */
        savePlayer(
                player,
                file
        );

        enterStaffMode(player);

        player.sendMessage(
                ChatColor.AQUA
                        + "Staff mode enabled."
        );
    }

    /**
     * Toggles StaffMode for another player.
     */
    public void toggleOther(
            Player sender,
            Player target
    ) {

        File targetFile =
                getPlayerFile(target);

        /*
         * Disable StaffMode.
         */
        if (targetFile.exists()) {

            restorePlayer(
                    target,
                    targetFile
            );

            target.sendMessage(
                    ChatColor.RED
                            + "Your staff mode was disabled."
            );

            sender.sendMessage(
                    ChatColor.GREEN
                            + "Disabled staff mode for "
                            + target.getName()
            );

            return;
        }

        /*
         * Enable StaffMode.
         */
        savePlayer(
                target,
                targetFile
        );

        enterStaffMode(target);

        target.sendMessage(
                ChatColor.AQUA
                        + "Your staff mode was enabled."
        );

        sender.sendMessage(
                ChatColor.GREEN
                        + "Enabled staff mode for "
                        + target.getName()
        );
    }

    /**
     * Saves the player's normal state.
     *
     * This data is restored when StaffMode
     * is disabled.
     */
    public void savePlayer(
            Player player,
            File file
    ) {

        YamlConfiguration config =
                new YamlConfiguration();

        /*
         * INVENTORY
         */
        config.set(
                "inventory.contents",
                Arrays.asList(
                        player
                                .getInventory()
                                .getContents()
                )
        );

        config.set(
                "inventory.armor",
                Arrays.asList(
                        player
                                .getInventory()
                                .getArmorContents()
                )
        );

        config.set(
                "inventory.extra",
                Arrays.asList(
                        player
                                .getInventory()
                                .getExtraContents()
                )
        );

        config.set(
                "inventory.heldslot",
                player
                        .getInventory()
                        .getHeldItemSlot()
        );

        /*
         * LOCATION + GAME MODE
         */
        config.set(
                "location",
                player.getLocation()
        );

        config.set(
                "gamemode",
                player.getGameMode().name()
        );

        /*
         * EXPERIENCE
         */
        config.set(
                "exp",
                player.getExp()
        );

        config.set(
                "level",
                player.getLevel()
        );

        config.set(
                "totalexp",
                player.getTotalExperience()
        );

        /*
         * HEALTH / FOOD
         */
        config.set(
                "health",
                player.getHealth()
        );

        config.set(
                "food",
                player.getFoodLevel()
        );

        config.set(
                "saturation",
                player.getSaturation()
        );

        config.set(
                "exhaustion",
                player.getExhaustion()
        );

        config.set(
                "fireticks",
                player.getFireTicks()
        );

        config.set(
                "falldistance",
                player.getFallDistance()
        );

        /*
         * FLIGHT
         */
        config.set(
                "allowflight",
                player.getAllowFlight()
        );

        config.set(
                "flying",
                player.isFlying()
        );

        /*
         * POTION EFFECTS
         */
        config.set(
                "potions",
                new ArrayList<>(
                        player.getActivePotionEffects()
                )
        );

        save(
                config,
                file
        );
    }

    /**
     * Initially enters StaffMode.
     */
    public void enterStaffMode(Player player) {

        /*
         * Remove normal inventory.
         */
        clearStaffInventory(player);

        /*
         * StaffMode settings.
         */
        player.setGameMode(
                GameMode.CREATIVE
        );

        player.setAllowFlight(true);
        player.setFlying(true);

        double maxHealth =
                Objects.requireNonNull(
                        player.getAttribute(
                                Attribute.MAX_HEALTH
                        )
                ).getValue();

        player.setHealth(
                maxHealth
        );

        player.setFoodLevel(
                20
        );

        player.setSaturation(
                20
        );

        player.setExhaustion(
                0
        );

        player.setFireTicks(
                0
        );

        player.setFallDistance(
                0
        );

        /*
         * Start staff-hour tracking.
         */
        staffHoursManager
                .startTrackingNow(player);

        player.updateInventory();
    }

    /**
     * Re-applies StaffMode without touching the
     * player's saved normal inventory.
     *
     * This is important for Multiverse-Inventories.
     *
     * When Multiverse changes worlds it may put
     * the player's normal inventory back.
     *
     * StaffMode calls this afterward to clear
     * that inventory again.
     */
    public void reapplyStaffMode(Player player) {

        /*
         * Do nothing unless the player's StaffMode
         * save file still exists.
         */
        if (!isInStaffMode(player)) {
            return;
        }

        /*
         * Clear whatever inventory Multiverse
         * may have loaded.
         */
        clearStaffInventory(player);

        /*
         * Re-apply StaffMode settings.
         */
        player.setGameMode(
                GameMode.CREATIVE
        );

        player.setAllowFlight(true);
        player.setFlying(true);

        player.setFallDistance(
                0
        );

        player.setFireTicks(
                0
        );

        player.updateInventory();
    }

    /**
     * Clears inventory/equipment while
     * StaffMode is active.
     */
    private void clearStaffInventory(
            Player player
    ) {

        player
                .getInventory()
                .clear();

        player
                .getInventory()
                .setArmorContents(null);

        player
                .getInventory()
                .setExtraContents(null);
    }

    /**
     * Restores the player's state from before
     * they entered StaffMode.
     */
    public void restorePlayer(
            Player player,
            File file
    ) {

        YamlConfiguration config =
                YamlConfiguration
                        .loadConfiguration(file);

        /*
         * INVENTORY
         */
        List<?> contents =
                config.getList(
                        "inventory.contents"
                );

        List<?> armor =
                config.getList(
                        "inventory.armor"
                );

        List<?> extra =
                config.getList(
                        "inventory.extra"
                );

        if (contents != null) {

            player
                    .getInventory()
                    .setContents(
                            contents.toArray(
                                    new ItemStack[
                                            contents.size()
                                            ]
                            )
                    );
        }

        if (armor != null) {

            player
                    .getInventory()
                    .setArmorContents(
                            armor.toArray(
                                    new ItemStack[
                                            armor.size()
                                            ]
                            )
                    );
        }

        if (extra != null) {

            player
                    .getInventory()
                    .setExtraContents(
                            extra.toArray(
                                    new ItemStack[
                                            extra.size()
                                            ]
                            )
                    );
        }

        player
                .getInventory()
                .setHeldItemSlot(
                        config.getInt(
                                "inventory.heldslot"
                        )
                );

        /*
         * LOCATION
         */
        Location location =
                config.getLocation(
                        "location"
                );

        if (location != null) {

            player.teleport(
                    location
            );
        }

        /*
         * GAME MODE
         */
        String gameModeName =
                config.getString(
                        "gamemode",
                        "SURVIVAL"
                );

        try {

            player.setGameMode(
                    GameMode.valueOf(
                            gameModeName
                    )
            );

        } catch (IllegalArgumentException exception) {

            player.setGameMode(
                    GameMode.SURVIVAL
            );
        }

        /*
         * EXPERIENCE
         */
        player.setExp(
                (float) config.getDouble(
                        "exp"
                )
        );

        player.setLevel(
                config.getInt(
                        "level"
                )
        );

        player.setTotalExperience(
                config.getInt(
                        "totalexp"
                )
        );

        /*
         * HEALTH
         */
        double maxHealth =
                Objects.requireNonNull(
                        player.getAttribute(
                                Attribute.MAX_HEALTH
                        )
                ).getValue();

        player.setHealth(
                Math.min(
                        config.getDouble(
                                "health"
                        ),
                        maxHealth
                )
        );

        player.setFoodLevel(
                config.getInt(
                        "food"
                )
        );

        player.setSaturation(
                (float) config.getDouble(
                        "saturation"
                )
        );

        player.setExhaustion(
                (float) config.getDouble(
                        "exhaustion"
                )
        );

        player.setFireTicks(
                config.getInt(
                        "fireticks"
                )
        );

        player.setFallDistance(
                (float) config.getDouble(
                        "falldistance"
                )
        );

        /*
         * FLIGHT
         */
        player.setAllowFlight(
                config.getBoolean(
                        "allowflight"
                )
        );

        player.setFlying(
                config.getBoolean(
                        "flying"
                )
                        &&
                        player.getAllowFlight()
        );

        /*
         * POTION EFFECTS
         */
        for (
                PotionEffect effect :
                player.getActivePotionEffects()
        ) {

            player.removePotionEffect(
                    effect.getType()
            );
        }

        List<?> potionList =
                config.getList(
                        "potions"
                );

        if (potionList != null) {

            for (Object object :
                    potionList) {

                if (object instanceof PotionEffect effect) {

                    player.addPotionEffect(
                            effect
                    );
                }
            }
        }

        player.updateInventory();

        /*
         * Stop StaffMode tracking.
         */
        staffHoursManager
                .stopTracking(player);

        /*
         * Delete the StaffMode state file.
         *
         * Once this is deleted,
         * isInStaffMode() becomes false.
         */
        if (!file.delete()) {

            plugin
                    .getLogger()
                    .warning(
                            "Could not delete StaffMode data file for "
                                    + player.getName()
                    );
        }

        /*
         * Reset temporary toggles.
         */
        dropEnabled.remove(
                player.getUniqueId()
        );

        pickupEnabled.remove(
                player.getUniqueId()
        );

        buildEnabled.remove(
                player.getUniqueId()
        );
    }

    /**
     * Toggles item dropping.
     */
    public boolean toggleDrop(
            Player player
    ) {

        if (!isInStaffMode(player)) {

            player.sendMessage(
                    ChatColor.RED
                            + "You must be in staff mode to toggle item dropping."
            );

            return true;
        }

        if (!player.hasPermission(
                "staffmode.drop"
        )) {

            player.sendMessage(
                    ChatColor.RED
                            + "You do not have permission to toggle item dropping."
            );

            return true;
        }

        UUID uuid =
                player.getUniqueId();

        if (dropEnabled.contains(uuid)) {

            dropEnabled.remove(uuid);

            player.sendMessage(
                    ChatColor.RED
                            + "Staff mode item dropping disabled."
            );

        } else {

            dropEnabled.add(uuid);

            player.sendMessage(
                    ChatColor.GREEN
                            + "Staff mode item dropping enabled."
            );
        }

        return true;
    }

    /**
     * Toggles item pickup.
     */
    public boolean togglePickup(
            Player player
    ) {

        if (!isInStaffMode(player)) {

            player.sendMessage(
                    ChatColor.RED
                            + "You must be in staff mode to toggle item pickup."
            );

            return true;
        }

        if (!player.hasPermission(
                "staffmode.pickup"
        )) {

            player.sendMessage(
                    ChatColor.RED
                            + "You do not have permission to toggle item pickup."
            );

            return true;
        }

        UUID uuid =
                player.getUniqueId();

        if (pickupEnabled.contains(uuid)) {

            pickupEnabled.remove(uuid);

            player.sendMessage(
                    ChatColor.RED
                            + "Staff mode item pickup disabled."
            );

        } else {

            pickupEnabled.add(uuid);

            player.sendMessage(
                    ChatColor.GREEN
                            + "Staff mode item pickup enabled."
            );
        }

        return true;
    }

    /**
     * Toggles building while in StaffMode.
     */
    public boolean toggleBuild(
            Player player
    ) {

        if (!isInStaffMode(player)) {

            player.sendMessage(
                    ChatColor.RED
                            + "You must be in staff mode to toggle building."
            );

            return true;
        }

        String worldName =
                player
                        .getWorld()
                        .getName();

        if (!player.hasPermission(
                "staffmode.build.*"
        )
                &&
                !player.hasPermission(
                        "staffmode.build."
                                + worldName
                )) {

            player.sendMessage(
                    ChatColor.RED
                            + "You do not have permission to build in this world."
            );

            return true;
        }

        UUID uuid =
                player.getUniqueId();

        if (buildEnabled.contains(uuid)) {

            buildEnabled.remove(uuid);

            player.sendMessage(
                    ChatColor.RED
                            + "Staff mode building disabled."
            );

        } else {

            buildEnabled.add(uuid);

            player.sendMessage(
                    ChatColor.GREEN
                            + "Staff mode building enabled."
            );
        }

        return true;
    }

    /**
     * Checks whether building is currently
     * allowed in the player's world.
     */
    public boolean canBuildInThisWorld(
            Player player
    ) {

        String worldName =
                player
                        .getWorld()
                        .getName();

        boolean hasWorldPermission =
                player.hasPermission(
                        "staffmode.build.*"
                )
                        ||
                        player.hasPermission(
                                "staffmode.build."
                                        + worldName
                        );

        return buildEnabled.contains(
                player.getUniqueId()
        )
                &&
                hasWorldPermission;
    }

    /**
     * Returns whether dropping is currently enabled.
     */
    public boolean isDropEnabled(
            Player player
    ) {

        return dropEnabled.contains(
                player.getUniqueId()
        );
    }

    /**
     * Returns whether pickup is currently enabled.
     */
    public boolean isPickupEnabled(
            Player player
    ) {

        return pickupEnabled.contains(
                player.getUniqueId()
        );
    }

    /**
     * Saves YAML safely.
     */
    private void save(
            YamlConfiguration config,
            File file
    ) {

        try {

            config.save(file);

        } catch (IOException exception) {

            plugin
                    .getLogger()
                    .severe(
                            "Could not save StaffMode data for "
                                    + file.getName()
                    );

            exception.printStackTrace();
        }
    }
}