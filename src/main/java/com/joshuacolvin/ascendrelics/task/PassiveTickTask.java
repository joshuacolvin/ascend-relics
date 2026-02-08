package com.joshuacolvin.ascendrelics.task;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

public class PassiveTickTask extends BukkitRunnable {

    private final AscendRelics plugin;

    public PassiveTickTask(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<RelicType> relics = getRelicsInInventory(player);
            for (RelicType type : relics) {
                Relic relic = plugin.relicRegistry().get(type);
                if (relic != null && relic.passive() != null) {
                    relic.passive().tick(player);
                }
            }
        }
    }

    private Set<RelicType> getRelicsInInventory(Player player) {
        Set<RelicType> relics = EnumSet.noneOf(RelicType.class);
        for (ItemStack item : player.getInventory().getContents()) {
            RelicType type = RelicItemFactory.identifyRelic(item);
            if (type != null) {
                relics.add(type);
            }
        }
        return relics;
    }
}
