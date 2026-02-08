package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20, 4, false, true, true));
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
