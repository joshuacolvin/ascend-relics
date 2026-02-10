package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
        private static final double RADIUS = 5.0;
        private static final int DURATION_TICKS = 100; // 5 seconds

        FlameBurstAbility() {
            super("Flame Burst", "Fire circle at your feet: damages enemies, heals allies", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

            AscendRelics ar = AscendRelics.getInstance();

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > DURATION_TICKS / 20 || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    Location center = player.getLocation();

                    // Fire ring particles around the caster's feet
                    ParticleUtil.ring(center.clone().add(0, 0.2, 0), Particle.FLAME, RADIUS, 30);
                    ParticleUtil.ring(center.clone().add(0, 0.2, 0), Particle.SMALL_FLAME, RADIUS * 0.6, 15);

                    for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
                        if (entity.equals(player)) continue;

                        if (entity instanceof Player target) {
                            if (ar.trustManager().isTrusted(player.getUniqueId(), target.getUniqueId())) {
                                // Heal trusted allies
                                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                                target.setHealth(Math.min(target.getHealth() + 2.0, maxHealth));
                                target.getWorld().spawnParticle(Particle.HEART,
                                        target.getLocation().add(0, 2, 0), 3, 0.3, 0.2, 0.3, 0);
                            } else {
                                // Damage non-trusted players
                                trueDamage(target, 3.0, player);
                                target.setFireTicks(40);
                            }
                        } else if (entity instanceof LivingEntity living) {
                            // Damage mobs
                            trueDamage(living, 3.0, player);
                            living.setFireTicks(40);
                        }
                    }
                }
            }.runTaskTimer(FireRelic.this.plugin, 0L, 20L);

            return AbilityResult.SUCCESS;
        }
    }

    private class InfernoAbility extends ActiveAbility {
        private static final double BLAST_RADIUS = 7.0;
        private static final double SELF_DAMAGE = 6.0; // 3 hearts

        InfernoAbility() {
            super("Inferno", "Explode at your location, costs 3 hearts", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location center = player.getLocation().clone();
            player.getWorld().playSound(center, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);

            // Warning particles during delay
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 30) { cancel(); return; } // 1.5s delay
                    player.getWorld().spawnParticle(Particle.FLAME,
                            player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02);
                }
            }.runTaskTimer(FireRelic.this.plugin, 0L, 1L);

            // Actual explosion after delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    Location blastCenter = player.getLocation();
                    blastCenter.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, blastCenter, 5, 0, 0, 0, 0);
                    blastCenter.getWorld().spawnParticle(Particle.FLAME, blastCenter, 80, 3, 1.5, 3, 0.15);
                    blastCenter.getWorld().playSound(blastCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                    // Damage self (3 hearts)
                    trueDamage(player, SELF_DAMAGE);

                    // Damage nearby enemies
                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(blastCenter, BLAST_RADIUS, player)) {
                        trueDamage(entity, 8.0, player);
                        entity.setFireTicks(100);
                    }
                }
            }.runTaskLater(FireRelic.this.plugin, 30L);

            return AbilityResult.SUCCESS;
        }
    }
}
