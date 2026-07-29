package com.legacy.salxaet.manager;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Ihlalleri bir Discord webhook'una gonderir. Ana thread'i bloklamamak icin
 * her istek async (BukkitScheduler) uzerinden atilir.
 */
public class DiscordWebhook {

    private final LAnticheat plugin;
    private final HttpClient client;

    public DiscordWebhook(LAnticheat plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendViolation(Player player, String checkName, String details, int violations) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;

        String url = plugin.getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || !url.startsWith("https://discord.com/api/webhooks/")) {
            plugin.getLogger().warning("Discord webhook URL gecersiz/bos - config.yml -> discord.webhook-url'i kontrol et.");
            return;
        }

        int minViolations = plugin.getConfig().getInt("discord.min-violations-to-report", 3);
        if (violations < minViolations) return;

        String color = String.valueOf(plugin.getConfig().getInt("discord.embed-color", 15158332));
        String json = buildEmbedJson(player, checkName, details, violations, color);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 300) {
                    plugin.getLogger().warning("Discord webhook basarisiz, HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Discord webhook gonderilemedi: " + e.getMessage());
            }
        });
    }

    private String buildEmbedJson(Player player, String checkName, String details, int violations, String color) {
        String server = plugin.getConfig().getString("discord.server-name", "Sunucu");
        return "{"
                + "\"embeds\":[{"
                + "\"title\":\"SalxAET Uyari\","
                + "\"color\":" + color + ","
                + "\"fields\":["
                + field("Oyuncu", escape(player.getName())) + ","
                + field("Kontrol", escape(checkName)) + ","
                + field("Detay", escape(details == null ? "-" : details)) + ","
                + field("Ihlal Sayisi", String.valueOf(violations))
                + "],"
                + "\"footer\":{\"text\":\"" + escape(server) + "\"}"
                + "}]"
                + "}";
    }

    private String field(String name, String value) {
        return "{\"name\":\"" + escape(name) + "\",\"value\":\"" + value + "\",\"inline\":true}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
