package com.legacy.salxaet.manager;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Tum check'lerin ihlalleri buradan gecer.
 *
 * Eski surumde her check kendi ic mantigiyla log basmaya calisiyordu; bir
 * kismi config'i hic okumadan sessizce dönüyordu, bu da "log bos geliyor"
 * sikayetinin sebebiydi. Artik tek giris noktasi var: raise(...).
 */
public class AlertManager {

    private final LAnticheat plugin;
    private final DiscordWebhook discordWebhook;

    public AlertManager(LAnticheat plugin) {
        this.plugin = plugin;
        this.discordWebhook = new DiscordWebhook(plugin);
    }

    /**
     * Bir ihlal bildirir. checkName ornek: "KillAura", "Speed" vb.
     * details ornek: "cps=22" ya da "delta=1.8"
     */
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

        // Konsol logu — burasi kosulsuz calisiyordu ama config bayraklari
        // yanlis okunuyordu (getBoolean anahtar yolu hatali idi). Duzeltildi.
        if (logConsole) {
            plugin.getLogger().log(Level.INFO, ChatColor.stripColor(prefix + formatted));
        }

        if (debug) {
            plugin.getLogger().info("[DEBUG] check=" + checkName + " player=" + player.getName()
                    + " details=" + details + " violations=" + violationCount);
        }

        // Oyun ici alert — sadece salxaet.admin yetkisi olanlara.
        if (sendAlerts) {
            String finalMsg = prefix + formatted;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("salxaet.admin")) {
                    online.sendMessage(finalMsg);
                }
            }
        }

        discordWebhook.sendViolation(player, checkName, details, violationCount);
    }

    public void info(String message) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("prefix", "&8[&dSalxAET&8] "));
        plugin.getLogger().info(ChatColor.stripColor(prefix + message));
    }
}
