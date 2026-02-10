package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaterRelic extends Relic {

    /** Players currently shielded by Tidal Guard (immune to projectiles). */
    public static final Set<UUID> tidalGuardActive = ConcurrentHashMap.newKeySet();

    private final AscendRelics plugin;
    private final WaterPassive passive = new WaterPassive();
    private final WhirlpoolAbility ability1;
    private final TidalGuardAbility ability2;

    public WaterRelic(AscendRelics plugin) {
        super(RelicType.WATER);
        this.plugin = plugin;
        this.ability1 = new WhirlpoolAbility();
        this.ability2 = new TidalGuardAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class WaterPassive implements PassiveAbility {
        @Override public String name() { return "Ocean's Grace"; }
        @Override public String description() { return "Conduit Power"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {
            player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
        }
        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONDUIT_POWER, 40, 0, true, false, true));
        }
    }

    private class WhirlpoolAbility extends ActiveAbility {
        WhirlpoolAbility() {
            super("Whirlpool", "Create a vortex that pulls enemies in", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location center = TargetUtil.raycastLocation(player, 10.0);
            player.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_HURT, 1.0f, 0.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 100) { cancel(); return; } // 5 seconds

                    ParticleUtil.ring(center, Particle.SPLASH, 3.0, 20);
                    ParticleUtil.ring(center, Particle.BUBBLE, 5.0, 15);

                    for (Entity entity : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            Vector pull = center.toVector().subtract(living.getLocation().toVector())
                                    .normalize().multiply(0.3);
                            pull.setY(0.1);
                            living.setVelocity(living.getVelocity().add(pull));

                            if (ticks % 20 == 0) {
                                trueDamage(living, 1.0, player);
                            }
                        }
                    }
                }
            }.runTaskTimer(WaterRelic.this.plugin, 0L, 1L);

            return AbilityResult.SUCCESS;
        }
    }

    private class TidalGuardAbility extends ActiveAbility {
        private static final int DURATION_TICKS = 120; // 6 seconds

        TidalGuardAbility() {
            super("Tidal Guard", "60% damage reduction + projectile immunity for 6s", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            // Resistance II = 60% damage reduction (buffed from 40%)
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, DURATION_TICKS, 2, false, true, true));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.2f);

            // Mark as projectile immune
            tidalGuardActive.add(player.getUniqueId());
            MessageUtil.success(player, "Tidal Guard activated!");

            // Spinning water particle visual
            new BukkitRunnable() {
                int ticks = 0;
                double angle = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > DURATION_TICKS || !player.isOnline()) { cancel(); return; }
                    angle += 0.3;
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    player.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                            player.getLocation().add(x, 1, z), 3, 0, 0, 0, 0);
                }
            }.runTaskTimer(WaterRelic.this.plugin, 0L, 1L);

            // Remove projectile immunity after duration
            new BukkitRunnable() {
                @Override
                public void run() {
                    tidalGuardActive.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        MessageUtil.info(player, "Tidal Guard has worn off.");
                    }
                }
            }.runTaskLater(WaterRelic.this.plugin, DURATION_TICKS);

            return AbilityResult.SUCCESS;
        }
    }
}
