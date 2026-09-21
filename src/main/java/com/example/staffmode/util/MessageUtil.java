package com.example.staffmode.util;

import org.bukkit.ChatColor;

public final class MessageUtil {
    private MessageUtil() {}

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
