package com.joshuacolvin.ascendrelics.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class FreezeListener implements Listener {

    private final Plugin plugin;

    public FreezeListener(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancel outgoing attacks from frozen players.
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        if (isFrozen(damager)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancel knockback on frozen players by zeroing velocity after any damage.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        if (isFrozen(entity)) {
            // Zero velocity next tick to cancel knockback
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> entity.setVelocity(new Vector(0, Math.min(0, entity.getVelocity().getY()), 0)),
                    1L
            );
        }
    }

    private boolean isFrozen(LivingEntity entity) {
        PotionEffect slowness = entity.getPotionEffect(PotionEffectType.SLOWNESS);
        PotionEffect fatigue = entity.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        return slowness != null && slowness.getAmplifier() >= 126
                && fatigue != null && fatigue.getAmplifier() >= 126;
    }
}
