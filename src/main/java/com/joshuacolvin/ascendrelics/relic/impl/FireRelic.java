package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class FireRelic extends Relic {

    private final AscendRelics plugin;
    private final FirePassive passive = new FirePassive();
    private final FlameBurstAbility ability1;
    private final InfernoAbility ability2;

    public FireRelic(AscendRelics plugin) {
        super(RelicType.FIRE);
        this.plugin = plugin;
        this.ability1 = new FlameBurstAbility();
        this.ability2 = new InfernoAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class FirePassive implements PassiveAbility {
        @Override public String name() { return "Fireproof"; }
        @Override public String description() { return "Fire Resistance"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false, true));
        }
    }

    private class FlameBurstAbility extends ActiveAbility {
        FlameBurstAbility() {
            super("Flame Burst", "Create a moving zone of fire", 10);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location start = player.getLocation().clone();
            Vector direction = start.getDirection().setY(0).normalize();
            player.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

            new BukkitRunnable() {
                int step = 0;
                @Override
                public void run() {
                    step++;
                    if (step > 10) { cancel(); return; }

                    Location point = start.clone().add(direction.clone().multiply(step * 1.2));

                    for (int i = 0; i < 10; i++) {
                        double ox = (Math.random() - 0.5) * 3;
                        double oz = (Math.random() - 0.5) * 3;
                        player.getWorld().spawnParticle(Particle.FLAME,
                                point.clone().add(ox, 0.5, oz), 1, 0, 0, 0, 0);
                    }

                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(point, 1.5, player)) {
                        entity.damage(4.0, player);
                        entity.setFireTicks(60);
                    }
                }
            }.runTaskTimer(FireRelic.this.plugin, 0L, 4L);

            return AbilityResult.SUCCESS;
        }
    }

    private class InfernoAbility extends ActiveAbility {
        InfernoAbility() {
            super("Inferno", "Delayed explosion at target location", 30);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location target = TargetUtil.raycastLocation(player, 15.0);
            player.getWorld().playSound(target, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);

            // Warning particles during delay
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 30) { cancel(); return; } // 1.5s delay
                    target.getWorld().spawnParticle(Particle.FLAME, target, 5, 0.5, 0.5, 0.5, 0.02);
                }
            }.runTaskTimer(FireRelic.this.plugin, 0L, 1L);

            // Actual explosion after delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target, 3, 0, 0, 0, 0);
                    target.getWorld().spawnParticle(Particle.FLAME, target, 50, 2, 1, 2, 0.1);
                    target.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(target, 4.0, player)) {
                        entity.damage(8.0, player);
                        entity.setFireTicks(100);
                    }
                }
            }.runTaskLater(FireRelic.this.plugin, 30L);

            return AbilityResult.SUCCESS;
        }
    }
}
