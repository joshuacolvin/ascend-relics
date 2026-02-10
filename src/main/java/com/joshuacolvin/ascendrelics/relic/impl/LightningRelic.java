package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LightningRelic extends Relic {

    /** Tracks active Chain Strike buffs: player UUID -> hit count so far. */
    public static final Map<UUID, Integer> chainStrikeActive = new ConcurrentHashMap<>();

    private final AscendRelics plugin;
    private final LightningPassive passive = new LightningPassive();
    private final ChainStrikeAbility ability1;
    private final ThunderStepAbility ability2;

    public LightningRelic(AscendRelics plugin) {
        super(RelicType.LIGHTNING);
        this.plugin = plugin;
        this.ability1 = new ChainStrikeAbility();
        this.ability2 = new ThunderStepAbility();
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
        private static final int DURATION_TICKS = 140; // 7 seconds

        ChainStrikeAbility() {
            super("Chain Strike", "Buff attacks: +0.1x per hit (cap 1.5x) + lightning for 7s", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);

            // Activate chain strike buff
            chainStrikeActive.put(player.getUniqueId(), 0);
            MessageUtil.success(player, "Chain Strike activated!");

            // Remove after duration
            new BukkitRunnable() {
                @Override
                public void run() {
                    chainStrikeActive.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        MessageUtil.info(player, "Chain Strike has worn off.");
                    }
                }
            }.runTaskLater(LightningRelic.this.plugin, DURATION_TICKS);

            return AbilityResult.SUCCESS;
        }
    }

    private class ThunderStepAbility extends ActiveAbility {
        private static final int MAX_DISTANCE = 15;

        ThunderStepAbility() {
            super("Thunder Step", "Teleport where you look with lightning at both ends", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location origin = player.getLocation().clone();
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection().normalize();

            // Scan forward, stop at last safe position (no phasing through blocks)
            Location lastSafe = null;
            for (int i = 1; i <= MAX_DISTANCE; i++) {
                Location check = eyeLoc.clone().add(direction.clone().multiply(i));
                Block feetBlock = check.getBlock();
                Block headBlock = check.clone().add(0, 1, 0).getBlock();

                if (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid()) {
                    lastSafe = check.clone();
                    lastSafe.setYaw(origin.getYaw());
                    lastSafe.setPitch(origin.getPitch());
                } else {
                    // Hit a wall, stop scanning
                    break;
                }
            }

            if (lastSafe == null) return AbilityResult.FAILED;

            // Strike lightning at origin
            origin.getWorld().strikeLightningEffect(origin);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, origin, 30, 1, 1, 1, 0.1);

            player.teleport(lastSafe);

            // Strike lightning at destination
            lastSafe.getWorld().strikeLightningEffect(lastSafe);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, lastSafe, 30, 1, 1, 1, 0.1);

            // Damage nearby non-trusted players at both locations
            AscendRelics ar = AscendRelics.getInstance();
            for (Player target : TargetUtil.getNearbyNonTrusted(player, 3.0, ar.trustManager())) {
                if (target.getLocation().distanceSquared(origin) <= 9
                        || target.getLocation().distanceSquared(lastSafe) <= 9) {
                    trueDamage(target, 6.0, player);
                }
            }

            return AbilityResult.SUCCESS;
        }
    }
}
