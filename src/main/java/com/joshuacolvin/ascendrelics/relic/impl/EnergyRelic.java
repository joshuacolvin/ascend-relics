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
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnergyRelic extends Relic {

    /** Players currently in Overdrive (cannot die). */
    public static final Set<UUID> overdriveActive = ConcurrentHashMap.newKeySet();

    private static final NamespacedKey OVERDRIVE_ATTACK_KEY =
            NamespacedKey.fromString("ascendrelics:overdrive_attack");

    private final AscendRelics plugin;
    private final EnergyPassive passive = new EnergyPassive();
    private final PulseBeamAbility ability1;
    private final OverdriveAbility ability2;

    public EnergyRelic(AscendRelics plugin) {
        super(RelicType.ENERGY);
        this.plugin = plugin;
        this.ability1 = new PulseBeamAbility();
        this.ability2 = new OverdriveAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class EnergyPassive implements PassiveAbility {
        @Override public String name() { return "Surge"; }
        @Override public String description() { return "Speed II"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 40, 1, true, false, true));
        }
    }

    private class PulseBeamAbility extends ActiveAbility {
        PulseBeamAbility() {
            super("Pulse Beam", "Fire an energy beam for 6 seconds", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 120 || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    Location eye = player.getEyeLocation();
                    Vector dir = eye.getDirection();

                    // Spawn beam particles
                    for (int i = 1; i <= 20; i++) {
                        Location point = eye.clone().add(dir.clone().multiply(i));
                        player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
                    }

                    // Raycast for entity hit
                    RayTraceResult result = player.getWorld().rayTraceEntities(
                            eye, dir, 20.0, 0.5,
                            e -> e instanceof LivingEntity && e != player
                    );
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        trueDamage(target, 2.0, player);
                    }
                }
            }.runTaskTimer(EnergyRelic.this.plugin, 0L, 20L);

            return AbilityResult.SUCCESS;
        }
    }

    private class OverdriveAbility extends ActiveAbility {
        private static final AttributeModifier ATTACK_MODIFIER = new AttributeModifier(
                OVERDRIVE_ATTACK_KEY, 2.0, AttributeModifier.Operation.ADD_NUMBER
        );

        OverdriveAbility() {
            super("Overdrive", "Speed III + Haste II + Attack Damage for 8s, then debuffs", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 160, 1, false, true, true));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 2.0f);

            // Add +2 attack damage
            AttributeInstance attackAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackAttr != null && !attackAttr.getModifiers().contains(ATTACK_MODIFIER)) {
                attackAttr.addModifier(ATTACK_MODIFIER);
            }

            // Mark player as unkillable
            overdriveActive.add(player.getUniqueId());

            // End overdrive after 8 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    overdriveActive.remove(player.getUniqueId());
                    if (!player.isOnline()) return;

                    // Remove attack damage modifier
                    AttributeInstance attr = player.getAttribute(Attribute.ATTACK_DAMAGE);
                    if (attr != null) attr.removeModifier(ATTACK_MODIFIER);

                    // Apply debuffs: Slowness II and Weakness II for 3 seconds
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 60, 1, false, true, true));
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.WEAKNESS, 60, 1, false, true, true));
                }
            }.runTaskLater(EnergyRelic.this.plugin, 160L);

            return AbilityResult.SUCCESS;
        }
    }
}
