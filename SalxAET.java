package com.legacy.salxaet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SalxAET extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.GREEN + "====================================");
        getLogger().info(ChatColor.GREEN + "SalxAET Aktif Edildi! Config Yukleniyor...");
        getLogger().info(ChatColor.GREEN + "====================================");
        
        // Eventleri kaydet ve configi yükle
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "SalxAET Kapatildi.");
    }

    @EventHandler
    public void onElytraTargetDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player mağdur = (Player) event.getEntity();
            
            if (mağdur.isGliding()) {
                if (event.getDamager() instanceof Player) {
                    Player vuran = (Player) event.getDamager();
                    
                    if (!vuran.hasPermission("salxaet.bypass")) {
                        double mesafe = vuran.getLocation().distance(mağdur.getLocation());
                        
                        // Ayarları direkt az önce attığın config.yml dosyasından çekiyoruz:
                        double maxMesafe = getConfig().getDouble("checks.combat.reach.max-distance", 4.5);
                        boolean alertsEnabled = getConfig().getBoolean("Settings.send-alerts-to-operators", true);
                        
                        if (mesafe > maxMesafe) {
                            event.setCancelled(true);
                            
                            if (alertsEnabled) {
                                sendAlert(vuran, "KillAura / Reach (Elytra)", mesafe);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendAlert(Player player, String hileTürü, double mesafe) {
        // Configdeki yetki ayarını çekiyoruz
        String perm = getConfig().getString("alerts.permission", "salxaet.alerts");
        
        String mesaj = ChatColor.RED + "[SalxAET] " + ChatColor.YELLOW + player.getName() + 
                       ChatColor.WHITE + " hile şüphesi! " + 
                       ChatColor.GRAY + "(" + hileTürü + " - Mesafe: " + String.format("%.2f", mesafe) + ")";
        
        if (getConfig().getBoolean("alerts.console", true)) {
            getLogger().warning(player.getName() + " hile suphesi engellendi! Mesafe: " + mesafe);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(perm) || onlinePlayer.isOp()) {
                onlinePlayer.sendMessage(mesaj);
            }
        }
    }
}
