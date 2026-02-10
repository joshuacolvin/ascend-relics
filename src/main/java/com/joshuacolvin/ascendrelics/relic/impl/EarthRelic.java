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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EarthRelic extends Relic {

    public static final ConcurrentHashMap<Location, AnchorData> activeAnchors = new ConcurrentHashMap<>();

    private final AscendRelics plugin;
    private final EarthPassive passive = new EarthPassive();
    private final FaultLineAbility ability1 = new FaultLineAbility();
    private final WorldAnchorAbility ability2;

    public EarthRelic(AscendRelics plugin) {
        super(RelicType.EARTH);
        this.plugin = plugin;
        this.ability2 = new WorldAnchorAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    public static class AnchorData {
        public final Location center;
        public final UUID ownerUUID;
        public final long expiryTime;
        public final Set<UUID> trusted;

        public AnchorData(Location center, UUID owner, long expiry, Set<UUID> trusted) {
            this.center = center;
            this.ownerUUID = owner;
            this.expiryTime = expiry;
            this.trusted = trusted;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }

        public boolean isInside(Location loc) {
            if (!loc.getWorld().equals(center.getWorld())) return false;
            return loc.distanceSquared(center) <= 100; // 10-block radius
        }
    }

    private static class EarthPassive implements PassiveAbility {
        @Override public String name() { return "Stone Skin"; }
        @Override public String description() { return "Resistance I"; }

        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
        }

        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, 40, 0, true, false, true));
        }
    }

    private static class FaultLineAbility extends ActiveAbility {
        FaultLineAbility() {
            super("Fault Line", "Create a line of damaging earth", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            Location start = player.getLocation();
            Vector direction = start.getDirection().setY(0).normalize();

            player.getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);

            for (int i = 1; i <= 15; i++) {
                Location point = start.clone().add(direction.clone().multiply(i));
                point.setY(start.getY());

                player.getWorld().spawnParticle(Particle.BLOCK, point, 5,
                        0.5, 0.2, 0.5, 0, Material.DIRT.createBlockData());

                for (LivingEntity entity : TargetUtil.getNearbyLivingEntities(point, 2.0, player)) {
                    trueDamage(entity, 6.0, player);
                }
            }
            return AbilityResult.SUCCESS;
        }
    }

    private class WorldAnchorAbility extends ActiveAbility {
        private static final int RADIUS = 10;
        private static final double SHELL_THICKNESS = 1.5;

        WorldAnchorAbility() {
            super("World Anchor", "Spawn a hollow dirt dome for 15s", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            Location center = player.getLocation().clone();
            Set<UUID> trustedSet = ConcurrentHashMap.newKeySet();
            trustedSet.addAll(AscendRelics.getInstance().trustManager().getTrusted(player.getUniqueId()));
            trustedSet.add(player.getUniqueId());

            AnchorData data = new AnchorData(center, player.getUniqueId(),
                    System.currentTimeMillis() + 15000, trustedSet);
            activeAnchors.put(center, data);

            // Build the hollow dirt sphere and track placed blocks
            List<Block> placedBlocks = buildSphere(center);

            player.getWorld().playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.8f);
            player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.3f);

            // Remove sphere after 15 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    activeAnchors.remove(center);
                    for (Block block : placedBlocks) {
                        if (block.getType() == Material.DIRT) {
                            block.setType(Material.AIR);
                        }
                    }
                    player.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
                }
            }.runTaskLater(EarthRelic.this.plugin, 300L); // 15 seconds

            return AbilityResult.SUCCESS;
        }

        private List<Block> buildSphere(Location center) {
            List<Block> placed = new ArrayList<>();
            int cx = center.getBlockX();
            int cy = center.getBlockY();
            int cz = center.getBlockZ();
            double innerRadius = RADIUS - SHELL_THICKNESS;

            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -RADIUS; y <= RADIUS; y++) {
                    for (int z = -RADIUS; z <= RADIUS; z++) {
                        double dist = Math.sqrt(x * x + y * y + z * z);
                        // Only place blocks in the shell (between inner and outer radius)
                        if (dist <= RADIUS && dist >= innerRadius) {
                            Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                            // Only replace air/non-solid blocks
                            if (!block.getType().isSolid()) {
                                block.setType(Material.DIRT);
                                placed.add(block);
                            }
                        }
                    }
                }
            }
            return placed;
        }
    }
}
