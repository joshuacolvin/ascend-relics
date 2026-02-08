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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
        PhaseStepAbility() {
            super("Phase Step", "Teleport through solid blocks", 8);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection().normalize();
            boolean inSolid = false;
            Location destination = null;

            for (int i = 1; i <= 10; i++) {
                Location check = eyeLoc.clone().add(direction.clone().multiply(i));
                Block block = check.getBlock();
                Block headBlock = check.clone().add(0, 1, 0).getBlock();

                if (block.getType().isSolid()) {
                    inSolid = true;
                } else if (inSolid && !block.getType().isSolid() && !headBlock.getType().isSolid()) {
                    destination = check.clone();
                    destination.setYaw(player.getLocation().getYaw());
                    destination.setPitch(player.getLocation().getPitch());
                    break;
                }
            }

            if (destination == null) return AbilityResult.FAILED;

            player.teleport(destination);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
            return AbilityResult.SUCCESS;
        }
    }

    private class VanishAbility extends ActiveAbility {
        VanishAbility() {
            super("Vanish", "Hide your armor from other players for 4s", 20);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
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

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    player.updateInventory();
                    MessageUtil.info(player, "Vanish has worn off.");
                }
            }.runTaskLater(GhostRelic.this.plugin, 80L);

            return AbilityResult.SUCCESS;
        }
    }
}
