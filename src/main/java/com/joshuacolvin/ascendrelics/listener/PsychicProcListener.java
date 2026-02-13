package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PsychicProcListener implements Listener {

    private final AscendRelics plugin;

    // Tracks hit count for stacking proc chance
    private final Map<UUID, Integer> hitCount = new HashMap<>();
    // Tracks cooldown expiry time after a proc
    private final Map<UUID, Long> procCooldown = new HashMap<>();

    private static final long PROC_COOLDOWN_MS = 5000; // 5 seconds after proc
    private static final int LOCK_DURATION_TICKS = 60; // 3 seconds camera lock

    public PsychicProcListener(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPsychicRelic(victim)) return;

        UUID victimId = victim.getUniqueId();

        // Check if on cooldown
        Long cooldownExpiry = procCooldown.get(victimId);
        if (cooldownExpiry != null) {
            if (System.currentTimeMillis() < cooldownExpiry) {
                return; // Still on cooldown, hits don't count
            }
            // Cooldown expired, reset counter
            procCooldown.remove(victimId);
            hitCount.put(victimId, 0);
        }

        // Increment hit count
        int count = hitCount.getOrDefault(victimId, 0) + 1;
        hitCount.put(victimId, count);

        // Roll with count * 5% chance
        double chance = count * 0.05;
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            // Proc! Camera lock the attacker
            float lockedYaw = attacker.getLocation().getYaw();
            float lockedPitch = attacker.getLocation().getPitch();

            attacker.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, 10, 0, false, false, true));
            attacker.getWorld().playSound(attacker.getLocation(),
                    Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 1.5f);
            attacker.getWorld().spawnParticle(Particle.WITCH,
                    attacker.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > LOCK_DURATION_TICKS || !attacker.isOnline()) {
                        cancel();
                        return;
                    }
                    attacker.setRotation(lockedYaw, lockedPitch);
                }
            }.runTaskTimer(plugin, 0L, 1L);

            // Reset hit count and set 5s cooldown
            hitCount.put(victimId, 0);
            procCooldown.put(victimId, System.currentTimeMillis() + PROC_COOLDOWN_MS);
        }
    }

    private boolean hasPsychicRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.PSYCHIC) return true;
        }
        return false;
    }
}
