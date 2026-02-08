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

public class LightningRelic extends Relic {

    private final AscendRelics plugin;
    private final LightningPassive passive = new LightningPassive();
    private final ChainStrikeAbility ability1;
    private final StormDashAbility ability2;

    public LightningRelic(AscendRelics plugin) {
        super(RelicType.LIGHTNING);
        this.plugin = plugin;
        this.ability1 = new ChainStrikeAbility();
        this.ability2 = new StormDashAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class LightningPassive implements PassiveAbility {
        @Override public String name() { return "Storm Touched"; }
        @Override public String description() { return "5% chance to strike lightning on hit"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private class ChainStrikeAbility extends ActiveAbility {
        ChainStrikeAbility() {
            super("Chain Strike", "Dash forward and strike the first entity hit", 8);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Vector dash = player.getLocation().getDirection().multiply(2.0);
            player.setVelocity(dash);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 10 || !player.isOnline()) { cancel(); return; }

                    List<LivingEntity> nearby = TargetUtil.getNearbyLivingEntities(
                            player.getLocation(), 2.0, player);
                    if (!nearby.isEmpty()) {
                        LivingEntity target = nearby.get(0);
                        target.damage(4.0, player);
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                        cancel();
                    }
                }
            }.runTaskTimer(LightningRelic.this.plugin, 1L, 2L);

            return AbilityResult.SUCCESS;
        }
    }

    private class StormDashAbility extends ActiveAbility {
        StormDashAbility() {
            super("Storm Dash", "Teleport 15 blocks with lightning at both ends", 20);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location origin = player.getLocation().clone();
            Vector dir = origin.getDirection().setY(0).normalize();
            Location dest = origin.clone().add(dir.multiply(15));
            dest.setY(origin.getY());
            dest.setYaw(origin.getYaw());
            dest.setPitch(origin.getPitch());

            player.teleport(dest);

            origin.getWorld().strikeLightningEffect(origin);
            dest.getWorld().strikeLightningEffect(dest);

            AscendRelics ar = AscendRelics.getInstance();
            for (Location loc : new Location[]{origin, dest}) {
                for (Player target : TargetUtil.getNearbyNonTrusted(player, 3.0, ar.trustManager())) {
                    if (target.getLocation().distanceSquared(loc) <= 9) {
                        target.damage(6.0, player);
                    }
                }
            }

            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, dest, 30, 1, 1, 1, 0.1);
            return AbilityResult.SUCCESS;
        }
    }
}
