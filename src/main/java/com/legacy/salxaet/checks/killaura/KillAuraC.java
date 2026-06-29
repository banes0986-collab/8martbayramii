package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class KillAuraC implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled() || !(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Vector toVictim = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        Vector playerLook = player.getLocation().getDirection().normalize();
        double angle = Math.toDegrees(playerLook.angle(toVictim));
        
        double maxAngle = plugin.getConfig().getDouble("checks.hitbox.max-angle", 35.0);
        if (angle > maxAngle) {
            event.setCancelled(true);
            plugin.triggerAlert(player, "KillAura (C/Hitbox)", "Aci Sapmasi: " + String.format("%.1f", angle) + "°");
        }
    }
}
