package com.joshuacolvin.ascendrelics.manager;

import java.util.*;

public class TrustManager {

    private final Map<UUID, Set<UUID>> trustMap = new HashMap<>();

    public boolean isTrusted(UUID owner, UUID target) {
        Set<UUID> trusted = trustMap.get(owner);
        return trusted != null && trusted.contains(target);
    }

    public boolean addTrust(UUID owner, UUID target) {
        return trustMap.computeIfAbsent(owner, k -> new HashSet<>()).add(target);
    }

    public boolean removeTrust(UUID owner, UUID target) {
        Set<UUID> trusted = trustMap.get(owner);
        if (trusted == null) return false;
        boolean removed = trusted.remove(target);
        if (trusted.isEmpty()) trustMap.remove(owner);
        return removed;
    }

    public Set<UUID> getTrusted(UUID owner) {
        return Collections.unmodifiableSet(trustMap.getOrDefault(owner, Collections.emptySet()));
    }

    public Map<UUID, Set<UUID>> getAllTrustData() {
        return trustMap;
    }

    public void setAllTrustData(Map<UUID, Set<UUID>> data) {
        trustMap.clear();
        trustMap.putAll(data);
    }
}
