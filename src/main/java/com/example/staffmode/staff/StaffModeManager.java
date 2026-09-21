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

public class StaffModeManager {

    private final JavaPlugin plugin;
    private final StaffHoursManager staffHoursManager;
    private final File playerDataFolder;

    private final Set<UUID> dropEnabled = new HashSet<>();
    private final Set<UUID> pickupEnabled = new HashSet<>();
    private final Set<UUID> buildEnabled = new HashSet<>();

    public StaffModeManager(JavaPlugin plugin, StaffHoursManager staffHoursManager) {
        this.plugin = plugin;
        this.staffHoursManager = staffHoursManager;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    public void createFolders() {
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public File getPlayerFile(Player player) {
        return new File(playerDataFolder, player.getUniqueId() + ".yml");
    }

    public boolean isInStaffMode(Player player) {
        return getPlayerFile(player).exists();
    }

    public void toggleSelf(Player player) {
        File file = getPlayerFile(player);

        if (file.exists()) {
            restorePlayer(player, file);
            player.sendMessage(ChatColor.GREEN + "Staff mode disabled.");
        } else {
            savePlayer(player, file);
            enterStaffMode(player);
            player.sendMessage(ChatColor.AQUA + "Staff mode enabled.");
        }
    }

    public void toggleOther(Player sender, Player target) {
        File targetFile = getPlayerFile(target);

        if (targetFile.exists()) {
            restorePlayer(target, targetFile);
            target.sendMessage(ChatColor.RED + "Your staff mode was disabled.");
            sender.sendMessage(ChatColor.GREEN + "Disabled staff mode for " + target.getName());
        } else {
            savePlayer(target, targetFile);
            enterStaffMode(target);
            target.sendMessage(ChatColor.AQUA + "Your staff mode was enabled.");
            sender.sendMessage(ChatColor.GREEN + "Enabled staff mode for " + target.getName());
        }
    }

    public void savePlayer(Player player, File file) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("inventory.contents", Arrays.asList(player.getInventory().getContents()));
        config.set("inventory.armor", Arrays.asList(player.getInventory().getArmorContents()));
        config.set("inventory.extra", Arrays.asList(player.getInventory().getExtraContents()));
        config.set("inventory.heldslot", player.getInventory().getHeldItemSlot());

        config.set("location", player.getLocation());
        config.set("gamemode", player.getGameMode().name());

        config.set("exp", player.getExp());
        config.set("level", player.getLevel());
        config.set("totalexp", player.getTotalExperience());

        config.set("health", player.getHealth());
        config.set("food", player.getFoodLevel());
        config.set("saturation", player.getSaturation());
        config.set("exhaustion", player.getExhaustion());
        config.set("fireticks", player.getFireTicks());
        config.set("falldistance", player.getFallDistance());

        config.set("allowflight", player.getAllowFlight());
        config.set("flying", player.isFlying());

        config.set("potions", new ArrayList<>(player.getActivePotionEffects()));
        save(config, file);
    }

    public void enterStaffMode(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();

        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
        player.setFireTicks(0);
        player.setFallDistance(0);

        staffHoursManager.startTrackingNow(player);
        player.updateInventory();
    }

    public void restorePlayer(Player player, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<?> contents = config.getList("inventory.contents");
        List<?> armor = config.getList("inventory.armor");
        List<?> extra = config.getList("inventory.extra");

        if (contents != null) {
            player.getInventory().setContents(contents.toArray(new ItemStack[contents.size()]));
        }

        if (armor != null) {
            player.getInventory().setArmorContents(armor.toArray(new ItemStack[armor.size()]));
        }

        if (extra != null) {
            player.getInventory().setExtraContents(extra.toArray(new ItemStack[extra.size()]));
        }

        player.getInventory().setHeldItemSlot(config.getInt("inventory.heldslot"));

        Location location = config.getLocation("location");

        if (location != null) {
            player.teleport(location);
        }

        player.setGameMode(GameMode.valueOf(config.getString("gamemode", "SURVIVAL")));

        player.setExp((float) config.getDouble("exp"));
        player.setLevel(config.getInt("level"));
        player.setTotalExperience(config.getInt("totalexp"));

        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
        player.setHealth(Math.min(config.getDouble("health"), maxHealth));

        player.setFoodLevel(config.getInt("food"));
        player.setSaturation((float) config.getDouble("saturation"));
        player.setExhaustion((float) config.getDouble("exhaustion"));
        player.setFireTicks(config.getInt("fireticks"));
        player.setFallDistance((float) config.getDouble("falldistance"));

        player.setAllowFlight(config.getBoolean("allowflight"));
        player.setFlying(config.getBoolean("flying") && player.getAllowFlight());

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        List<?> potionList = config.getList("potions");

        if (potionList != null) {
            for (Object obj : potionList) {
                if (obj instanceof PotionEffect effect) {
                    player.addPotionEffect(effect);
                }
            }
        }

        player.updateInventory();

        staffHoursManager.stopTracking(player);
        file.delete();
        dropEnabled.remove(player.getUniqueId());
        pickupEnabled.remove(player.getUniqueId());
        buildEnabled.remove(player.getUniqueId());
    }

    public boolean toggleDrop(Player player) {
        if (!isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You must be in staff mode to toggle item dropping.");
            return true;
        }

        if (!player.hasPermission("staffmode.drop")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to toggle item dropping.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (dropEnabled.contains(uuid)) {
            dropEnabled.remove(uuid);
            player.sendMessage(ChatColor.RED + "Staff mode item dropping disabled.");
        } else {
            dropEnabled.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Staff mode item dropping enabled.");
        }

        return true;
    }

    public boolean togglePickup(Player player) {
        if (!isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You must be in staff mode to toggle item pickup.");
            return true;
        }

        if (!player.hasPermission("staffmode.pickup")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to toggle item pickup.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (pickupEnabled.contains(uuid)) {
            pickupEnabled.remove(uuid);
            player.sendMessage(ChatColor.RED + "Staff mode item pickup disabled.");
        } else {
            pickupEnabled.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Staff mode item pickup enabled.");
        }

        return true;
    }

    public boolean toggleBuild(Player player) {
        if (!isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You must be in staff mode to toggle building.");
            return true;
        }

        String worldName = player.getWorld().getName();

        if (!player.hasPermission("staffmode.build.*") && !player.hasPermission("staffmode.build." + worldName)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to build in this world.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (buildEnabled.contains(uuid)) {
            buildEnabled.remove(uuid);
            player.sendMessage(ChatColor.RED + "Staff mode building disabled.");
        } else {
            buildEnabled.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Staff mode building enabled.");
        }

        return true;
    }

    public boolean canBuildInThisWorld(Player player) {
        String worldName = player.getWorld().getName();
        boolean hasWorldPermission = player.hasPermission("staffmode.build.*") || player.hasPermission("staffmode.build." + worldName);
        return buildEnabled.contains(player.getUniqueId()) && hasWorldPermission;
    }

    public boolean isDropEnabled(Player player) {
        return dropEnabled.contains(player.getUniqueId());
    }

    public boolean isPickupEnabled(Player player) {
        return pickupEnabled.contains(player.getUniqueId());
    }

    private void save(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
