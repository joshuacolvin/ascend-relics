package com.joshuacolvin.ascendrelics.data;

import com.joshuacolvin.ascendrelics.manager.LockManager;
import com.joshuacolvin.ascendrelics.manager.TrustManager;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataManager {

    private final Plugin plugin;
    private final File trustFile;
    private final File locksFile;

    public DataManager(Plugin plugin) {
        this.plugin = plugin;
        this.trustFile = new File(plugin.getDataFolder(), "trust.yml");
        this.locksFile = new File(plugin.getDataFolder(), "locks.yml");
    }

    public void saveTrust(TrustManager trustManager) {
        YamlConfiguration config = new YamlConfiguration();
        Map<UUID, Set<UUID>> allData = trustManager.getAllTrustData();
        for (Map.Entry<UUID, Set<UUID>> entry : allData.entrySet()) {
            List<String> trusted = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                trusted.add(id.toString());
            }
            config.set(entry.getKey().toString(), trusted);
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(trustFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save trust data", e);
        }
    }

    public void loadTrust(TrustManager trustManager) {
        if (!trustFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(trustFile);
        Map<UUID, Set<UUID>> data = new HashMap<>();
        for (String key : config.getKeys(false)) {
            UUID owner = UUID.fromString(key);
            Set<UUID> trusted = new HashSet<>();
            for (String value : config.getStringList(key)) {
                trusted.add(UUID.fromString(value));
            }
            data.put(owner, trusted);
        }
        trustManager.setAllTrustData(data);
    }

    public void saveLocks(LockManager lockManager) {
        YamlConfiguration config = new YamlConfiguration();
        List<String> locked = new ArrayList<>();
        for (RelicType type : lockManager.getLockedRelics()) {
            locked.add(type.name());
        }
        config.set("locked", locked);
        try {
            plugin.getDataFolder().mkdirs();
            config.save(locksFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save lock data", e);
        }
    }

    public void loadLocks(LockManager lockManager) {
        if (!locksFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(locksFile);
        Set<RelicType> locked = EnumSet.noneOf(RelicType.class);
        for (String name : config.getStringList("locked")) {
            try {
                locked.add(RelicType.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
        lockManager.setLockedRelics(locked);
    }

    public void saveAll(TrustManager trustManager, LockManager lockManager) {
        saveTrust(trustManager);
        saveLocks(lockManager);
    }

    public void loadAll(TrustManager trustManager, LockManager lockManager) {
        loadTrust(trustManager);
        loadLocks(lockManager);
    }
}
