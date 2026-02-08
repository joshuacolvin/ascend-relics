package com.joshuacolvin.ascendrelics.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, String abilityName) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(abilityName);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(abilityName);
            return false;
        }
        return true;
    }

    public long getRemainingMillis(UUID playerId, String abilityName) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(abilityName);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void setCooldown(UUID playerId, String abilityName, long durationMillis) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(abilityName, System.currentTimeMillis() + durationMillis);
    }

    public void resetAll(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void resetAll() {
        cooldowns.clear();
    }
}
