package com.joshuacolvin.ascendrelics.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OverrideManager {

    private final Map<UUID, Long> overrides = new HashMap<>();

    public void applyOverride(UUID playerId, long durationMillis) {
        overrides.put(playerId, System.currentTimeMillis() + durationMillis);
    }

    public boolean isOverridden(UUID playerId) {
        Long expiry = overrides.get(playerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            overrides.remove(playerId);
            return false;
        }
        return true;
    }

    public long getRemainingMillis(UUID playerId) {
        Long expiry = overrides.get(playerId);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void remove(UUID playerId) {
        overrides.remove(playerId);
    }
}
