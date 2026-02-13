package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class GhostRelic extends Relic {

    private final AscendRelics plugin;
    private final GhostPassive passive = new GhostPassive();
    private final PhaseStepAbility ability1 = new PhaseStepAbility();
    private final VanishAbility ability2;

    public GhostRelic(AscendRelics plugin) {
        super(RelicType.GHOST);
        this.plugin = plugin;
        this.ability2 = new VanishAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class GhostPassive implements PassiveAbility {
        @Override public String name() { return "Spectral Form"; }
        @Override public String description() { return "Permanent invisibility"; }

        @Override
        public void apply(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, 40, 0, true, false, true));
        }

        @Override
        public void remove(Player player) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        @Override
        public void tick(Player player) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, 40, 0, true, false, true));
        }
    }

    private static class PhaseStepAbility extends ActiveAbility {
        private static final int MAX_DISTANCE = 15;

        PhaseStepAbility() {
            super("Phase Step", "Teleport forward, phasing through walls", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection().normalize();
            // Horizontal-only direction for open-air movement
            Vector horizontalDir = direction.clone().setY(0).normalize();

            boolean inSolid = false;
            Location lastSafeAir = null;
            Location phaseDestination = null;

            for (int i = 1; i <= MAX_DISTANCE; i++) {
                // Use full direction when phasing through solid, horizontal-only in open air
                Vector stepDir = inSolid ? direction : horizontalDir;
                Location check = (lastSafeAir != null ? lastSafeAir.clone() : eyeLoc.clone())
                        .add(stepDir.clone().multiply(inSolid ? i : 1));

                // Recalculate from origin for consistency
                if (!inSolid) {
                    check = eyeLoc.clone().add(horizontalDir.clone().multiply(i));
                } else {
                    check = eyeLoc.clone().add(direction.clone().multiply(i));
                }

                Block feetBlock = check.getBlock();
                Block headBlock = check.clone().add(0, 1, 0).getBlock();
                boolean feetClear = !feetBlock.getType().isSolid();
                boolean headClear = !headBlock.getType().isSolid();

                if (feetClear && headClear) {
                    if (inSolid) {
                        // Found air on the other side of a wall - phase through
                        phaseDestination = check.clone();
                        phaseDestination.setYaw(player.getLocation().getYaw());
                        phaseDestination.setPitch(player.getLocation().getPitch());
                        break;
                    }
                    lastSafeAir = check.clone();
                    lastSafeAir.setYaw(player.getLocation().getYaw());
                    lastSafeAir.setPitch(player.getLocation().getPitch());
                } else {
                    inSolid = true;
                }
            }

            Location destination = phaseDestination != null ? phaseDestination : lastSafeAir;
            if (destination == null) return AbilityResult.FAILED;

            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);

            player.teleport(destination);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
            return AbilityResult.SUCCESS;
        }
    }

    private class VanishAbility extends ActiveAbility {
        VanishAbility() {
            super("Vanish", "Hide your armor from other players for 4s", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            // Send empty equipment to all viewers
            Map<EquipmentSlot, ItemStack> emptyEquipment = new HashMap<>();
            emptyEquipment.put(EquipmentSlot.HEAD, ItemStack.empty());
            emptyEquipment.put(EquipmentSlot.CHEST, ItemStack.empty());
            emptyEquipment.put(EquipmentSlot.LEGS, ItemStack.empty());
            emptyEquipment.put(EquipmentSlot.FEET, ItemStack.empty());
            emptyEquipment.put(EquipmentSlot.HAND, ItemStack.empty());
            emptyEquipment.put(EquipmentSlot.OFF_HAND, ItemStack.empty());

            for (Player viewer : player.getWorld().getPlayers()) {
                if (!viewer.equals(player)) {
                    viewer.sendEquipmentChange(player, emptyEquipment);
                }
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, true, false, true));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 1, 0.5, 0.05);

            // Restore armor visibility after 4 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    // Explicitly send real equipment back to all online players
                    PlayerInventory inv = player.getInventory();
                    Map<EquipmentSlot, ItemStack> realEquipment = new HashMap<>();
                    realEquipment.put(EquipmentSlot.HEAD, getOrEmpty(inv.getHelmet()));
                    realEquipment.put(EquipmentSlot.CHEST, getOrEmpty(inv.getChestplate()));
                    realEquipment.put(EquipmentSlot.LEGS, getOrEmpty(inv.getLeggings()));
                    realEquipment.put(EquipmentSlot.FEET, getOrEmpty(inv.getBoots()));
                    realEquipment.put(EquipmentSlot.HAND, getOrEmpty(inv.getItemInMainHand()));
                    realEquipment.put(EquipmentSlot.OFF_HAND, getOrEmpty(inv.getItemInOffHand()));

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.equals(player)) {
                            viewer.sendEquipmentChange(player, realEquipment);
                        }
                    }

                    player.updateInventory();
                    MessageUtil.info(player, "Vanish has worn off.");
                }
            }.runTaskLater(GhostRelic.this.plugin, 80L);

            return AbilityResult.SUCCESS;
        }

        private ItemStack getOrEmpty(ItemStack item) {
            return item != null ? item : ItemStack.empty();
        }
    }
}
