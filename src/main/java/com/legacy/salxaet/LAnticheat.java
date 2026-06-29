package com.legacy.salxaet;

import com.legacy.salxaet.data.PlayerData;
import com.legacy.salxaet.checks.killaura.*;
import com.legacy.salxaet.checks.movement.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin implements Listener, CommandExecutor {

    private static LAnticheat instance;
    private final HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private boolean isAnticheatEnabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        createMessagesConfig();
        
        // MODÜLER KONTROLLERİ KAYDETME (Ayrı Class'lar Burada Tetiklenir)
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new KillAuraA(), this);
        getServer().getPluginManager().registerEvents(new KillAuraB(), this);
        getServer().getPluginManager().registerEvents(new KillAuraC(), this);
        getServer().getPluginManager().registerEvents(new KillAuraD(), this);
        getServer().getPluginManager().registerEvents(new SpeedCheck(), this);
        getServer().getPluginManager().registerEvents(new FlyCheck(), this);

        getCommand("salxaet").setExecutor(this);

        getLogger().info("========================================");
        getLogger().info("SalxAET v2.0 - Modüler Altyapi Aktif!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        playerDataMap.clear();
    }

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadPluginConfigs() {
        reloadConfig();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        String msg = messagesConfig.getString(path, "");
        if (msg.isEmpty()) {
            if (path.equals("prefix")) return "§8[§dSalxAET§8] ";
            if (path.equals("alert-format")) return "§f%player% §7adli oyuncu §e%check% §7shuphesi firlatti! §8(%details%)";
            if (path.equals("kick.reason")) return "§c§l[SalxAET]\n\n§7Bu sunucudan uzaklastirildiniz!\n§bSebep: §eHile Kullanimi\n\n§7Eğer bunun bir hata olduğunu düşünüyorsanız yetkililere bildirin.";
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void triggerAlert(Player player, String checkName, String details) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        data.incrementTotalLogs();

        String prefix = getMessage("prefix");
        String format = getMessage("alert-format")
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%details%", details);
        
        String finalMessage = prefix + format + " §8[" + data.getTotalLogs() + "/15]";

        if (getConfig().getBoolean("settings.send-alerts", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp() || p.hasPermission("salxaet.admin")) {
                    p.sendMessage(finalMessage);
                }
            }
        }
        
        if (getConfig().getBoolean("settings.log-to-console", true)) {
            Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ") [" + data.getTotalLogs() + "/15]");
        }

        if (data.getTotalLogs() >= 15) {
            data.resetTotalLogs();
            String kickMessage = getMessage("kick.reason");
            Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(kickMessage));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("salxaet.admin")) {
            sender.sendMessage(getMessage("prefix") + "§cYetkiniz yok!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadPluginConfigs();
            sender.sendMessage(getMessage("prefix") + "§aAyarlar basariyla yenilendi!");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Kullanim: /salxaet reload");
        return true;
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

    public boolean isAnticheatEnabled() { return isAnticheatEnabled; }
    public HashMap<UUID, PlayerData> getPlayerDataMap() { return playerDataMap; }
    public static LAnticheat getInstance() { return instance; }
}
