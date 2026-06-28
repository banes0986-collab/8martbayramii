package com.legacy.salxaet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class SalxAET extends JavaPlugin implements Listener, CommandExecutor {

    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.GREEN + "====================================");
        getLogger().info(ChatColor.GREEN + "SalxAET Aktif Edildi! Komutlar Yukleniyor...");
        getLogger().info(ChatColor.GREEN + "====================================");
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // Komutu sunucuya kaydet
        if (getCommand("salxaet") != null) {
            getCommand("salxaet").setExecutor(this);
        }
        
        // Dosyaları oluştur ve yükle
        saveDefaultConfig();
        createMessagesConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "SalxAET Kapatildi.");
    }

    // messages.yml dosyasını yükleyen fonksiyon
    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Oyun içinden girilen komutları işleyen kısım
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("salxaet")) {
            
            // Eğer reload argümanı girildiyse (/salxaet reload)
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                
                // Yetki kontrolü
                if (!sender.hasPermission("salxaet.reload")) {
                    String noPerm = messagesConfig.getString("Messages.no-permission", "&cBu komutu kullanmak için yetkiniz yok!");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPerm));
                    return true;
                }
                
                // Ana config.yml dosyasını yenile
                reloadConfig();
                
                // messages.yml dosyasını diskten yeniden oku
                if (messagesFile == null) {
                    messagesFile = new File(getDataFolder(), "messages.yml");
                }
                messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
                
                // Başarılı mesajını messages.yml'den çek ve gönder
                String prefix = messagesConfig.getString("Messages.prefix", "&e[&bSalxAET&e] ");
                String successMsg = messagesConfig.getString("Messages.reload-success", "&aKonfigurasayon ve mesaj dosyalari basariyla yenilendi!");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + successMsg));
                return true;
            }
            
            // Yanlış komut kullanımı durumunda bilgilendirme
            sender.sendMessage(ChatColor.RED + "Kullanim: /salxaet reload");
            return true;
        }
        return false;
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
                        double maxMesafe = getConfig().getDouble("checks.combat.reach.max-distance", 4.5);
                        boolean alertsEnabled = getConfig().getBoolean("Settings.send-alerts-to-operators", true);
                        
                        if (mesafe > maxMesafe) {
                            event.setCancelled(true);
                            
                            if (alertsEnabled) {
                                sendAlert(vuran, "Elytra Target / Reach", mesafe);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendAlert(Player player, String hileTürü, double mesafe) {
        String perm = getConfig().getString("alerts.permission", "salxaet.alerts");
        
        String prefix = messagesConfig.getString("Messages.prefix", "&e[&bSalxAET&e] ");
        String alertFormat = messagesConfig.getString("Messages.alert-format", "&c%player% &7hile suphesi! &8(&e%type% &7- Mesafe: &b%distance%&8)");
        
        String hamMesaj = alertFormat
                .replace("%player%", player.getName())
                .replace("%type%", hileTürü)
                .replace("%distance%", String.format("%.2f", mesafe));
        
        String renkliMesaj = ChatColor.translateAlternateColorCodes('&', prefix + hamMesaj);
        
        if (getConfig().getBoolean("alerts.console", true)) {
            getLogger().warning(player.getName() + " hile suphesi engellendi! Tür: " + hileTürü);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(perm) || onlinePlayer.isOp()) {
                onlinePlayer.sendMessage(renkliMesaj);
            }
        }
    }
}
