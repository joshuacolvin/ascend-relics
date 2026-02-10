package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.relic.impl.LightningRelic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ChainStrikeListener implements Listener {

    private static final double MAX_MULTIPLIER = 1.5;

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Integer hitCount = LightningRelic.chainStrikeActive.get(attacker.getUniqueId());
        if (hitCount == null) return;

        // Increment hit count
        int newCount = hitCount + 1;
        LightningRelic.chainStrikeActive.put(attacker.getUniqueId(), newCount);

        // Calculate multiplier: 1.0 + (hits * 0.1), capped at 1.5x
        double multiplier = Math.min(MAX_MULTIPLIER, 1.0 + (newCount * 0.1));
        event.setDamage(event.getDamage() * multiplier);

        // Strike lightning on every hit during Chain Strike
        target.getWorld().strikeLightningEffect(target.getLocation());
    }
}
