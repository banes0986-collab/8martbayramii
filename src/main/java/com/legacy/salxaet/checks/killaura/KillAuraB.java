package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class KillAuraB implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled() || !(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        double distance = player.getLocation().distance(victim.getLocation());
        double maxReach = plugin.getConfig().getDouble("checks.reach.max-distance", 3.3);
        if (player.isGliding()) maxReach += 1.0; 

        if (distance > maxReach) {
            event.setCancelled(true);
            plugin.triggerAlert(player, "KillAura (B/Reach)", "Mesafe: " + String.format("%.2f", distance) + " blok");
        }
    }
}
