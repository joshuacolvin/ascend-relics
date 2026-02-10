package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeartRelic extends Relic {

    private static final NamespacedKey HEALTH_MODIFIER_KEY =
            NamespacedKey.fromString("ascendrelics:heart_health");

    private final AscendRelics plugin;
    private final HeartPassive passive;
    private final HealingPulseAbility ability1;
    private final GuardianPulseAbility ability2;

    public HeartRelic(AscendRelics plugin) {
        super(RelicType.HEART);
        this.plugin = plugin;
        this.passive = new HeartPassive();
        this.ability1 = new HealingPulseAbility();
        this.ability2 = new GuardianPulseAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class HeartPassive implements PassiveAbility {
        private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
                HEALTH_MODIFIER_KEY, 4.0, AttributeModifier.Operation.ADD_NUMBER
        );

        @Override public String name() { return "Heart Passive"; }
        @Override public String description() { return "+2 extra hearts and Regeneration I"; }

        @Override
        public void apply(Player player) {
            AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && !attr.getModifiers().contains(HEALTH_MODIFIER)) {
                attr.addModifier(HEALTH_MODIFIER);
            }
        }

        @Override
        public void remove(Player player) {
            AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.removeModifier(HEALTH_MODIFIER);
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }

        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, 40, 0, true, false, true));
        }
    }

    private class HealingPulseAbility extends ActiveAbility {
        HealingPulseAbility() {
            super("Healing Pulse", "Heal self and nearby trusted players", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + 6.0, maxHealth));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
            ParticleUtil.sphere(player.getLocation().add(0, 1, 0), Particle.HEART, 1.5, 20);

            AscendRelics ar = AscendRelics.getInstance();
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (entity instanceof Player target && !target.equals(player)) {
                    if (ar.trustManager().isTrusted(player.getUniqueId(), target.getUniqueId())) {
                        double tMax = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                        target.setHealth(Math.min(target.getHealth() + 4.0, tMax));
                        ParticleUtil.sphere(target.getLocation().add(0, 1, 0), Particle.HEART, 1.0, 10);
                    }
                }
            }
            return AbilityResult.SUCCESS;
        }
    }

    /** Players currently under Guardian Pulse (take +20% damage). */
    public static final Set<UUID> guardianCasters = ConcurrentHashMap.newKeySet();
    /** Players currently shielded by Guardian Pulse (take -30% damage). */
    public static final Set<UUID> guardianShielded = ConcurrentHashMap.newKeySet();

    private class GuardianPulseAbility extends ActiveAbility {
        private static final double ALLY_REDUCTION = 0.30;  // allies take 30% less damage
        private static final double CASTER_INCREASE = 0.20; // caster takes 20% more damage
        private static final int DURATION_TICKS = 160;      // 8 seconds
        private static final double RANGE = 15.0;

        GuardianPulseAbility() {
            super("Guardian Pulse", "Allies take 30% less damage, you take 20% more for 8s", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
            ParticleUtil.sphere(player.getLocation().add(0, 1, 0), Particle.HEART, 3.0, 40);

            // Mark caster
            guardianCasters.add(player.getUniqueId());

            // Mark all nearby trusted allies
            AscendRelics ar = AscendRelics.getInstance();
            for (Entity entity : player.getNearbyEntities(RANGE, RANGE, RANGE)) {
                if (entity instanceof Player ally && !ally.equals(player)) {
                    if (ar.trustManager().isTrusted(player.getUniqueId(), ally.getUniqueId())) {
                        guardianShielded.add(ally.getUniqueId());
                        ParticleUtil.sphere(ally.getLocation().add(0, 1, 0), Particle.HEART, 1.0, 10);
                        MessageUtil.info(ally, "You are shielded by " + player.getName() + "'s Guardian Pulse!");
                    }
                }
            }

            MessageUtil.success(player, "Guardian Pulse activated! You absorb damage for your allies.");

            // Remove after duration
            new BukkitRunnable() {
                @Override
                public void run() {
                    guardianCasters.remove(player.getUniqueId());
                    // Remove shielded allies that were in range
                    for (Entity entity : player.getWorld().getPlayers()) {
                        if (entity instanceof Player ally && !ally.equals(player)) {
                            if (ar.trustManager().isTrusted(player.getUniqueId(), ally.getUniqueId())) {
                                guardianShielded.remove(ally.getUniqueId());
                            }
                        }
                    }
                    if (player.isOnline()) {
                        MessageUtil.info(player, "Guardian Pulse has worn off.");
                    }
                }
            }.runTaskLater(HeartRelic.this.plugin, DURATION_TICKS);

            return AbilityResult.SUCCESS;
        }
    }
}
