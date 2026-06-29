package com.legacy.salxaet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final LAnticheat plugin;

    public PlayerListener(LAnticheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.playerDataMap.put(player.getUniqueId(), new LAnticheat.PlayerData(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerDataMap.remove(event.getPlayer().getUniqueId());
    }
}
