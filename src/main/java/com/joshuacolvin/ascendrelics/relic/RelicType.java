package com.joshuacolvin.ascendrelics.relic;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum RelicType {
    EARTH("Earth", NamedTextColor.DARK_GREEN, 1001),
    METAL("Metal", NamedTextColor.GRAY, 1002),
    ICE("Ice", NamedTextColor.AQUA, 1003),
    PSYCHIC("Psychic", NamedTextColor.LIGHT_PURPLE, 1004),
    ENERGY("Energy", NamedTextColor.YELLOW, 1005),
    GHOST("Ghost", NamedTextColor.WHITE, 1006),
    HEART("Heart", NamedTextColor.RED, 1007),
    ACID("Acid", NamedTextColor.GREEN, 1008),
    LIGHTNING("Lightning", NamedTextColor.GOLD, 1009),
    WATER("Water", NamedTextColor.BLUE, 1010),
    FIRE("Fire", NamedTextColor.DARK_RED, 1011),
    GRAVITY("Gravity", NamedTextColor.DARK_PURPLE, 1012);

    private final String displayName;
    private final TextColor color;
    private final int customModelData;

    RelicType(String displayName, TextColor color, int customModelData) {
        this.displayName = displayName;
        this.color = color;
        this.customModelData = customModelData;
    }

    public String displayName() {
        return displayName;
    }

    public TextColor color() {
        return color;
    }

    public int customModelData() {
        return customModelData;
    }

    public static RelicType fromName(String name) {
        for (RelicType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
