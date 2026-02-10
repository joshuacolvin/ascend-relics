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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class AcidRelic extends Relic {

    private final AscendRelics plugin;
    private final AcidPassive passive = new AcidPassive();
    private final AcidRainAbility ability1;
    private final BreakdownAbility ability2;

    public AcidRelic(AscendRelics plugin) {
        super(RelicType.ACID);
        this.plugin = plugin;
        this.ability1 = new AcidRainAbility();
        this.ability2 = new BreakdownAbility();
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

    private class AcidRainAbility extends ActiveAbility {
        private static final double RADIUS = 6.0;
        private static final int DURATION_TICKS = 160; // 8 seconds
        private static final int SATURATION_DRAIN = 4;  // drain per tick (every second)

        AcidRainAbility() {
            super("Acid Rain", "Rain acid around you, draining saturation and damaging enemies for 8s", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 1.0f, 0.5f);

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

                    // Rain particles falling from above the caster
                    for (int i = 0; i < 20; i++) {
                        double ox = (Math.random() - 0.5) * RADIUS * 2;
                        double oz = (Math.random() - 0.5) * RADIUS * 2;
                        center.getWorld().spawnParticle(Particle.FALLING_WATER,
                                center.clone().add(ox, 8, oz), 3, 0, 0, 0, 0);
                    }
                    ParticleUtil.cloud(center, Particle.ITEM_SLIME, RADIUS, 25);

                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(center, RADIUS, player)) {
                        trueDamage(entity, 1.5, player);

                        // Drain saturation heavily
                        if (entity instanceof Player target) {
                            float newSat = Math.max(0, target.getSaturation() - SATURATION_DRAIN);
                            target.setSaturation(newSat);
                            // Also drain food level if saturation is depleted
                            if (newSat <= 0) {
                                int newFood = Math.max(0, target.getFoodLevel() - 2);
                                target.setFoodLevel(newFood);
                            }
                        }
                    }
                }
            }.runTaskTimer(AcidRelic.this.plugin, 0L, 20L);

            return AbilityResult.SUCCESS;
        }
    }

    private class BreakdownAbility extends ActiveAbility {
        private static final int POISON_DURATION_TICKS = 160; // 8 seconds
        private static final int DURABILITY_DRAIN_PER_TICK = 3;

        BreakdownAbility() {
            super("Breakdown", "Shoot acid ball that poisons and drains armor durability", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LLAMA_SPIT, 1.0f, 0.5f);

            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection().normalize();

            // Acid ball projectile
            new BukkitRunnable() {
                Location pos = eye.clone();
                int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40) { // max 2 seconds of travel
                        cancel();
                        return;
                    }

                    // Move projectile forward
                    pos.add(dir.clone().multiply(1.5));

                    // Acid ball particles
                    pos.getWorld().spawnParticle(Particle.ITEM_SLIME, pos, 5, 0.2, 0.2, 0.2, 0);
                    pos.getWorld().spawnParticle(Particle.FALLING_WATER, pos, 3, 0.1, 0.1, 0.1, 0);

                    // Stop if hit a block
                    if (pos.getBlock().getType().isSolid()) {
                        cancel();
                        return;
                    }

                    // Check for entity hit
                    RayTraceResult result = pos.getWorld().rayTraceEntities(
                            pos, dir, 1.5, 0.8,
                            e -> e instanceof LivingEntity && e != player
                    );
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        onHit(target, player);
                        cancel();
                    }
                }
            }.runTaskTimer(AcidRelic.this.plugin, 0L, 1L);

            return AbilityResult.SUCCESS;
        }

        private void onHit(LivingEntity target, Player caster) {
            // Impact effects
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);
            target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);

            // Apply poison
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON, POISON_DURATION_TICKS, 1, false, true, true));

            // True damage on impact
            trueDamage(target, 3.0, caster);

            // Drain armor durability every second while poisoned
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > POISON_DURATION_TICKS / 20) {
                        cancel();
                        return;
                    }
                    // Stop if target is dead or no longer poisoned
                    if (target.isDead() || !target.hasPotionEffect(PotionEffectType.POISON)) {
                        cancel();
                        return;
                    }

                    // Drain all armor durability
                    if (target.getEquipment() != null) {
                        for (ItemStack armor : target.getEquipment().getArmorContents()) {
                            if (armor != null && armor.getType().getMaxDurability() > 0) {
                                ItemMeta meta = armor.getItemMeta();
                                if (meta instanceof Damageable damageable) {
                                    damageable.setDamage(damageable.getDamage() + DURABILITY_DRAIN_PER_TICK);
                                    armor.setItemMeta(meta);
                                }
                            }
                        }
                    }

                    // Dripping acid particles on the target
                    target.getWorld().spawnParticle(Particle.ITEM_SLIME,
                            target.getLocation().add(0, 1.5, 0), 5, 0.3, 0.5, 0.3, 0.02);
                }
            }.runTaskTimer(AcidRelic.this.plugin, 20L, 20L);
        }
    }
}
