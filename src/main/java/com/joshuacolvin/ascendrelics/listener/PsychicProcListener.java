package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.Bukkit;
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

    // Tracks when the next 5% roll can happen for each psychic player
    private final Map<UUID, Long> nextRollTime = new HashMap<>();
    // Tracks the last attacker of each psychic player
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    private static final long ROLL_INTERVAL_MS = 10000; // 10 seconds between rolls
    private static final int LOCK_DURATION_TICKS = 60; // 3 seconds camera lock

    public PsychicProcListener(AscendRelics plugin) {
        this.plugin = plugin;

        // Check for procs every second
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                var iterator = lastAttacker.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    UUID psychicId = entry.getKey();
                    UUID attackerId = entry.getValue();

                    // Check if enough time has passed for a roll
                    Long nextRoll = nextRollTime.get(psychicId);
                    if (nextRoll != null && now < nextRoll) continue;

                    Player psychicPlayer = Bukkit.getPlayer(psychicId);
                    Player attacker = Bukkit.getPlayer(attackerId);
                    if (psychicPlayer == null || attacker == null) {
                        iterator.remove();
                        nextRollTime.remove(psychicId);
                        continue;
                    }

                    // Roll 5% chance
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                        // Lock camera in place for 3 seconds
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
                    }

                    // Whether it procced or not, wait another 10 seconds before next roll
                    nextRollTime.put(psychicId, now + ROLL_INTERVAL_MS);
                    iterator.remove(); // Clear attacker, needs a fresh hit to retrigger
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPsychicRelic(victim)) return;

        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());

        // If no roll is scheduled yet, allow immediate first roll
        if (!nextRollTime.containsKey(victim.getUniqueId())) {
            nextRollTime.put(victim.getUniqueId(), 0L);
        }
    }

    private boolean hasPsychicRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.PSYCHIC) return true;
        }
        return false;
    }
}
