package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.relic.impl.EnergyRelic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class OverdriveListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!EnergyRelic.overdriveActive.contains(player.getUniqueId())) return;

        // If this damage would kill the player, cap health at half a heart instead
        double healthAfter = player.getHealth() - event.getFinalDamage();
        if (healthAfter <= 0) {
            event.setCancelled(true);
            player.setHealth(1.0); // half a heart
        }
    }
}
