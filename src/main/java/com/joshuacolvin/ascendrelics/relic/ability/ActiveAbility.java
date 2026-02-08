package com.joshuacolvin.ascendrelics.relic.ability;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class ActiveAbility implements Ability {

    private final String name;
    private final String description;
    private final long cooldownMillis;

    protected ActiveAbility(String name, String description, long cooldownSeconds) {
        this.name = name;
        this.description = description;
        this.cooldownMillis = cooldownSeconds * 1000L;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public abstract AbilityResult execute(Player player, Plugin plugin);
}
