package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.AlertManager;
import com.legacy.salxaet.manager.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class MovementCheck {

    private final LAnticheat plugin;
    private final AlertManager alerts;

    public MovementCheck(LAnticheat plugin) {
        this.plugin = plugin;
        this.alerts = plugin.getAlertManager();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public void onMove(Player player, Location from, Location to, PlayerData data) {
        if (player.hasPermission("salxaet.bypass")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!from.getWorld().equals(to.getWorld())) return;

        long now = System.currentTimeMillis();
        double elapsedSeconds = data.lastMoveTime == 0 ? 0.05 : Math.max(0.05, (now - data.lastMoveTime) / 1000.0);

        checkSpeed(player, from, to, elapsedSeconds, data);
        checkFly(player, from, to, data);
        checkNoWeb(player, data);
        checkAntiWall(player, from, to, data);
        checkElytra(player, from, to, elapsedSeconds, data);
        checkTimer(player, data, now);
        checkNoSlow(player, from, to, elapsedSeconds, data);

        data.lastMoveTime = now;
        data.lastLocation = to;
    }

    /**
     * Wurst/Thunderhack gibi clientlerdeki "Timer" modulu istemciyi 20 TPS'den
     * hizli calistirir, bu da hareket paketlerinin normalden sik gelmesine
     * sebep olur. Ortalama paket araligini izleyip dusukse yakalariz.
     */
    private void checkTimer(Player player, PlayerData data, long now) {
        if (!cfg().getBoolean("checks.timer.enabled", true)) return;
        if (data.lastMoveTime == 0) return;

        long intervalMs = now - data.lastMoveTime;
        // Asiri kucuk/anlamsiz degerleri (paket birikmesi vs.) filtrele
        if (intervalMs <= 0 || intervalMs > 1000) return;

        int sampleSize = cfg().getInt("checks.timer.sample-size", 20);
        data.moveIntervalsMs.addLast(intervalMs);
        while (data.moveIntervalsMs.size() > sampleSize) data.moveIntervalsMs.pollFirst();

        if (data.moveIntervalsMs.size() < sampleSize) return; // henuz yeterli ornek yok

        double avg = data.moveIntervalsMs.stream().mapToLong(Long::longValue).average().orElse(50.0);
        double minAvg = cfg().getDouble("checks.timer.min-avg-interval-ms", 42.0);

        if (avg < minAvg) {
            int v = data.addViolation("Timer");
            alerts.raise(player, "Timer", String.format("ort.paket araligi=%.1fms", avg), v);
        } else {
            data.decayViolation("Timer");
        }
    }

    /**
     * Kalkan/yay gerdirirken (blocking) hiz dusmesi gerekir. Thunderhack ve
     * benzeri clientlerde "NoSlow" toggle'i acikken oyuncu blocking sirasinda
     * normal hizda hareket etmeye devam eder.
     */
    private void checkNoSlow(Player player, Location from, Location to, double elapsedSeconds, PlayerData data) {
        if (!cfg().getBoolean("checks.noslow.enabled", true)) return;
        if (!player.isBlocking()) {
            data.decayViolation("NoSlow");
            return;
        }

        double horizontalDist = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        double blocksPerSecond = horizontalDist / elapsedSeconds;
        double max = cfg().getDouble("checks.noslow.max-blocks-per-second-while-blocking", 3.0);

        if (blocksPerSecond > max) {
            int v = data.addViolation("NoSlow");
            alerts.raise(player, "NoSlow", String.format("blocking iken %.2f blok/sn", blocksPerSecond), v);
        } else {
            data.decayViolation("NoSlow");
        }
    }

    private void checkSpeed(Player player, Location from, Location to, double elapsedSeconds, PlayerData data) {
        if (!cfg().getBoolean("checks.speed.enabled", true)) return;
        if (player.isFlying() || player.isGliding()) return;

        double horizontalDist = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        double blocksPerSecond = horizontalDist / elapsedSeconds;
        double max = cfg().getDouble("checks.speed.max-blocks-per-second", 6.5);

        // Hiz iksiri varsa toleransi artir
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getActivePotionEffects().stream()
                    .filter(e -> e.getType().equals(PotionEffectType.SPEED))
                    .findFirst().map(e -> e.getAmplifier() + 1).orElse(0);
            max += amplifier * 2.0;
        }

        if (blocksPerSecond > max) {
            int v = data.addViolation("Speed");
            alerts.raise(player, "Speed", String.format("%.2f blok/sn", blocksPerSecond), v);
            handleRubberband(player, data, "speed", v, from);
        } else {
            data.decayViolation("Speed");
        }
    }

    private void checkFly(Player player, Location from, Location to, PlayerData data) {
        if (!cfg().getBoolean("checks.fly.enabled", true)) return;
        if (player.isFlying() || player.getAllowFlight() || player.isGliding()) return;
        if (player.isInsideVehicle()) return;

        boolean onGroundBefore = from.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
        double yDelta = to.getY() - from.getY();
        double tolerance = cfg().getDouble("checks.fly.max-fall-tolerance", 1.2);

        // Havada surekli yukselme/ayni seviyede kalma (dusmeme) supheli
        if (!onGroundBefore && yDelta > 0 && yDelta < tolerance && !player.hasPotionEffect(PotionEffectType.JUMP_BOOST)
                && !isNearClimbable(player)) {
            int v = data.addViolation("Fly");
            alerts.raise(player, "Fly", String.format("dY=%.2f", yDelta), v);
            handleRubberband(player, data, "fly", v, from);
        } else {
            data.decayViolation("Fly");
        }
    }

    private boolean isNearClimbable(Player player) {
        Block block = player.getLocation().getBlock();
        String type = block.getType().name();
        return type.contains("LADDER") || type.contains("VINE") || type.contains("WATER")
                || type.contains("SCAFFOLDING");
    }

    private void checkNoWeb(Player player, PlayerData data) {
        if (!cfg().getBoolean("checks.noweb.enabled", true)) return;
        Block feet = player.getLocation().getBlock();
        if (feet.getType().name().contains("COBWEB")) {
            // Web icinde normalden hizli hareket ediyorsa (yani hic yavaslamiyorsa) supheli.
            // Basit yaklasim: web icindeyken hiz check'i zaten yakalar; burada sadece sayaci artiriyoruz
            // ki web-icindeki hareket miktari config'de ayri izlenebilsin.
            data.addViolation("NoWeb-InWeb"); // izleme amacli, alert atmiyoruz — Speed check zaten yakalayacak
        }
    }

    private void checkAntiWall(Player player, Location from, Location to, PlayerData data) {
        if (!cfg().getBoolean("checks.antiwall.enabled", true)) return;
        double step = cfg().getDouble("checks.antiwall.ray-step", 0.1);
        double distance = from.distance(to);
        if (distance < 0.3) return; // kucuk hareketlerde ray-trace gereksiz

        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector()).normalize();
        Location cursor = from.clone();
        int steps = (int) (distance / step);
        for (int i = 0; i < steps; i++) {
            cursor.add(direction.clone().multiply(step));
            if (cursor.getBlock().getType().isSolid() && !cursor.getBlock().isPassable()) {
                int v = data.addViolation("AntiWall");
                alerts.raise(player, "AntiWall", "duvar icinden gecis tespit edildi", v);
                handleRubberband(player, data, "antiwall", v, from);
                return;
            }
        }
        data.decayViolation("AntiWall");
    }

    private void checkElytra(Player player, Location from, Location to, double elapsedSeconds, PlayerData data) {
        if (!cfg().getBoolean("checks.elytra.enabled", true)) return;
        if (!player.isGliding()) return;

        double dist3d = from.distance(to);
        double speed = dist3d / elapsedSeconds;
        double max = cfg().getDouble("checks.elytra.max-speed", 2.2) * 10; // blok/sn'e cevir (kabaca)

        if (speed > max) {
            int v = data.addViolation("Elytra");
            alerts.raise(player, "Elytra", String.format("%.2f blok/sn", speed), v);
        } else {
            data.decayViolation("Elytra");
        }
    }

    private void handleRubberband(Player player, PlayerData data, String checkKey, int violations, Location safeLocation) {
        boolean rubberband = cfg().getBoolean("checks." + checkKey + ".rubberband", false);
        int max = cfg().getInt("checks." + checkKey + ".max-violations", 5);
        if (rubberband && violations >= max) {
            player.teleport(safeLocation);
        }
    }
}
