package com.joshuacolvin.ascendrelics.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FreezeListener implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        PotionEffect slowness = damager.getPotionEffect(PotionEffectType.SLOWNESS);
        PotionEffect fatigue = damager.getPotionEffect(PotionEffectType.MINING_FATIGUE);

        if (slowness != null && slowness.getAmplifier() >= 126
                && fatigue != null && fatigue.getAmplifier() >= 126) {
            event.setCancelled(true);
        }
    }
}
