package com.joshuacolvin.ascendrelics.command;

import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GiveAllCommand implements CommandExecutor {

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

        for (RelicType type : RelicType.values()) {
            player.getInventory().addItem(RelicItemFactory.createRelic(type));
        }
        MessageUtil.success(player, "All " + RelicType.values().length + " relics have been given to you!");
        return true;
    }
}
