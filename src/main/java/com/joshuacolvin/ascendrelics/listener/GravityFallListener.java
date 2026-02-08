package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GravityFallListener implements Listener {

    private final AscendRelics plugin;

    public GravityFallListener(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasGravityRelic(player)) return;

        float fallDistance = player.getFallDistance();
        double aoeDamage = fallDistance * 0.5;

        List<Player> nearby = TargetUtil.getNearbyNonTrusted(player, 4.0, plugin.trustManager());

        double totalDamage = 0;
        for (Player target : nearby) {
            target.damage(aoeDamage, player);
            totalDamage += aoeDamage;
        }

        if (totalDamage > 0) {
            double heartsDealt = totalDamage / 2.0;
            double chargeGain = heartsDealt * 15.0;
            plugin.ascendentMeterManager().addCharge(player.getUniqueId(), chargeGain);
        }

        ParticleUtil.ring(player.getLocation(), Particle.CRIT, 4.0, 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
    }

    private boolean hasGravityRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.GRAVITY) return true;
        }
        return false;
    }
}
