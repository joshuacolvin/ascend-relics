package com.joshuacolvin.ascendrelics.relic;

import com.joshuacolvin.ascendrelics.data.RelicKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class RelicItemFactory {

    private RelicItemFactory() {}

    public static ItemStack createRelic(RelicType type) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        Component displayName = Component.text("[" + type.displayName() + " Relic]")
                .color(type.color())
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);

        meta.lore(List.of(
                Component.text("An elemental relic of " + type.displayName())
                        .color(type.color())
                        .decoration(TextDecoration.ITALIC, false)
        ));

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
