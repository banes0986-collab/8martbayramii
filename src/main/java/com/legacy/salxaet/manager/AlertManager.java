package com.legacy.salxaet.manager;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class AlertManager {

    private final LAnticheat plugin;

    public AlertManager(LAnticheat plugin) {
        this.plugin = plugin;
    }

    public void raise(Player player, String checkName, String details, int violationCount) {
        FileConfiguration cfg = plugin.getConfig();
        boolean sendAlerts = cfg.getBoolean("settings.send-alerts", true);
        boolean logConsole = cfg.getBoolean("settings.log-to-console", true);
        boolean debug = cfg.getBoolean("settings.debug", false);

        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("prefix", "&8[&dSalxAET&8] "));

        String format = plugin.getMessagesConfig().getString(
                "alert-format",
                "&f%player% &7-> &e%check% &8| &7%details% &8(&c%violations%&8)");

        String formatted = ChatColor.translateAlternateColorCodes('&', format
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%details%", details == null ? "" : details)
                .replace("%violations%", String.valueOf(violationCount)));

        if (logConsole) {
            plugin.getLogger().log(Level.INFO, ChatColor.stripColor(prefix + formatted));
        }

        if (debug) {
            plugin.getLogger().info("[DEBUG] check=" + checkName + " player=" + player.getName()
                    + " details=" + details + " violations=" + violationCount);
        }

        if (sendAlerts) {
            String finalMsg = prefix + formatted;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("salxaet.admin")) {
                    online.sendMessage(finalMsg);
                }
            }
        }
    }

    public void info(String message) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("prefix", "&8[&dSalxAET&8] "));
        plugin.getLogger().info(ChatColor.stripColor(prefix + message));
    }
                                                               }
