package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class AcidHitTracker implements Listener {

    private final AscendRelics plugin;
    private final Map<String, Integer> hitCounts = new HashMap<>();

    public AcidHitTracker(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!hasAcidRelic(player)) return;

        String key = player.getUniqueId() + ":" + target.getUniqueId();
        int count = hitCounts.getOrDefault(key, 0) + 1;

        if (count >= 15) {
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.HUNGER, 100, 2, false, true, true));
            hitCounts.remove(key);
        } else {
            hitCounts.put(key, count);
        }
    }

    private boolean hasAcidRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.ACID) return true;
        }
        return false;
    }
}
