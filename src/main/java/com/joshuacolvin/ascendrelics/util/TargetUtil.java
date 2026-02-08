package com.joshuacolvin.ascendrelics.util;

import com.joshuacolvin.ascendrelics.manager.TrustManager;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class TargetUtil {

    private TargetUtil() {}

    public static LivingEntity raycastLivingEntity(Player player, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLocation, direction, maxDistance, 0.5,
                entity -> entity instanceof LivingEntity && entity != player
        );
        if (result != null && result.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public static List<Player> getNearbyNonTrusted(Player caster, double radius, TrustManager trustManager) {
        UUID casterId = caster.getUniqueId();
        Collection<Entity> nearby = caster.getNearbyEntities(radius, radius, radius);
        List<Player> targets = new ArrayList<>();
        for (Entity entity : nearby) {
            if (entity instanceof Player target && !target.equals(caster)) {
                if (!trustManager.isTrusted(casterId, target.getUniqueId())) {
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    public static List<LivingEntity> getNearbyLivingEntities(Location center, double radius, Player exclude) {
        List<LivingEntity> entities = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !living.equals(exclude)) {
                entities.add(living);
            }
        }
        return entities;
    }

    public static Location raycastLocation(Player player, double maxDistance) {
        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                0.0,
                entity -> false
        );
        if (result != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }
        return player.getEyeLocation().add(
                player.getEyeLocation().getDirection().multiply(maxDistance)
        );
    }
}
