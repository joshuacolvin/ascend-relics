package com.joshuacolvin.ascendrelics.relic.impl;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.relic.ability.PassiveAbility;
import com.joshuacolvin.ascendrelics.util.TargetUtil;
import static com.joshuacolvin.ascendrelics.util.TargetUtil.trueDamage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class MetalRelic extends Relic {

    private static final NamespacedKey TOUGHNESS_KEY =
            NamespacedKey.fromString("ascendrelics:metal_toughness");

    private final AscendRelics plugin;
    private final MetalPassive passive = new MetalPassive();
    private final IronGraspAbility ability1;
    private final OverrideAbility ability2;

    public MetalRelic(AscendRelics plugin) {
        super(RelicType.METAL);
        this.plugin = plugin;
        this.ability1 = new IronGraspAbility();
        this.ability2 = new OverrideAbility();
    }

    @Override public PassiveAbility passive() { return passive; }
    @Override public ActiveAbility ability1() { return ability1; }
    @Override public ActiveAbility ability2() { return ability2; }

    private static class MetalPassive implements PassiveAbility {
        private static final AttributeModifier TOUGHNESS_MODIFIER = new AttributeModifier(
                TOUGHNESS_KEY, 4.0, AttributeModifier.Operation.ADD_NUMBER
        );

        @Override public String name() { return "Metal Plating"; }
        @Override public String description() { return "+4 Armor Toughness"; }

        @Override
        public void apply(Player player) {
            AttributeInstance attr = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (attr != null && !attr.getModifiers().contains(TOUGHNESS_MODIFIER)) {
                attr.addModifier(TOUGHNESS_MODIFIER);
            }
        }

        @Override
        public void remove(Player player) {
            AttributeInstance attr = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (attr != null) attr.removeModifier(TOUGHNESS_MODIFIER);
        }

        @Override public void tick(Player player) {}
    }

    private class IronGraspAbility extends ActiveAbility {
        IronGraspAbility() {
            super("Iron Grasp", "Dash and pull the first entity hit", 60);
        }

        @Override
        public AbilityResult execute(Player player, Plugin pluginRef) {
            player.setVelocity(player.getLocation().getDirection().multiply(3.0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 10 || !player.isOnline()) { cancel(); return; }

                    List<LivingEntity> nearby = TargetUtil.getNearbyLivingEntities(
                            player.getLocation(), 2.0, player);
                    if (!nearby.isEmpty()) {
                        LivingEntity target = nearby.get(0);
                        trueDamage(target, 4.0, player);
                        Vector pull = player.getLocation().toVector()
                                .subtract(target.getLocation().toVector()).normalize().multiply(1.2);
                        pull.setY(0.3);
                        target.setVelocity(pull);
                        target.getWorld().spawnParticle(Particle.CRIT,
                                target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                        cancel();
                    }
                }
            }.runTaskTimer(MetalRelic.this.plugin, 2L, 2L);

            return AbilityResult.SUCCESS;
        }
    }

    private static class OverrideAbility extends ActiveAbility {
        OverrideAbility() {
            super("Override", "Disable target's abilities for 10s", 90);
        }

        @Override
        public AbilityResult execute(Player player, Plugin plugin) {
            LivingEntity hit = TargetUtil.raycastLivingEntity(player, 15.0);
            if (!(hit instanceof Player target)) return AbilityResult.NO_TARGET;

            AscendRelics.getInstance().overrideManager().applyOverride(target.getUniqueId(), 10000);
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.8f);
            player.getWorld().spawnParticle(Particle.SMOKE,
                    target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            target.sendMessage(Component.text("Your abilities have been overridden for 10s!", NamedTextColor.RED));
            return AbilityResult.SUCCESS;
        }
    }
}
