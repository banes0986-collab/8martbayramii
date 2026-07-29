package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class MovementCheck {

    private final LAnticheat plugin;

    public MovementCheck(LAnticheat plugin) {
        this.plugin = plugin;
    }

    public void process(Player player, Location from, Location to, PlayerData data) {
        // Yaratıcı, İzleyici modunda, uçuşta veya binek üzerinde olan oyuncuları atla
        if (player.getAllowFlight() || player.isFlying() || player.isInsideVehicle()) {
            return;
        }

        // Yatay (XZ) ve Dikey (Y) mesafe değişimleri
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double deltaXZ = Math.hypot(deltaX, deltaZ);
        double deltaY = to.getY() - from.getY();

        // Zaman farkı hesabı (Blok / Saniye - BPS)
        long currentTime = System.currentTimeMillis();
        long lastTime = data.getLastMoveTime();
        data.setLastMoveTime(currentTime);

        if (lastTime == 0) {
            return;
        }

        long timeDiff = currentTime - lastTime;
        
        // Çok kısa paketlerde veya lag/ping sıçramalarında hatalı tespiti engelle
        if (timeDiff <= 10 || timeDiff > 1000) {
            return;
        }

        // BPS Hesaplama: $BPS = \frac{\Delta XZ}{\Delta t}$
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

            // Buz Yüzey Kontrolü (Ekstra Tolerans)
            Material blockBelow = from.clone().subtract(0, 0.5, 0).getBlock().getType();
            if (blockBelow.name().contains("ICE")) {
                maxBps *= 1.40;
            }

            // İhlal Algılama
            if (blocksPerSecond > maxBps) {
                data.incrementSpeedViolations();
                int maxViolations = config.getInt("checks.speed.max-violations", 8);

                if (config.getBoolean("settings.debug", false)) {
                    plugin.getLogger().info(String.format("[DEBUG Speed] %s: BPS=%.2f, Max=%.2f, VL=%d",
                            player.getName(), blocksPerSecond, maxBps, data.getSpeedViolations()));
                }

                plugin.getAlertManager().sendAlert(player, "Speed", data.getSpeedViolations(), maxViolations);

                if (config.getBoolean("checks.speed.rubberband", true)) {
                    player.teleport(from);
                }
            } else {
                // Zamanla ihlal puanını düşür (Decay)
                data.decaySpeedViolations();
            }
        }

        // =========================================================================
        // 2. NOSLOW CHECK (Kalkan/Yay Tutarken Hızlı Yürüme Kontrolü)
        // =========================================================================
        if (config.getBoolean("checks.noslow.enabled", true) && player.isHandRaised()) {
            double maxNoSlowBps = config.getDouble("checks.noslow.max-blocks-per-second-while-blocking", 3.8);

            // Speed iksiri bonusunu NoSlow'a da yansıt
            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                int amp = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
                maxNoSlowBps *= (1.0 + (0.20 * amp));
            }

            if (blocksPerSecond > maxNoSlowBps) {
                data.incrementNoSlowViolations();
                int maxViolations = config.getInt("checks.noslow.max-violations", 5);

                plugin.getAlertManager().sendAlert(player, "NoSlow", data.getNoSlowViolations(), maxViolations);

                if (config.getBoolean("checks.noslow.rubberband", true)) {
                    player.teleport(from);
                }
            } else {
                data.decayNoSlowViolations();
            }
        }

        // =========================================================================
        // 3. FLY CHECK (Dikey Y-Aksı ve Havada Kalma Kontrolü)
        // =========================================================================
        if (config.getBoolean("checks.fly.enabled", true)) {
            boolean nearGround = isNearGround(to) || isNearGround(from);

            if (!nearGround) {
                // Zıplama anındaki maksimum yasal dikey ivme ($v_y \approx 0.42$)
                double maxAscent = 0.55;

                // Jump Boost İksiri Kontrolü
                if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                    int amp = player.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1;
                    maxAscent += (amp * 0.15);
                }

                // Eğer oyuncu zeminde değilse ve yukarı doğru normal sınırların üstünde yükseliyorsa
                if (deltaY > maxAscent) {
                    data.incrementFlyViolations();
                    int maxViolations = config.getInt("checks.fly.max-violations", 5);

                    if (config.getBoolean("settings.debug", false)) {
                        plugin.getLogger().info(String.format("[DEBUG Fly] %s: DeltaY=%.2f, Max=%.2f, VL=%d",
                                player.getName(), deltaY, maxAscent, data.getFlyViolations()));
                    }

                    plugin.getAlertManager().sendAlert(player, "Fly", data.getFlyViolations(), maxViolations);

                    if (config.getBoolean("checks.fly.rubberband", true)) {
                        player.teleport(from);
                    }
                }
            } else {
                data.decayFlyViolations();
            }
        }
    }

    /**
     * Oyuncunun etrafındaki blokları kontrol ederek yarım blok (slab), merdiven
     * veya zıplama anlarında zemin tespiti toleransı sağlar.
     */
    private boolean isNearGround(Location loc) {
        if (loc.getBlock().getType().isSolid()) {
            return true;
        }

        // Oyuncunun hit-box sınırları içerisinde alt blok kontrolü (-0.5 Y offset)
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
