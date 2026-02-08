package com.joshuacolvin.ascendrelics.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class ParticleUtil {

    private ParticleUtil() {}

    public static void line(Location from, Location to, Particle particle, int density) {
        World world = from.getWorld();
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        double step = distance / density;

        for (int i = 0; i <= density; i++) {
            Location point = from.clone().add(direction.clone().multiply(step * i));
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    public static void sphere(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        double increment = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double radiusAtY = Math.sqrt(1 - y * y);
            double theta = increment * i;
            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            Location point = center.clone().add(x * radius, y * radius, z * radius);
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    public static void cloud(Location center, Particle particle, double radius, int count) {
        World world = center.getWorld();
        for (int i = 0; i < count; i++) {
            double x = (Math.random() - 0.5) * 2 * radius;
            double y = (Math.random() - 0.5) * radius;
            double z = (Math.random() - 0.5) * 2 * radius;
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    public static void ring(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(particle, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }
}
