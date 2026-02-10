package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.impl.IceRelic;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class IceCritListener implements Listener {

    private final AscendRelics plugin;

    public IceCritListener(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!hasIceRelic(player)) return;

        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            // Full freeze for 1 second (20 ticks) - no movement, no camera, no knockback
            IceRelic.applyFreeze(target, 20, plugin);
            target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private boolean hasIceRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.ICE) return true;
        }
        return false;
    }
}
