package com.legacy.salxaet;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
        
        // ProtocolLib Entegrasyonu
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            registerCombatCheck();
        } else {
            getLogger().severe("ProtocolLib bulunamadi! Savas korumalari devre disi.");
        }
        
        // Eventleri Kaydet
        getServer().getPluginManager().registerEvents(this, this);

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

    // 1. MODÜL: HAREKET KORUMASI (SPEED & FLY)
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight()) {
            return;
        }

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = to.getY() - from.getY();

        // SPEED KONTROLÜ
        if (horizontalDistance > 0.75 && !player.isFlying()) {
            data.addFlag();
            triggerAlert(player, "Speed (Hiz Hilesi)", "Hiz: " + String.format("%.2f", horizontalDistance));
            if (data.getFlags() > 8) {
                event.setTo(from); // Geri çekme (Rubberband)
            }
        }

        // FLY KONTROLÜ
        if (!player.getLocation().getBlock().getType().isSolid() && !player.getEyeLocation().getBlock().getType().isSolid()) {
            if (deltaY == 0.0 && horizontalDistance > 0.1) {
                data.addFlag();
                triggerAlert(player, "Fly (Ucma Hilesi)", "Havada Suzulme");
                if (data.getFlags() > 5) {
                    event.setTo(from);
                }
            }
        }
    }

    // 2. MODÜL: PAKET TABANLI SAVAŞ KORUMASI (KILLAURA)
    private void registerCombatCheck() {
        protocolManager.addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    
                    if (data == null) return;

                    long now = System.currentTimeMillis();
                    long lastAttack = data.getLastAttackTime();
                    
                    if (lastAttack != 0) {
                        long diff = now - lastAttack;
                        // Saniyede 22 CPS üzeri (İnsan dışı hız) tespiti
                        if (diff < 45) {
                            data.addFlag();
                            Bukkit.getScheduler().runTask(LAnticheat.getInstance(), () -> {
                                triggerAlert(player, "Killaura (Savas Hilesi)", "Milisaniye: " + diff + "ms");
                            });
                        }
                    }
                    data.setLastAttackTime(now);
                }
            }
        );
    }

    private void triggerAlert(Player player, String checkName, String details) {
        String message = "§8[§dSalxAET§8] §f" + player.getName() + " §7adli oyuncu §c" + checkName + " §7shuphesi firlatti! §8(" + details + ")";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("salxaet.admin")) {
                p.sendMessage(message);
            }
        }
        Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ")");
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
