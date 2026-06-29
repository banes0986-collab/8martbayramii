package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin implements Listener, CommandExecutor {

    private static LAnticheat instance;
    private ProtocolManager protocolManager;
    private final HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
    private boolean isAnticheatEnabled = true;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketChecks();
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("salxaet").setExecutor(this);
        getLogger().info("SalxAET v1.2 Aktif edildi.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("salxaet.admin")) return true;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("ac")) {
                isAnticheatEnabled = true;
                sender.sendMessage(ChatColor.GREEN + "SalxAET koruması açıldı.");
            } else if (args[0].equalsIgnoreCase("kapat")) {
                isAnticheatEnabled = false;
                sender.sendMessage(ChatColor.RED + "SalxAET koruması devre dışı bırakıldı.");
            }
        }
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!isAnticheatEnabled) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getAllowFlight()) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        // Elytra Kontrolü
        if (player.isGliding()) {
            if (System.currentTimeMillis() - data.lastFireworkBoost < 3000) return;
        }

        // Hız Kontrolü (Basit)
        Location from = event.getFrom();
        Location to = event.getTo();
        double dist = from.distanceSquared(to);
        if (dist > 2.0 && !player.isGliding()) { // Çok hızlı hareketlerde flag
            triggerAlert(player, "Speed/Movement", "Hizli hareket tespit edildi.");
        }
    }

    private void registerPacketChecks() {
        // Fişek Boostu Algılama
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.FIREWORK_ROCKET) {
                    PlayerData data = playerDataMap.get(event.getPlayer().getUniqueId());
                    if (data != null) data.lastFireworkBoost = System.currentTimeMillis();
                }
            }
        });

        // Rotation ve Killaura Kontrolleri
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                PlayerData data = playerDataMap.get(event.getPlayer().getUniqueId());
                if (data == null) return;

                float yaw = event.getPacket().getFloat().read(0);
                float pitch = event.getPacket().getFloat().read(1);

                if (Math.abs(yaw - data.lastYaw) > 350.0F || Math.abs(pitch - data.lastPitch) > 170.0F) {
                    triggerAlert(event.getPlayer(), "Killaura (Rotation)", "Ani donus.");
                }
                data.lastYaw = yaw;
                data.lastPitch = pitch;
            }
        });
    }

    private void triggerAlert(Player player, String check, String details) {
        String msg = ChatColor.RED + "[SalxAET] " + player.getName() + " -> " + check + ": " + details;
        Bukkit.getConsoleSender().sendMessage(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) p.sendMessage(msg);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { playerDataMap.put(e.getPlayer().getUniqueId(), new PlayerData()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { playerDataMap.remove(e.getPlayer().getUniqueId()); }

    public static class PlayerData {
        public long lastFireworkBoost = 0;
        public float lastYaw = 0.0F, lastPitch = 0.0F;
    }
    }
                    
