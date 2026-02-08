package com.joshuacolvin.ascendrelics.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public final class LocationUtil {

    private LocationUtil() {}

    public static boolean isInsideSphere(Location point, Location center, double radius) {
        if (!point.getWorld().equals(center.getWorld())) return false;
        return point.distanceSquared(center) <= radius * radius;
    }

    public static Location getGroundLocation(Location location) {
        Location loc = location.clone();
        while (loc.getY() > loc.getWorld().getMinHeight() && !loc.getBlock().getType().isSolid()) {
            loc.subtract(0, 1, 0);
        }
        return loc.add(0, 1, 0);
    }

    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().subtract(0, 1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }

    public static Vector directionBetween(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }
}
