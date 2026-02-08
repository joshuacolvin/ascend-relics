package com.joshuacolvin.ascendrelics.data;

import org.bukkit.NamespacedKey;
import com.joshuacolvin.ascendrelics.AscendRelics;

public final class RelicKeys {

    private RelicKeys() {}

    private static NamespacedKey key(String name) {
        return new NamespacedKey(AscendRelics.getInstance(), name);
    }

    public static final NamespacedKey RELIC_TYPE = key("relic_type");
    public static final NamespacedKey RELIC_UUID = key("relic_uuid");
}
