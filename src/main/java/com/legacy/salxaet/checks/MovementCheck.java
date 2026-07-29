package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MovementCheck {

    private final LAnticheat plugin;

    // PlayerData sınıfındaki eksik metotlara bağımlı kalmamak için lokal veriler
    private final Map<UUID, Long> lastMoveTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> speedViolations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> noSlowViolations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> flyViolations = new ConcurrentHashMap<>();

    public MovementCheck(LAnticheat plugin) {
        this.plugin = plugin;
    }

    /**
     * MovementListener tarafından çağrılan ana hareket kontrol metodu.
     */
    public void onMove(Player player, Location from, Location to, PlayerData data) {
        // Yaratıcı, İzleyici modunda, uçuşta veya binek üzerinde olan oyuncuları atla
        if (player.getAllowFlight() || player.isFlying() || player.isInsideVehicle()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Yatay (XZ) ve Dikey (Y) mesafe değişimleri
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double deltaXZ = Math.hypot(deltaX, deltaZ);
        double deltaY = to.getY() - from.getY();

        // Zaman farkı hesabı (Blok / Saniye - BPS)
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMoveTimes.getOrDefault(uuid, 0L);
        lastMoveTimes.put(uuid, currentTime);

        if (lastTime == 0) {
            return;
        }

        long timeDiff = currentTime - lastTime;
        
        // Çok kısa paketlerde veya lag/ping sıçramalarında hatalı tespiti engelle
        if (timeDiff <= 10 || timeDiff > 1000) {
            return;
        }

        // BPS Hesaplama
        double blocksPerSecond = (deltaXZ / (timeDiff / 1000.0));
        var config = plugin.getConfig();

        // =========================================================================
        // 1. SPEED CHECK (Yatay Hareket Kontrolü)
        // =========================================================================
        if (config.getBoolean("checks.speed.enabled", true)) {
            double maxBps = config.getDouble("checks.speed.max-blocks-per-second", 7.5);

            // Speed (Hız) İksiri Katlanması (+%20 Her Seviye İçin)
            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                int amp = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
                maxBps *= (1.0 + (0.20 * amp));
            }

            // Buz Yüzey Kontrolü
            Material blockBelow = from.clone().subtract(0, 0.5, 0).getBlock().getType();
            if (blockBelow.name().contains("ICE")) {
                maxBps *= 1.40;
            }

            if (blocksPerSecond > maxBps) {
                int currentVL = speedViolations.getOrDefault(uuid, 0) + 1;
                speedViolations.put(uuid, currentVL);
                int maxViolations = config.getInt("checks.speed.max-violations", 8);

                if (config.getBoolean("settings.debug", false)) {
                    plugin.getLogger().info(String.format("[DEBUG Speed] %s: BPS=%.2f, Max=%.2f, VL=%d",
                            player.getName(), blocksPerSecond, maxBps, currentVL));
                }

                plugin.getAlertManager().sendAlert(player, "Speed", currentVL, maxViolations);

                if (config.getBoolean("checks.speed.rubberband", true)) {
                    player.teleport(from);
                }
            } else {
                int currentVL = speedViolations.getOrDefault(uuid, 0);
                if (currentVL > 0) {
                    speedViolations.put(uuid, currentVL - 1);
                }
            }
        }

        // =========================================================================
        // 2. NOSLOW CHECK (Kalkan/Yay Tutarken Hızlı Yürüme Kontrolü)
        // =========================================================================
        if (config.getBoolean("checks.noslow.enabled", true) && player.isHandRaised()) {
            double maxNoSlowBps = config.getDouble("checks.noslow.max-blocks-per-second-while-blocking", 3.8);

            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                int amp = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
                maxNoSlowBps *= (1.0 + (0.20 * amp));
            }

            if (blocksPerSecond > maxNoSlowBps) {
                int currentVL = noSlowViolations.getOrDefault(uuid, 0) + 1;
                noSlowViolations.put(uuid, currentVL);
                int maxViolations = config.getInt("checks.noslow.max-violations", 5);

                plugin.getAlertManager().sendAlert(player, "NoSlow", currentVL, maxViolations);

                if (config.getBoolean("checks.noslow.rubberband", true)) {
                    player.teleport(from);
                }
            } else {
                int currentVL = noSlowViolations.getOrDefault(uuid, 0);
                if (currentVL > 0) {
                    noSlowViolations.put(uuid, currentVL - 1);
                }
            }
        }

        // =========================================================================
        // 3. FLY CHECK (Dikey Y-Aksı Kontrolü)
        // =========================================================================
        if (config.getBoolean("checks.fly.enabled", true)) {
            boolean nearGround = isNearGround(to) || isNearGround(from);

            if (!nearGround) {
                double maxAscent = 0.55;

                // Sürüm uyumluluğu için Jump Boost kontrolü
                PotionEffectType jumpType = PotionEffectType.getByName("JUMP_BOOST");
                if (jumpType == null) {
                    jumpType = PotionEffectType.getByName("JUMP");
                }

                if (jumpType != null && player.hasPotionEffect(jumpType)) {
                    int amp = player.getPotionEffect(jumpType).getAmplifier() + 1;
                    maxAscent += (amp * 0.15);
                }

                if (deltaY > maxAscent) {
                    int currentVL = flyViolations.getOrDefault(uuid, 0) + 1;
                    flyViolations.put(uuid, currentVL);
                    int maxViolations = config.getInt("checks.fly.max-violations", 5);

                    if (config.getBoolean("settings.debug", false)) {
                        plugin.getLogger().info(String.format("[DEBUG Fly] %s: DeltaY=%.2f, Max=%.2f, VL=%d",
                                player.getName(), deltaY, maxAscent, currentVL));
                    }

                    plugin.getAlertManager().sendAlert(player, "Fly", currentVL, maxViolations);

                    if (config.getBoolean("checks.fly.rubberband", true)) {
                        player.teleport(from);
                    }
                }
            } else {
                int currentVL = flyViolations.getOrDefault(uuid, 0);
                if (currentVL > 0) {
                    flyViolations.put(uuid, currentVL - 1);
                }
            }
        }
    }

    /**
     * Oyuncunun etrafındaki blokları kontrol ederek zemin toleransı sağlar.
     */
    private boolean isNearGround(Location loc) {
        if (loc.getBlock().getType().isSolid()) {
            return true;
        }

        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Location checkLoc = loc.clone().add(x, -0.5, z);
                if (!checkLoc.getBlock().getType().isAir() && checkLoc.getBlock().getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
}
