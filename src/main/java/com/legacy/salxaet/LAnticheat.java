package com.legacy.salxaet.src.main.java.com.legacy.salxaet;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin implements Listener {

    private static LAnticheat instance;
    private ProtocolManager protocolManager;
    private final HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MovementCheck(), this);
        
        CombatCheck.register();

        getLogger().info("========================================");
        getLogger().info("SalxAET v1.0 - Gelismis Paket Altyapisi Aktif!");
        getLogger().info("LegacyNetwork icin ozel olarak optimize edildi.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        playerDataMap.clear();
        getLogger().info("SalxAET Devre Disi Birakildi!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataMap.put(player.getUniqueId(), new PlayerData(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }

    public static LAnticheat getInstance() {
        return instance;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public HashMap<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public static class PlayerData {
        private final Player player;
        private int violationFlags = 0;
        private long lastAttackTime = 0;

        public PlayerData(Player player) {
            this.player = player;
        }

        public Player getPlayer() { return player; }
        public int getFlags() { return violationFlags; }
        public void addFlag() { this.violationFlags++; }
        
        public long getLastAttackTime() { return lastAttackTime; }
        public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    }
}
