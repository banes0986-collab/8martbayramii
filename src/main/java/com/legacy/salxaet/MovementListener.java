package com.legacy.salxaet;

import com.legacy.salxaet.checks.KillAuraCheck;
import com.legacy.salxaet.checks.MovementCheck;
import com.legacy.salxaet.manager.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final LAnticheat plugin;
    private final MovementCheck movementCheck;
    private final KillAuraCheck killAuraCheck;

    public MovementListener(LAnticheat plugin) {
        this.plugin = plugin;
        this.movementCheck = new MovementCheck(plugin);
        this.killAuraCheck = plugin.getKillAuraCheck();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.isAnticheatEnabled()) return;
        if (event.getTo() == null) return;

        PlayerData data = plugin.getDataManager().get(event.getPlayer());

        // Sadece pozisyon degil rotasyon da degistiyse orn topla (KillAura icin)
        if (event.getFrom().getYaw() != event.getTo().getYaw()
                || event.getFrom().getPitch() != event.getTo().getPitch()) {
            killAuraCheck.trackRotation(data, event.getTo().getYaw(), event.getTo().getPitch());
        }

        movementCheck.onMove(event.getPlayer(), event.getFrom(), event.getTo(), data);
    }
}
