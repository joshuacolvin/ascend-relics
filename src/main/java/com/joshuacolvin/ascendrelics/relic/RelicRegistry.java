package com.joshuacolvin.ascendrelics.relic;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class RelicRegistry {

    private final Map<RelicType, Relic> relics = new EnumMap<>(RelicType.class);

    public void register(Relic relic) {
        relics.put(relic.type(), relic);
    }

    public Relic get(RelicType type) {
        return relics.get(type);
    }

    public Collection<Relic> all() {
        return relics.values();
    }
}
