package com.joshuacolvin.ascendrelics.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChainStrikeListener implements Listener {

    private static final Map<String, ChainData> chains = new HashMap<>();
    private static final long CHAIN_DURATION = 10_000L; // 10 seconds

    public static double getMultiplier(UUID attacker, UUID target) {
        String key = attacker + ":" + target;
        ChainData data = chains.get(key);
        if (data == null || data.isExpired()) {
            chains.remove(key);
            return 1.0;
        }
        return Math.min(1.5, 1.0 + (data.hitCount * 0.1));
    }

    public static void incrementChain(UUID attacker, UUID target) {
        String key = attacker + ":" + target;
        ChainData data = chains.get(key);
        if (data == null || data.isExpired()) {
            data = new ChainData();
        }
        data.hitCount++;
        data.expiry = System.currentTimeMillis() + CHAIN_DURATION;
        chains.put(key, data);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        String key = attacker.getUniqueId() + ":" + target.getUniqueId();
        ChainData data = chains.get(key);
        if (data != null && !data.isExpired() && data.hitCount > 0) {
            double multiplier = getMultiplier(attacker.getUniqueId(), target.getUniqueId());
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    private static class ChainData {
        int hitCount = 0;
        long expiry = 0;

        boolean isExpired() {
            return System.currentTimeMillis() >= expiry;
        }
    }
}
