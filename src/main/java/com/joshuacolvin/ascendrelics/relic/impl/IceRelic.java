package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IceRelic extends Relic {

    private static final Set<Material> FREEZABLE_GROUND = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.DIRT_PATH,
            Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.MUD,
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.CLAY, Material.PODZOL, Material.MYCELIUM
    );

    private static final Set<Material> WATER_LIKE = Set.of(
            Material.WATER
    );

    private final AscendRelics plugin;
    private final IcePassive passive = new IcePassive();
    private final FreezeFrameAbility ability1;
    private final SnowballFightAbility ability2;

    public IceRelic(AscendRelics plugin) {
        super(RelicType.ICE);
        this.plugin = plugin;
        this.ability1 = new FreezeFrameAbility();
        this.ability2 = new SnowballFightAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    /**
     * Applies a full freeze to a target: no movement, no camera, no knockback, no jump, no attack.
     */
    public static void applyFreeze(LivingEntity target, int durationTicks, Plugin plugin) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 126, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 126, false, false, true));

        // Lock camera and prevent jumping by zeroing positive Y velocity each tick
        if (target instanceof Player playerTarget) {
            float lockedYaw = playerTarget.getLocation().getYaw();
            float lockedPitch = playerTarget.getLocation().getPitch();

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > durationTicks || !playerTarget.isOnline()) {
                        cancel();
                        return;
                    }
                    playerTarget.setRotation(lockedYaw, lockedPitch);
                    // Prevent jumping by zeroing positive Y velocity
                    Vector vel = playerTarget.getVelocity();
                    if (vel.getY() > 0) {
                        vel.setY(0);
                        playerTarget.setVelocity(vel);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private static class IcePassive implements PassiveAbility {
        @Override public String name() { return "Frost Touch"; }
        @Override public String description() { return "5% crit chance to fully freeze targets"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private class FreezeFrameAbility extends ActiveAbility {
        FreezeFrameAbility() {
            super("Freeze Frame", "Completely freeze a target for 6s", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            LivingEntity target = TargetUtil.raycastLivingEntity(player, 30.0);
            if (target == null) return AbilityResult.NO_TARGET;

            applyFreeze(target, 120, pluginRef); // 6 seconds = 120 ticks

            player.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                    30, 0.5, 1, 0.5, 0.05);
            return AbilityResult.SUCCESS;
        }
    }

    private class SnowballFightAbility extends ActiveAbility {
        SnowballFightAbility() {
            super("Snowball Fight", "Shoot 3 snowballs that freeze terrain or damage players", 90);
        }

        // Track all modified blocks for terrain reset
        private final List<Map.Entry<Block, Material>> modifiedBlocks = new ArrayList<>();

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);

            List<Map.Entry<Block, Material>> sessionBlocks = new ArrayList<>();

            // Fire 3 snowballs 20 ticks apart
            for (int shot = 0; shot < 3; shot++) {
                final int shotIndex = shot;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        Location currentEye = player.getEyeLocation();
                        Vector dir = currentEye.getDirection().normalize();
                        launchSnowball(player, currentEye.clone(), dir, pluginRef, sessionBlocks);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 1.0f + shotIndex * 0.2f);
                    }
                }.runTaskLater(pluginRef, shot * 20L);
            }

            // Restore terrain after 15 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Map.Entry<Block, Material> entry : sessionBlocks) {
                        Block block = entry.getKey();
                        Material original = entry.getValue();
                        // Only restore if still frozen
                        if (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.SNOW
                                || block.getType() == Material.ICE) {
                            block.setType(original);
                        }
                    }
                }
            }.runTaskLater(pluginRef, 300L);

            return AbilityResult.SUCCESS;
        }

        private void launchSnowball(Player shooter, Location start, Vector direction, Plugin pluginRef,
                                     List<Map.Entry<Block, Material>> sessionBlocks) {
            new BukkitRunnable() {
                Location current = start.clone();
                int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 60) {
                        cancel();
                        return;
                    }

                    Vector step = direction.clone().multiply(1.5);
                    current.add(step);

                    current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 8, 0.2, 0.2, 0.2, 0.01);
                    current.getWorld().spawnParticle(Particle.BLOCK, current, 3, 0.15, 0.15, 0.15, 0,
                            Material.SNOW_BLOCK.createBlockData());

                    // Check for entity hit
                    for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(current, 1.2, shooter)) {
                        if (entity instanceof Player) {
                            trueDamage(entity, 4.0, shooter);
                        } else {
                            trueDamage(entity, 3.0, shooter);
                        }
                        entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                                entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
                        cancel();
                        return;
                    }

                    // Check for block hit
                    Block block = current.getBlock();
                    if (block.getType().isSolid()) {
                        List<LivingEntity> nearby = TargetUtil.getNearbyLivingEntities(current, 3.0, shooter);
                        for (LivingEntity entity : nearby) {
                            trueDamage(entity, 2.0, shooter);
                        }

                        freezeTerrain(current, 4, sessionBlocks);

                        current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 30, 1.5, 0.5, 1.5, 0.1);
                        current.getWorld().playSound(current, Sound.BLOCK_SNOW_BREAK, 1.0f, 0.5f);
                        cancel();
                    }
                }
            }.runTaskTimer(pluginRef, 0L, 1L);
        }

        private void freezeTerrain(Location center, int radius, List<Map.Entry<Block, Material>> sessionBlocks) {
            int cx = center.getBlockX();
            int cy = center.getBlockY();
            int cz = center.getBlockZ();

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius) continue;

                    for (int y = 2; y >= -2; y--) {
                        Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                        Block above = block.getRelative(BlockFace.UP);

                        if (FREEZABLE_GROUND.contains(block.getType()) && !above.getType().isSolid()) {
                            Material original = block.getType();
                            block.setType(Material.SNOW_BLOCK);
                            sessionBlocks.add(Map.entry(block, original));
                            if (above.getType() == Material.AIR) {
                                sessionBlocks.add(Map.entry(above, Material.AIR));
                                above.setType(Material.SNOW);
                            }
                            break;
                        }

                        if (WATER_LIKE.contains(block.getType())) {
                            Material original = block.getType();
                            block.setType(Material.ICE);
                            sessionBlocks.add(Map.entry(block, original));
                            break;
                        }
                    }
                }
            }
        }
    }
}
