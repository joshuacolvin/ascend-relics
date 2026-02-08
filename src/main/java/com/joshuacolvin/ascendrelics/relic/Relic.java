package com.joshuacolvin.ascendrelics.relic;

import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;

public abstract class Relic {

    private final RelicType type;

    protected Relic(RelicType type) {
        this.type = type;
    }

    public RelicType type() {
        return type;
    }

    public abstract PassiveAbility passive();

    public abstract ActiveAbility ability1();

    public abstract ActiveAbility ability2();
}
