package com.joshuacolvin.ascendrelics.command;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GiveAllCommand implements CommandExecutor {

    private final AscendRelics plugin;

    public GiveAllCommand(AscendRelics plugin) {
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

        int count = 0;
        for (Relic relic : plugin.relicRegistry().all()) {
            player.getInventory().addItem(RelicItemFactory.createRelic(relic));
            count++;
        }
        MessageUtil.success(player, "All " + count + " relics have been given to you!");
        return true;
    }
}
