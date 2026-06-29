package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class KillAuraD implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled()) return;
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (plugin.getConfig().getBoolean("checks.antiwall.enabled", true)) {
            Location eyeLoc = player.getEyeLocation();
            Location targetLoc = victim.getEyeLocation();
            double distance = eyeLoc.distance(targetLoc);
            Vector dir = targetLoc.toVector().subtract(eyeLoc.toVector()).normalize();

            RayTraceResult ray = player.getWorld().rayTraceBlocks(eyeLoc, dir, distance, FluidCollisionMode.NEVER, true);
            if (ray != null && ray.getHitBlock() != null && ray.getHitBlock().getType().isSolid()) {
                event.setCancelled(true);
                plugin.triggerAlert(player, "KillAura (D/Wall)", "Blok: " + ray.getHitBlock().getType().name());
            }
        }
    }
}
