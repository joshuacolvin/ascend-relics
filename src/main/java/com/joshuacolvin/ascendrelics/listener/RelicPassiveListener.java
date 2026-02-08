package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RelicPassiveListener implements Listener {

    private final AscendRelics plugin;
    private final Map<UUID, Set<RelicType>> activePassives = new HashMap<>();

    public RelicPassiveListener(AscendRelics plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Set<RelicType>> activePassives() {
        return activePassives;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshPassives(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeAllPassives(event.getPlayer());
        activePassives.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removeAllPassives(event.getEntity());
        activePassives.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> refreshPassives(player), 1L);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refreshPassives(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> refreshPassives(player), 1L);
        }
    }

    public void refreshPassives(Player player) {
        Set<RelicType> currentRelics = getRelicsInInventory(player);
        Set<RelicType> previousRelics = activePassives.getOrDefault(player.getUniqueId(), EnumSet.noneOf(RelicType.class));

        for (RelicType type : previousRelics) {
            if (!currentRelics.contains(type)) {
                Relic relic = plugin.relicRegistry().get(type);
                if (relic != null && relic.passive() != null) {
                    relic.passive().remove(player);
                }
            }
        }

        for (RelicType type : currentRelics) {
            if (!previousRelics.contains(type)) {
                Relic relic = plugin.relicRegistry().get(type);
                if (relic != null && relic.passive() != null) {
                    relic.passive().apply(player);
                }
            }
        }

        if (currentRelics.isEmpty()) {
            activePassives.remove(player.getUniqueId());
        } else {
            activePassives.put(player.getUniqueId(), currentRelics);
        }
    }

    private void removeAllPassives(Player player) {
        Set<RelicType> current = activePassives.getOrDefault(player.getUniqueId(), EnumSet.noneOf(RelicType.class));
        for (RelicType type : current) {
            Relic relic = plugin.relicRegistry().get(type);
            if (relic != null && relic.passive() != null) {
                relic.passive().remove(player);
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
