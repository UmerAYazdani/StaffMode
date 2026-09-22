package com.example.staffmode.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Small utility class for creating named GUI items and translating Minecraft color codes.
 */
public final class ItemUtil {
    private ItemUtil() {}

    // Creates a basic GUI item with the requested material and display name.
    public static ItemStack menuItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    // Converts ampersand color codes such as &c into Bukkit ChatColor formatting.
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
