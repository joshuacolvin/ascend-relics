package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.ParticleUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class AcidRelic extends Relic {

    private final AscendRelics plugin;
    private final AcidPassive passive = new AcidPassive();
    private final CorrosiveMistAbility ability1;
    private final AcidRainAbility ability2;

    public AcidRelic(AscendRelics plugin) {
        super(RelicType.ACID);
        this.plugin = plugin;
        this.ability1 = new CorrosiveMistAbility();
        this.ability2 = new AcidRainAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class AcidPassive implements PassiveAbility {
        @Override public String name() { return "Corrosive Touch"; }
        @Override public String description() { return "Every 15th hit applies Hunger III"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private class CorrosiveMistAbility extends ActiveAbility {
        CorrosiveMistAbility() {
            super("Corrosive Mist", "Create a damaging acid cloud", 15);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location center = TargetUtil.raycastLocation(player, 8.0);
            player.getWorld().playSound(center, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 6) { cancel(); return; }

                    ParticleUtil.cloud(center, Particle.ITEM_SLIME, 4.0, 30);

                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(center, 4.0, player)) {
                        entity.damage(2.0, player);
                        // Damage armor durability
                        if (entity.getEquipment() != null) {
                            for (ItemStack armor : entity.getEquipment().getArmorContents()) {
                                if (armor != null && armor.getType().getMaxDurability() > 0) {
                                    ItemMeta meta = armor.getItemMeta();
                                    if (meta instanceof Damageable damageable) {
                                        damageable.setDamage(damageable.getDamage() + 1);
                                        armor.setItemMeta(meta);
                                    }
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(AcidRelic.this.plugin, 0L, 20L);

            return AbilityResult.SUCCESS;
        }
    }

    private class AcidRainAbility extends ActiveAbility {
        AcidRainAbility() {
            super("Acid Rain", "Rain acid in a target area", 25);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location center = TargetUtil.raycastLocation(player, 15.0);
            player.getWorld().playSound(center, Sound.WEATHER_RAIN, 1.0f, 0.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 4) { cancel(); return; }

                    // Rain particles from above
                    for (int i = 0; i < 15; i++) {
                        double ox = (Math.random() - 0.5) * 12;
                        double oz = (Math.random() - 0.5) * 12;
                        center.getWorld().spawnParticle(Particle.FALLING_WATER,
                                center.clone().add(ox, 10, oz), 3, 0, 0, 0, 0);
                    }
                    ParticleUtil.cloud(center, Particle.ITEM_SLIME, 6.0, 20);

                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(center, 6.0, player)) {
                        entity.damage(3.0, player);
                        entity.addPotionEffect(new PotionEffect(
                                PotionEffectType.POISON, 60, 0, false, true, true));
                    }
                }
            }.runTaskTimer(AcidRelic.this.plugin, 0L, 20L);

            return AbilityResult.SUCCESS;
        }
    }
}
