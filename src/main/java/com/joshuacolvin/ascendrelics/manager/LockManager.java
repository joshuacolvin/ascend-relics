package com.joshuacolvin.ascendrelics.manager;

import com.joshuacolvin.ascendrelics.relic.RelicType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class LockManager {

    private final Set<RelicType> lockedRelics = EnumSet.noneOf(RelicType.class);

    public boolean isLocked(RelicType type) {
        return lockedRelics.contains(type);
    }

    public boolean lock(RelicType type) {
        return lockedRelics.add(type);
    }

    public boolean unlock(RelicType type) {
        return lockedRelics.remove(type);
    }

    public Set<RelicType> getLockedRelics() {
        return Collections.unmodifiableSet(lockedRelics);
    }

    public void setLockedRelics(Set<RelicType> relics) {
        lockedRelics.clear();
        lockedRelics.addAll(relics);
    }
}
