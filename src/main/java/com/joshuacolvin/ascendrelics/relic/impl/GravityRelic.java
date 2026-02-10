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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GravityRelic extends Relic {

    /** Players affected by Descendent Blows — take 30% more damage. */
    public static final Set<UUID> descendentVulnerable = ConcurrentHashMap.newKeySet();

    private final AscendRelics plugin;
    private final GravityPassive passive = new GravityPassive();
    private final GravitatingSlamAbility ability1;
    private final GravitonShiftAbility ability2;

    public GravityRelic(AscendRelics plugin) {
        super(RelicType.GRAVITY);
        this.plugin = plugin;
        this.ability1 = new GravitatingSlamAbility();
        this.ability2 = new GravitonShiftAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class GravityPassive implements PassiveAbility {
        @Override public String name() { return "Graviton Field"; }
        @Override public String description() { return "Fall damage creates AoE and charges meter"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private class GravitatingSlamAbility extends ActiveAbility {
        private static final double PULL_RADIUS = 10.0;

        GravitatingSlamAbility() {
            super("Gravitating Slam", "Pull in enemies, launch them up, then slam them down", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            AscendRelics ar = AscendRelics.getInstance();
            List<Player> targets = TargetUtil.getNearbyNonTrusted(player, PULL_RADIUS, ar.trustManager());
            if (targets.isEmpty()) return AbilityResult.NO_TARGET;

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
            ParticleUtil.sphere(player.getLocation().add(0, 1, 0), Particle.PORTAL, PULL_RADIUS, 60);

            // Phase 1: Pull enemies toward the caster
            for (Player target : targets) {
                Vector pull = player.getLocation().toVector()
                        .subtract(target.getLocation().toVector()).normalize().multiply(2.0);
                target.setVelocity(pull);
            }

            // Phase 2: After 15 ticks, launch them up
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
                    for (Player target : targets) {
                        if (target.isOnline()) {
                            target.setVelocity(new Vector(0, 2.5, 0));
                            target.getWorld().spawnParticle(Particle.PORTAL,
                                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                        }
                    }
                }
            }.runTaskLater(GravityRelic.this.plugin, 15L);

            // Phase 3: After 35 ticks (20 more), slam them down and deal damage
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    for (Player target : targets) {
                        if (target.isOnline()) {
                            target.setVelocity(new Vector(0, -3.5, 0));
                            trueDamage(target, 6.0, player);
                            target.getWorld().spawnParticle(Particle.CRIT,
                                    target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                    ParticleUtil.ring(player.getLocation(), Particle.CRIT, PULL_RADIUS * 0.5, 30);
                }
            }.runTaskLater(GravityRelic.this.plugin, 35L);

            return AbilityResult.SUCCESS;
        }
    }

    private class GravitonShiftAbility extends ActiveAbility {
        private static final int DESCENDENT_DURATION_TICKS = 160; // 8 seconds
        private static final double VULNERABILITY_BONUS = 0.30;   // 30% more damage taken
        private static final double ASCENDENT_BLAST_RADIUS = 8.0;
        private static final double ASCENDENT_MAX_DAMAGE = 14.0;  // 7 hearts

        GravitonShiftAbility() {
            super("Graviton Shift", "Descendent Blows or Ascendent Blow when charged", 150);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            AscendRelics ar = AscendRelics.getInstance();
            if (ar.ascendentMeterManager().isFullyCharged(player.getUniqueId())) {
                return executeAscendent(player, pluginRef, ar);
            } else {
                return executeDescendent(player, pluginRef, ar);
            }
        }

        // Non-Charged: Slow Falling + vulnerability debuff on nearby enemies
        private AbilityResult executeDescendent(Player player, Plugin pluginRef, AscendRelics ar) {
            List<Player> targets = TargetUtil.getNearbyNonTrusted(player, 10.0, ar.trustManager());
            if (targets.isEmpty()) return AbilityResult.NO_TARGET;

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            ParticleUtil.sphere(player.getLocation().add(0, 1, 0), Particle.PORTAL, 10.0, 80);

            for (Player target : targets) {
                // Slow Falling
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING, DESCENDENT_DURATION_TICKS, 0, false, true, true));
                // Mark as vulnerable
                descendentVulnerable.add(target.getUniqueId());
                MessageUtil.error(target, "Gravity weakens your defenses!");
            }

            MessageUtil.success(player, "Descendent Blows activated!");

            // Remove vulnerability after duration
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player target : targets) {
                        descendentVulnerable.remove(target.getUniqueId());
                    }
                }
            }.runTaskLater(GravityRelic.this.plugin, DESCENDENT_DURATION_TICKS);

            return AbilityResult.SUCCESS;
        }

        // Charged: Launch up, aim, slam down for up to 7 hearts
        private AbilityResult executeAscendent(Player player, Plugin pluginRef, AscendRelics ar) {
            ar.ascendentMeterManager().reset(player.getUniqueId());
            MessageUtil.success(player, "Ascendent Blow! Aim where you want to land!");

            // Launch the player up high
            player.setVelocity(new Vector(0, 3.5, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);

            // Give slow falling briefly so they can aim
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING, 60, 0, false, false, true));

            // After 2.5 seconds, slam them to where they're looking
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    // Remove slow falling
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);

                    // Raycast to find ground target from where player is looking
                    Location slamTarget = TargetUtil.raycastLocation(player, 30.0);

                    // Teleport to slam location
                    slamTarget.setYaw(player.getLocation().getYaw());
                    slamTarget.setPitch(player.getLocation().getPitch());
                    player.teleport(slamTarget);
                    player.setVelocity(new Vector(0, -1.0, 0));
                    // Cancel fall damage for the caster
                    player.setFallDistance(0);

                    // Blast on impact
                    slamTarget.getWorld().playSound(slamTarget, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                    ParticleUtil.sphere(slamTarget, Particle.EXPLOSION, ASCENDENT_BLAST_RADIUS, 120);
                    ParticleUtil.ring(slamTarget, Particle.EXPLOSION_EMITTER, 4.0, 16);

                    // Damage nearby non-trusted players (distance-based: closer = more damage)
                    for (Player target : TargetUtil.getNearbyNonTrusted(player, ASCENDENT_BLAST_RADIUS, ar.trustManager())) {
                        double dist = target.getLocation().distance(slamTarget);
                        // Scale: full damage at center, half at edge
                        double scale = 1.0 - (dist / (ASCENDENT_BLAST_RADIUS * 2));
                        double damage = Math.max(ASCENDENT_MAX_DAMAGE * 0.5, ASCENDENT_MAX_DAMAGE * scale);
                        trueDamage(target, damage, player);
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 60, 2, false, true, true));
                    }
                }
            }.runTaskLater(GravityRelic.this.plugin, 50L);

            return AbilityResult.SUCCESS;
        }
    }
}
