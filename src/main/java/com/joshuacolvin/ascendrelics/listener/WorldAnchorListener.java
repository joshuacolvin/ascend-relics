package com.joshuacolvin.ascendrelics.listener;

import com.joshuacolvin.ascendrelics.relic.impl.EarthRelic;
import com.joshuacolvin.ascendrelics.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;

public class WorldAnchorListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        checkAnchor(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        checkAnchor(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    private void checkAnchor(Player player, Location blockLoc, org.bukkit.event.Cancellable event) {
        var anchors = EarthRelic.activeAnchors;
        var iterator = anchors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, EarthRelic.AnchorData> entry = iterator.next();
            EarthRelic.AnchorData data = entry.getValue();

            if (data.isExpired()) {
                iterator.remove();
                continue;
            }

            if (data.isInside(blockLoc) && !data.trusted.contains(player.getUniqueId())) {
                event.setCancelled(true);
                MessageUtil.error(player, "This area is protected by a World Anchor!");
                return;
            }
        }
    }
}
