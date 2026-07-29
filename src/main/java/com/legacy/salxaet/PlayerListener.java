package com.legacy.salxaet;

import com.legacy.salxaet.manager.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final LAnticheat plugin;

    public PlayerListener(LAnticheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!plugin.isAnticheatEnabled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.hasPermission("salxaet.bypass")) return;

        PlayerData data = plugin.getDataManager().get(attacker);
        plugin.getKillAuraCheck().onAttack(attacker, event.getEntity(), data);
        plugin.getCombatCheck().onAttack(attacker, event.getEntity(), data);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDataManager().get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDataManager().remove(event.getPlayer());
    }
}
