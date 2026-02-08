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
import org.bukkit.entity.Snowball;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class IceRelic extends Relic {

    private final AscendRelics plugin;
    private final IcePassive passive = new IcePassive();
    private final FreezeFrameAbility ability1 = new FreezeFrameAbility();
    private final HailstormAbility ability2;

    public IceRelic(AscendRelics plugin) {
        super(RelicType.ICE);
        this.plugin = plugin;
        this.ability2 = new HailstormAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class IcePassive implements PassiveAbility {
        @Override public String name() { return "Frost Touch"; }
        @Override public String description() { return "5% crit chance to briefly freeze targets"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private static class FreezeFrameAbility extends ActiveAbility {
        FreezeFrameAbility() {
            super("Freeze Frame", "Freeze a target player in place for 4s", 15);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            LivingEntity target = TargetUtil.raycastLivingEntity(player, 30.0);
            if (target == null) return AbilityResult.NO_TARGET;

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 126, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 126, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 127, false, true, true));

            player.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                    30, 0.5, 1, 0.5, 0.05);
            return AbilityResult.SUCCESS;
        }
    }

    private class HailstormAbility extends ActiveAbility {
        HailstormAbility() {
            super("Hailstorm", "Rain snowballs in a target area", 20);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location target = TargetUtil.raycastLocation(player, 15.0);
            player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 1.0f, 0.5f);

            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count >= 10) { cancel(); return; }
                    count++;

                    Location spawnLoc = target.clone().add(
                            (Math.random() - 0.5) * 6,
                            10,
                            (Math.random() - 0.5) * 6
                    );
                    Snowball snowball = player.getWorld().spawn(spawnLoc, Snowball.class);
                    snowball.setShooter(player);
                    snowball.setVelocity(new Vector(0, -1.5, 0));
                }
            }.runTaskTimer(IceRelic.this.plugin, 0L, 2L);

            return AbilityResult.SUCCESS;
        }
    }
}
