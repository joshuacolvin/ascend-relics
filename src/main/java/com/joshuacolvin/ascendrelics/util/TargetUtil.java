package com.joshuacolvin.ascendrelics.util;

import com.joshuacolvin.ascendrelics.AscendRelics;
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

    /**
     * Deals true damage that bypasses armor and enchantments.
     * Directly reduces the entity's health, ignoring all damage reduction.
     * Lethal damage goes through the death pipeline (totems can proc).
     */
    public static void trueDamage(LivingEntity target, double amount) {
        double absorb = target.getAbsorptionAmount();
        if (absorb > 0) {
            if (absorb >= amount) {
                target.setAbsorptionAmount(absorb - amount);
                return;
            } else {
                amount -= absorb;
                target.setAbsorptionAmount(0);
            }
        }
        double newHealth = target.getHealth() - amount;
        if (newHealth <= 0) {
            // Set health low, then deal small damage through the pipeline so totems can proc
            target.setHealth(1);
            target.damage(50);
        } else {
            target.setHealth(newHealth);
        }
    }

    /**
     * Deals true damage with a damage source player (for death messages and kill credit).
     * Skips damage to trusted players. Lethal damage goes through the death pipeline
     * so totems of undying can activate.
     */
    public static void trueDamage(LivingEntity target, double amount, Player source) {
        // Trust check: never damage trusted players
        if (target instanceof Player targetPlayer) {
            if (AscendRelics.getInstance().trustManager()
                    .isTrusted(source.getUniqueId(), targetPlayer.getUniqueId())) {
                return;
            }
        }

        // Store original absorption
        double absorb = target.getAbsorptionAmount();
        if (absorb > 0) {
            if (absorb >= amount) {
                target.setAbsorptionAmount(absorb - amount);
                return;
            } else {
                amount -= absorb;
                target.setAbsorptionAmount(0);
            }
        }

        double newHealth = target.getHealth() - amount;
        if (newHealth <= 0) {
            // Set health low, then deal small damage through the pipeline so totems can proc
            target.setHealth(1);
            target.damage(50, source);
        } else {
            // Trigger hurt animation, then set health directly for true damage
            target.damage(0.001, source);
            target.setHealth(newHealth);
        }
    }

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
