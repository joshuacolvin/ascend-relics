package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            trueDamage(target, aoeDamage, player);
            totalDamage += aoeDamage;
        }

        if (totalDamage > 0) {
            double heartsDealt = totalDamage / 2.0;
            double chargeGain = heartsDealt * 15.0;
            plugin.ascendentMeterManager().addCharge(player.getUniqueId(), chargeGain);

            // Display meter on action bar
            if (plugin.ascendentMeterManager().isFullyCharged(player.getUniqueId())) {
                player.sendActionBar(Component.text("Ascendent Meter: FULLY CHARGED!", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                int percent = (int) (plugin.ascendentMeterManager().getChargePercent(player.getUniqueId()) * 100);
                player.sendActionBar(Component.text("Ascendent Meter: " + percent + "%", NamedTextColor.LIGHT_PURPLE));
            }
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
