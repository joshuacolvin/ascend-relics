package com.joshuacolvin.ascendrelics.relic.ability;

import org.bukkit.entity.Player;

public interface PassiveAbility extends Ability {

    void apply(Player player);

    void remove(Player player);

    void tick(Player player);
}
