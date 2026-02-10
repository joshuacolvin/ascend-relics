package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.relic.impl.WaterRelic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TidalGuardListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Projectile)) return;
        if (!WaterRelic.tidalGuardActive.contains(player.getUniqueId())) return;

        // Cancel all projectile damage while Tidal Guard is active
        event.setCancelled(true);
    }
}
