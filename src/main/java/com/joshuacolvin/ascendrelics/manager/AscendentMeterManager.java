package com.joshuacolvin.ascendrelics.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AscendentMeterManager {

    private static final double MAX_CHARGE = 100.0;

    private final Map<UUID, Double> meters = new HashMap<>();

    public double getCharge(UUID playerId) {
        return meters.getOrDefault(playerId, 0.0);
    }

    public void addCharge(UUID playerId, double amount) {
        double current = getCharge(playerId);
        meters.put(playerId, Math.min(MAX_CHARGE, current + amount));
    }

    public boolean isFullyCharged(UUID playerId) {
        return getCharge(playerId) >= MAX_CHARGE;
    }

    public void reset(UUID playerId) {
        meters.put(playerId, 0.0);
    }

    public double getChargePercent(UUID playerId) {
        return getCharge(playerId) / MAX_CHARGE;
    }
}
