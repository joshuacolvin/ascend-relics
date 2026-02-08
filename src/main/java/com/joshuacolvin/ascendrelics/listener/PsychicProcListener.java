package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PsychicProcListener implements Listener {

    private final AscendRelics plugin;
    private final Map<UUID, AttackInfo> recentAttacks = new HashMap<>();

    public PsychicProcListener(AscendRelics plugin) {
        this.plugin = plugin;

        // Check for procs every second
        new BukkitRunnable() {
            @Override
            public void run() {
                var iterator = recentAttacks.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    AttackInfo info = entry.getValue();

                    if (System.currentTimeMillis() - info.timestamp > 10000) {
                        iterator.remove();
                        continue;
                    }

                    Player psychicPlayer = Bukkit.getPlayer(entry.getKey());
                    Player attacker = Bukkit.getPlayer(info.attackerUUID);
                    if (psychicPlayer == null || attacker == null) {
                        iterator.remove();
                        continue;
                    }

                    if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                        // Proc: camera lock
                        attacker.addPotionEffect(new PotionEffect(
                                PotionEffectType.BLINDNESS, 10, 0, false, true, true));

                        new BukkitRunnable() {
                            int ticks = 0;
                            @Override
                            public void run() {
                                ticks++;
                                if (ticks > 60 || !attacker.isOnline() || !psychicPlayer.isOnline()) {
                                    cancel();
                                    return;
                                }
                                Location attackerLoc = attacker.getLocation();
                                Location targetLoc = psychicPlayer.getLocation();
                                Vector dir = targetLoc.toVector().subtract(attackerLoc.toVector()).normalize();
                                float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
                                float pitch = (float) Math.toDegrees(-Math.asin(dir.getY()));
                                attacker.setRotation(yaw, pitch);
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                        iterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPsychicRelic(victim)) return;

        recentAttacks.put(victim.getUniqueId(), new AttackInfo(attacker.getUniqueId()));
    }

    private boolean hasPsychicRelic(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemFactory.identifyRelic(item) == RelicType.PSYCHIC) return true;
        }
        return false;
    }

    private static class AttackInfo {
        final UUID attackerUUID;
        final long timestamp;

        AttackInfo(UUID attacker) {
            this.attackerUUID = attacker;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
