package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class GravityRelic extends Relic {

    private final AscendRelics plugin;
    private final GravityPassive passive = new GravityPassive();
    private final DescendentBlowsAbility ability1;
    private final GravitonShiftAbility ability2;

    public GravityRelic(AscendRelics plugin) {
        super(RelicType.GRAVITY);
        this.plugin = plugin;
        this.ability1 = new DescendentBlowsAbility();
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

    private class DescendentBlowsAbility extends ActiveAbility {
        DescendentBlowsAbility() {
            super("Descendent Blows", "Launch up then slam down for AoE damage", 15);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.setVelocity(new Vector(0, 2.0, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks == 30) {
                        player.setVelocity(new Vector(0, -3.0, 0));
                        cancel();
                    }
                }
            }.runTaskTimer(GravityRelic.this.plugin, 1L, 1L);

            // AoE on impact handled by GravityFallListener
            return AbilityResult.SUCCESS;
        }
    }

    private class GravitonShiftAbility extends ActiveAbility {
        GravitonShiftAbility() {
            super("Graviton Shift", "Pull enemies or unleash Ascendent Blow when charged", 12);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            AscendRelics ar = AscendRelics.getInstance();
            if (ar.ascendentMeterManager().isFullyCharged(player.getUniqueId())) {
                return executeAscendent(player, pluginRef, ar);
            } else {
                return executeDescendent(player, ar);
            }
        }

        private AbilityResult executeDescendent(Player player, AscendRelics ar) {
            List<Player> targets = TargetUtil.getNearbyNonTrusted(player, 8.0, ar.trustManager());
            if (targets.isEmpty()) return AbilityResult.NO_TARGET;

            for (Player target : targets) {
                Vector pull = player.getLocation().toVector()
                        .subtract(target.getLocation().toVector()).normalize().multiply(1.5);
                target.setVelocity(pull);
                target.damage(4.0, player);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 60, 1, false, true, true));
            }

            ParticleUtil.sphere(player.getLocation().add(0, 1, 0), Particle.PORTAL, 8.0, 80);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return AbilityResult.SUCCESS;
        }

        private AbilityResult executeAscendent(Player player, Plugin pluginRef, AscendRelics ar) {
            ar.ascendentMeterManager().reset(player.getUniqueId());
            player.setVelocity(new Vector(0, 3.0, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks == 30) {
                        player.setVelocity(new Vector(0, -9.0, 0));
                        cancel();
                    }
                }
            }.runTaskTimer(pluginRef, 1L, 1L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player target : TargetUtil.getNearbyNonTrusted(player, 8.0, ar.trustManager())) {
                        target.damage(15.0, player);
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 100, 2, false, true, true));
                    }
                    ParticleUtil.sphere(player.getLocation(), Particle.EXPLOSION, 8.0, 120);
                    ParticleUtil.ring(player.getLocation(), Particle.EXPLOSION_EMITTER, 4.0, 16);
                }
            }.runTaskLater(pluginRef, 40L);

            // Override cooldown to 30s
            new BukkitRunnable() {
                @Override public void run() {
                    ar.cooldownManager().setCooldown(player.getUniqueId(), name(), 30000L);
                }
            }.runTaskLater(pluginRef, 1L);

            return AbilityResult.SUCCESS;
        }
    }
}
