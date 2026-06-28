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
        // Plugin açıldığında konsola havalı bir mesaj yazdıralım
        getLogger().info(ChatColor.GREEN + "====================================");
        getLogger().info(ChatColor.GREEN + "SalxAET (Anti-Elytra-Target) Aktif!");
        getLogger().info(ChatColor.GREEN + "LegacyNetwork Ozel Koruma Sistemi.");
        getLogger().info(ChatColor.GREEN + "====================================");
        
        // Eventleri (etkinlikleri) sunucuya kaydet
        getServer().getPluginManager().registerEvents(this, this);
        
        // Eğer ileride config eklemek istersen hazır dursun
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "SalxAET Kapatildi.");
    }

    // Elytra ile uçan oyunculara yapılan anormal vuruşları (KillAura/Target) algılama eventi
    @EventHandler
    public void onElytraTargetDamage(EntityDamageByEntityEvent event) {
        // Eğer hasar alan bir oyuncuysa
        if (event.getEntity() instanceof Player) {
            Player mağdur = (Player) event.getEntity();
            
            // Eğer mağdur olan oyuncu o sırada Elytra ile havada uçuyorsa
            if (mağdur.isGliding()) {
                
                // Vuran kişi de bir oyuncuysa (Hile şüphelisi)
                if (event.getDamager() instanceof Player) {
                    Player vuran = (Player) event.getDamager();
                    
                    // Eğer vuran kişi yetkili (OP) değilse kontrol et
                    if (!vuran.hasPermission("salxaet.bypass")) {
                        
                        // Mesafe kontrolü (Uçan adama çok uzaktan vurulmasını engellemek için)
                        double mesafe = vuran.getLocation().distance(mağdur.getLocation());
                        
                        if (mesafe > 4.5) { // 4.5 bloktan uzaksa muhtemelen hiledir
                            // Hasarı iptal et
                            event.setCancelled(true);
                            
                            // Yetkililere alarm gönder
                            sendAlert(vuran, "Elytra Target / KillAura", mesafe);
                        }
                    }
                }
            }
        }
    }

    // Yetkililere hile bildirim mesajı gönderme fonksiyonu
    private void sendAlert(Player player, String hileTürü, double mesafe) {
        String mesaj = ChatColor.RED + "[SalxAET] " + ChatColor.YELLOW + player.getName() + 
                       ChatColor.WHITE + " hile kullaniyor olabilir! " + 
                       ChatColor.GRAY + "(" + hileTürü + " - Uzaklik: " + String.format("%.2f", mesafe) + " blok)";
        
        // Sunucudaki "salxaet.alerts" yetkisine sahip olan herkese mesajı ulaştır
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("salxaet.alerts") || onlinePlayer.isOp()) {
                onlinePlayer.sendMessage(mesaj);
            }
        }
        
        // Konsola da log düşelim
        getLogger().warning(player.getName() + " hile suphesiyle engellendi! Tür: " + hileTürü);
    }
                          }
