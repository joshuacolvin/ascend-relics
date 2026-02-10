package com.joshuacolvin.ascendrelics.relic;

import com.joshuacolvin.ascendrelics.data.RelicKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RelicItemFactory {

    private RelicItemFactory() {}

    public static ItemStack createRelic(Relic relic) {
        RelicType type = relic.type();
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        Component displayName = Component.text(type.displayName() + " Relic")
                .color(type.color())
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();

        // Passive
        lore.add(Component.empty());
        lore.add(Component.text("Passive: ", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(relic.passive().name(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, false)));
        lore.add(Component.text(" " + relic.passive().description(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Ability 1
        long cd1 = relic.ability1().cooldownMillis() / 1000;
        lore.add(Component.empty());
        lore.add(Component.text("Ability 1: ", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(relic.ability1().name(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text(" (" + formatCooldown(cd1) + ")", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.BOLD, false)));
        lore.add(Component.text(" " + relic.ability1().description(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Ability 2
        long cd2 = relic.ability2().cooldownMillis() / 1000;
        lore.add(Component.empty());
        lore.add(Component.text("Ability 2: ", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(relic.ability2().name(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text(" (" + formatCooldown(cd2) + ")", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.BOLD, false)));
        lore.add(Component.text(" " + relic.ability2().description(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        meta.setCustomModelData(type.customModelData());

        meta.getPersistentDataContainer().set(
                RelicKeys.RELIC_TYPE, PersistentDataType.STRING, type.name()
        );
        meta.getPersistentDataContainer().set(
                RelicKeys.RELIC_UUID, PersistentDataType.STRING, UUID.randomUUID().toString()
        );

        item.setItemMeta(meta);
        return item;
    }

    private static String formatCooldown(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (secs == 0) return mins + ":00";
        return mins + ":" + String.format("%02d", secs);
    }

    public static RelicType identifyRelic(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String typeStr = meta.getPersistentDataContainer().get(
                RelicKeys.RELIC_TYPE, PersistentDataType.STRING
        );
        if (typeStr == null) return null;

        try {
            return RelicType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isRelic(ItemStack item) {
        return identifyRelic(item) != null;
    }
}
