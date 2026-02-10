package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.relic.impl.HeartRelic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class GuardianPulseListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Caster takes 20% more damage
        if (HeartRelic.guardianCasters.contains(player.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.20);
        }

        // Shielded allies take 30% less damage
        if (HeartRelic.guardianShielded.contains(player.getUniqueId())) {
            event.setDamage(event.getDamage() * 0.70);
        }
    }
}
