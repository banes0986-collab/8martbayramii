package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RotationCheck implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled()) return;

        Player player = event.getPlayer();
        float pitch = event.getTo().getPitch();

        if (pitch > 90.0F || pitch < -90.0F) {
            event.setCancelled(true);
            plugin.triggerAlert(player, "Rotation (Impossible)", "Gecersiz Baki Acisi: " + pitch);
        }
    }
}
