package com.joshuacolvin.ascendrelics.command;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UnlockCommand implements CommandExecutor, TabCompleter {

    private final AscendRelics plugin;

    public UnlockCommand(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.isOp()) {
            MessageUtil.error(player, "You must be an operator to use this command.");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.error(player, "Usage: /unlock <relic>");
            return true;
        }

        RelicType type = RelicType.fromName(args[0]);
        if (type == null) {
            MessageUtil.error(player, "Unknown relic type: " + args[0]);
            return true;
        }

        if (plugin.lockManager().unlock(type)) {
            plugin.dataManager().saveLocks(plugin.lockManager());
            MessageUtil.success(player, type.displayName() + " Relic has been unlocked.");
        } else {
            MessageUtil.info(player, type.displayName() + " Relic is already unlocked.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (RelicType type : RelicType.values()) {
                if (type.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(type.name().toLowerCase());
                }
            }
        }
        return completions;
    }
}
