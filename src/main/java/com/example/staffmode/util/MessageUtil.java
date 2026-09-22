package com.example.staffmode.util;

import org.bukkit.ChatColor;

/**
 * Small utility class for translating ampersand color codes in configurable messages.
 */
public final class MessageUtil {
    private MessageUtil() {}

    // Converts ampersand color codes such as &c into Bukkit ChatColor formatting.
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
