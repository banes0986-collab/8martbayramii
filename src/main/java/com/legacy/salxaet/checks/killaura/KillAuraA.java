package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class KillAuraA implements Listener {

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled()) return;
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        PlayerData data = plugin.getPlayerDataMap().get(player.getUniqueId());
        if (data == null) return;

        if (plugin.getConfig().getBoolean("checks.killaura.enabled", true)) {
            long now = System.currentTimeMillis();
            long diff = now - data.getLastAttackTime();
            
            if (data.getLastAttackTime() != 0 && diff < plugin.getConfig().getInt("checks.killaura.min-ms-delay", 45)) {
                event.setCancelled(true);
                plugin.triggerAlert(player, "KillAura (A/CPS)", "Gecikme: " + diff + "ms");
                return;
            }
            data.setLastAttackTime(now);
        }
    }
}
