package com.joshuacolvin.ascendrelics.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private static final Component PREFIX = Component.text("[AscendRelics] ")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true);

    private MessageUtil() {}

    public static void send(Player player, String message, NamedTextColor color) {
        player.sendMessage(PREFIX.append(Component.text(message).color(color)));
    }

    public static void success(Player player, String message) {
        send(player, message, NamedTextColor.GREEN);
    }

    public static void error(Player player, String message) {
        send(player, message, NamedTextColor.RED);
    }

    public static void info(Player player, String message) {
        send(player, message, NamedTextColor.GRAY);
    }

    public static void cooldown(Player player, String abilityName, long remainingMillis) {
        long seconds = (remainingMillis / 1000) + 1;
        error(player, abilityName + " is on cooldown for " + seconds + "s.");
    }

    public static void actionBar(Player player, Component message) {
        player.sendActionBar(message);
    }
}
