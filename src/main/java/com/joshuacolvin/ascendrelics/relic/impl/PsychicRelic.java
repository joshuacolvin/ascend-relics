package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PsychicRelic extends Relic {

    private final AscendRelics plugin;
    private final PsychicPassive passive = new PsychicPassive();
    private final MindRewriteAbility ability1;
    private final ThoughtTheftAbility ability2;

    public PsychicRelic(AscendRelics plugin) {
        super(RelicType.PSYCHIC);
        this.plugin = plugin;
        this.ability1 = new MindRewriteAbility();
        this.ability2 = new ThoughtTheftAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class PsychicPassive implements PassiveAbility {
        @Override public String name() { return "Mind's Eye"; }
        @Override public String description() { return "Chance to lock attacker's camera after being hit"; }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void tick(Player player) {}
    }

    private class MindRewriteAbility extends ActiveAbility {
        MindRewriteAbility() {
            super("Mind Rewrite", "Scramble target's hotbar and invert movement for 5s", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            LivingEntity hit = TargetUtil.raycastLivingEntity(player, 15.0);
            if (!(hit instanceof Player target)) return AbilityResult.NO_TARGET;

            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 1.5f);
            target.getWorld().spawnParticle(Particle.WITCH,
                    target.getLocation().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);

            // Save original hotbar (slots 0-8)
            PlayerInventory inv = target.getInventory();
            ItemStack[] originalHotbar = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack item = inv.getItem(i);
                originalHotbar[i] = item != null ? item.clone() : null;
            }

            // Shuffle the hotbar
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < 9; i++) indices.add(i);
            Collections.shuffle(indices);
            for (int i = 0; i < 9; i++) {
                inv.setItem(i, originalHotbar[indices.get(i)]);
            }

            MessageUtil.error(target, "Your mind has been rewritten!");

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 100 || !target.isOnline()) { // 5 seconds
                        // Restore original hotbar
                        if (target.isOnline()) {
                            for (int i = 0; i < 9; i++) {
                                target.getInventory().setItem(i, originalHotbar[i]);
                            }
                            MessageUtil.info(target, "Mind Rewrite has worn off.");
                        }
                        cancel();
                        return;
                    }

                    // Invert movement using getCurrentInput
                    var input = target.getCurrentInput();
                    Location loc = target.getLocation();
                    Vector forward = loc.getDirection().setY(0).normalize();
                    Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                    Vector velocity = new Vector();

                    if (input.isForward()) velocity.add(forward.clone().multiply(-0.15));
                    if (input.isBackward()) velocity.add(forward.clone().multiply(0.15));
                    if (input.isLeft()) velocity.add(right.clone().multiply(0.15));
                    if (input.isRight()) velocity.add(right.clone().multiply(-0.15));

                    velocity.setY(target.getVelocity().getY());
                    target.setVelocity(velocity);
                }
            }.runTaskTimer(PsychicRelic.this.plugin, 1L, 1L);

            return AbilityResult.SUCCESS;
        }
    }

    private static class ThoughtTheftAbility extends ActiveAbility {
        ThoughtTheftAbility() {
            super("Thought Theft", "Copy and use target's last ability", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            LivingEntity hit = TargetUtil.raycastLivingEntity(player, 15.0);
            if (!(hit instanceof Player target)) return AbilityResult.NO_TARGET;

            AscendRelics ar = AscendRelics.getInstance();
            ActiveAbility lastAbility = ar.lastAbilityUsed().get(target.getUniqueId());
            if (lastAbility == null) {
                MessageUtil.error(player, target.getName() + " has no ability to steal!");
                return AbilityResult.NO_TARGET;
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.WITCH,
                    player.getLocation().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);
            MessageUtil.success(player, "Stole " + lastAbility.name() + " from " + target.getName() + "!");

            return lastAbility.execute(player, plugin);
        }
    }
}
