package com.joshuacolvin.ascendrelics.command;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TrustCommand implements CommandExecutor, TabCompleter {

    private final AscendRelics plugin;

    public TrustCommand(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.info(player, "Usage: /trust <add|remove|list> [player]");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "add" -> {
                if (args.length < 2) {
                    MessageUtil.error(player, "Usage: /trust add <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    MessageUtil.error(player, "Player not found!");
                    return true;
                }
                if (target.equals(player)) {
                    MessageUtil.error(player, "You cannot trust yourself!");
                    return true;
                }
                if (plugin.trustManager().addTrust(player.getUniqueId(), target.getUniqueId())) {
                    plugin.dataManager().saveTrust(plugin.trustManager());
                    MessageUtil.success(player, "Added " + target.getName() + " to your trust list.");
                } else {
                    MessageUtil.info(player, target.getName() + " is already trusted.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    MessageUtil.error(player, "Usage: /trust remove <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    MessageUtil.error(player, "Player not found!");
                    return true;
                }
                if (plugin.trustManager().removeTrust(player.getUniqueId(), target.getUniqueId())) {
                    plugin.dataManager().saveTrust(plugin.trustManager());
                    MessageUtil.success(player, "Removed " + target.getName() + " from your trust list.");
                } else {
                    MessageUtil.info(player, target.getName() + " was not trusted.");
                }
            }
            case "list" -> {
                Set<UUID> trusted = plugin.trustManager().getTrusted(player.getUniqueId());
                if (trusted.isEmpty()) {
                    MessageUtil.info(player, "Your trust list is empty.");
                } else {
                    MessageUtil.info(player, "Trusted players:");
                    for (UUID id : trusted) {
                        Player p = Bukkit.getPlayer(id);
                        String name = p != null ? p.getName() : id.toString();
                        MessageUtil.info(player, " - " + name);
                    }
                }
            }
            default -> MessageUtil.error(player, "Usage: /trust <add|remove|list> [player]");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("add", "remove", "list")) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
