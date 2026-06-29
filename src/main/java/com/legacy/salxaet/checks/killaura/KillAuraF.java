package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.UUID;

public class KillAuraF implements Listener {

    private final HashMap<UUID, UUID> lastTargets = new HashMap<>();
    private final HashMap<UUID, Long> lastTargetTimes = new HashMap<>();

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled() || !(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        UUID pId = player.getUniqueId();
        UUID vId = victim.getUniqueId();

        if (lastTargets.containsKey(pId) && !lastTargets.get(pId).equals(vId)) {
            long diff = System.currentTimeMillis() - lastTargetTimes.getOrDefault(pId, 0L);
            if (diff < 200) { 
                event.setCancelled(true);
                plugin.triggerAlert(player, "KillAura (F/MultiTarget)", "Anlik Hedef Degisimi: " + diff + "ms");
            }
        }
        
        lastTargets.put(pId, vId);
        lastTargetTimes.put(pId, System.currentTimeMillis());
    }
}
