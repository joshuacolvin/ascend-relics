package com.joshuacolvin.ascendrelics.data;

import com.joshuacolvin.ascendrelics.relic.RelicType;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private final Set<RelicType> activePassives = EnumSet.noneOf(RelicType.class);

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    public Set<RelicType> activePassives() {
        return activePassives;
    }
}
