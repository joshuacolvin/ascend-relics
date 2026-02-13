package com.joshuacolvin.ascendrelics.command;

import com.joshuacolvin.ascendrelics.AscendRelics;
import com.joshuacolvin.ascendrelics.relic.Relic;
import com.joshuacolvin.ascendrelics.relic.RelicItemFactory;
import com.joshuacolvin.ascendrelics.relic.RelicType;
import com.joshuacolvin.ascendrelics.relic.ability.AbilityResult;
import com.joshuacolvin.ascendrelics.relic.ability.ActiveAbility;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Ability2Command implements CommandExecutor {

    private final AscendRelics plugin;

    public Ability2Command(AscendRelics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        RelicType type = RelicItemFactory.identifyRelic(player.getInventory().getItemInMainHand());
        if (type == null) {
            type = RelicItemFactory.identifyRelic(player.getInventory().getItemInOffHand());
        }
        if (type == null) {
            MessageUtil.error(player, "You must be holding a relic to use an ability!");
            return true;
        }

        if (plugin.overrideManager().isOverridden(player.getUniqueId())) {
            long remaining = plugin.overrideManager().getRemainingMillis(player.getUniqueId()) / 1000 + 1;
            MessageUtil.error(player, "Your abilities are overridden for " + remaining + "s!");
            return true;
        }

        if (plugin.lockManager().isLocked(type) && !player.isOp()) {
            MessageUtil.error(player, type.displayName() + " Relic is currently locked!");
            return true;
        }

        Relic relic = plugin.relicRegistry().get(type);
        if (relic == null) {
            MessageUtil.error(player, "Unknown relic type!");
            return true;
        }

        ActiveAbility ability = relic.ability2();
        if (plugin.cooldownManager().isOnCooldown(player.getUniqueId(), ability.name())) {
            long remaining = plugin.cooldownManager().getRemainingMillis(player.getUniqueId(), ability.name());
            MessageUtil.cooldown(player, ability.name(), remaining);
            return true;
        }

        AbilityResult result = ability.execute(player, plugin);
        if (result == AbilityResult.SUCCESS) {
            plugin.cooldownManager().setCooldown(player.getUniqueId(), ability.name(), ability.cooldownMillis());
            plugin.lastAbilityUsed().put(player.getUniqueId(), ability);
        } else if (result == AbilityResult.SUCCESS_NO_COOLDOWN) {
            plugin.lastAbilityUsed().put(player.getUniqueId(), ability);
        } else if (result == AbilityResult.NO_TARGET) {
            MessageUtil.error(player, "No valid target found!");
        } else if (result == AbilityResult.FAILED) {
            MessageUtil.error(player, ability.name() + " failed!");
        }
        return true;
    }
}
