package com.joshuacolvin.ascendrelics;

import com.joshuacolvin.ascendrelics.command.*;
import com.joshuacolvin.ascendrelics.data.DataManager;
import com.joshuacolvin.ascendrelics.listener.*;
import com.joshuacolvin.ascendrelics.manager.*;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicRegistry;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.impl.*;
import com.joshuacolvin.ascendrelics.task.PassiveTickTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AscendRelics extends JavaPlugin {

    private static AscendRelics instance;

    private RelicRegistry relicRegistry;
    private CooldownManager cooldownManager;
    private TrustManager trustManager;
    private LockManager lockManager;
    private OverrideManager overrideManager;
    private AscendentMeterManager ascendentMeterManager;
    private DataManager dataManager;

    private final Map<UUID, ActiveAbility> lastAbilityUsed = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        cooldownManager = new CooldownManager();
        trustManager = new TrustManager();
        lockManager = new LockManager();
        overrideManager = new OverrideManager();
        ascendentMeterManager = new AscendentMeterManager();
        dataManager = new DataManager(this);

        dataManager.loadAll(trustManager, lockManager);

        relicRegistry = new RelicRegistry();
        registerRelics();

        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("AscendRelics enabled! " + relicRegistry.all().size() + " relics registered.");
    }

    @Override
    public void onDisable() {
        dataManager.saveAll(trustManager, lockManager);
        getLogger().info("AscendRelics disabled.");
    }

    private void registerRelics() {
        relicRegistry.register(new EarthRelic(this));
        relicRegistry.register(new MetalRelic(this));
        relicRegistry.register(new IceRelic(this));
        relicRegistry.register(new PsychicRelic(this));
        relicRegistry.register(new EnergyRelic(this));
        relicRegistry.register(new GhostRelic(this));
        relicRegistry.register(new HeartRelic(this));
        relicRegistry.register(new AcidRelic(this));
        relicRegistry.register(new LightningRelic(this));
        relicRegistry.register(new WaterRelic(this));
        relicRegistry.register(new FireRelic(this));
        relicRegistry.register(new GravityRelic(this));
    }

    private void registerCommands() {
        Ability1Command ability1Cmd = new Ability1Command(this);
        Ability2Command ability2Cmd = new Ability2Command(this);
        TrustCommand trustCmd = new TrustCommand(this);
        GiveAllCommand giveAllCmd = new GiveAllCommand();
        CooldownResetCommand cooldownCmd = new CooldownResetCommand(this);
        LockCommand lockCmd = new LockCommand(this);
        UnlockCommand unlockCmd = new UnlockCommand(this);

        getCommand("ability1").setExecutor(ability1Cmd);
        getCommand("ability2").setExecutor(ability2Cmd);
        getCommand("trust").setExecutor(trustCmd);
        getCommand("trust").setTabCompleter(trustCmd);
        getCommand("giveall").setExecutor(giveAllCmd);
        getCommand("cooldown").setExecutor(cooldownCmd);
        getCommand("cooldown").setTabCompleter(cooldownCmd);
        getCommand("lock").setExecutor(lockCmd);
        getCommand("lock").setTabCompleter(lockCmd);
        getCommand("unlock").setExecutor(unlockCmd);
        getCommand("unlock").setTabCompleter(unlockCmd);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new RelicPassiveListener(this), this);
        pm.registerEvents(new WorldAnchorListener(), this);
        pm.registerEvents(new ChainStrikeListener(), this);
        pm.registerEvents(new AcidHitTracker(this), this);
        pm.registerEvents(new GravityFallListener(this), this);
        pm.registerEvents(new FreezeListener(), this);
        pm.registerEvents(new IceCritListener(this), this);
        pm.registerEvents(new LightningProcListener(this), this);
        pm.registerEvents(new PsychicProcListener(this), this);
    }

    private void startTasks() {
        new PassiveTickTask(this).runTaskTimer(this, 20L, 20L);
    }

    public static AscendRelics getInstance() {
        return instance;
    }

    public RelicRegistry relicRegistry() { return relicRegistry; }
    public CooldownManager cooldownManager() { return cooldownManager; }
    public TrustManager trustManager() { return trustManager; }
    public LockManager lockManager() { return lockManager; }
    public OverrideManager overrideManager() { return overrideManager; }
    public AscendentMeterManager ascendentMeterManager() { return ascendentMeterManager; }
    public DataManager dataManager() { return dataManager; }
    public Map<UUID, ActiveAbility> lastAbilityUsed() { return lastAbilityUsed; }
}
