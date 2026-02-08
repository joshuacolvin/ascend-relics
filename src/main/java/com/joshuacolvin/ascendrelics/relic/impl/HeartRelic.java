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
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HeartRelic extends Relic {

    private static final NamespacedKey HEALTH_MODIFIER_KEY =
            NamespacedKey.fromString("ascendrelics:heart_health");

    private final AscendRelics plugin;
    private final HeartPassive passive;
    private final HealingPulseAbility ability1;
    private final LifeLinkAbility ability2;

    public HeartRelic(AscendRelics plugin) {
        super(RelicType.HEART);
        this.plugin = plugin;
        this.passive = new HeartPassive();
        this.ability1 = new HealingPulseAbility();
        this.ability2 = new LifeLinkAbility();
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
            super("Healing Pulse", "Heal self and nearby trusted players", 15);
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

    private class LifeLinkAbility extends ActiveAbility {
        LifeLinkAbility() {
            super("Life Link", "Redirect 30% of target's damage to you for 8s", 30);
        }

        @Override
        public AbilityResult execute(Player player, Plugin bukkitPlugin) {
            LivingEntity target = TargetUtil.raycastLivingEntity(player, 20.0);
            if (!(target instanceof Player targetPlayer) || targetPlayer.equals(player)) {
                return AbilityResult.NO_TARGET;
            }

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            ParticleUtil.line(player.getLocation().add(0, 1, 0),
                    targetPlayer.getLocation().add(0, 1, 0), Particle.HEART, 20);

            LifeLinkListener listener = new LifeLinkListener(player.getUniqueId(), targetPlayer.getUniqueId());
            Bukkit.getPluginManager().registerEvents(listener, HeartRelic.this.plugin);

            new BukkitRunnable() {
                @Override public void run() { HandlerList.unregisterAll(listener); }
            }.runTaskLater(HeartRelic.this.plugin, 160L);

            return AbilityResult.SUCCESS;
        }
    }

    private static class LifeLinkListener implements Listener {
        private final UUID casterId;
        private final UUID targetId;

        LifeLinkListener(UUID caster, UUID target) {
            this.casterId = caster;
            this.targetId = target;
        }

        @EventHandler
        public void onDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player damaged)) return;
            if (!damaged.getUniqueId().equals(targetId)) return;
            Player caster = Bukkit.getPlayer(casterId);
            if (caster == null || !caster.isOnline()) return;

            double original = event.getDamage();
            double redirected = original * 0.30;
            event.setDamage(original - redirected);
            caster.damage(redirected);
            ParticleUtil.line(damaged.getLocation().add(0, 1, 0),
                    caster.getLocation().add(0, 1, 0), Particle.DAMAGE_INDICATOR, 10);
        }
    }
}
