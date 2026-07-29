package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.AlertManager;
import com.legacy.salxaet.manager.PlayerData;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Eski surumde KillAuraA..G diye 7 ayri sinif vardi, hepsi benzer isi
 * birbirinden habersiz yapiyor ve genelde birbirini iptal ediyordu
 * (biri "aktif" derken digeri ayni tick'te sifirliyordu). Burada tek bir
 * check, birden fazla sinyali ayni PlayerData uzerinden degerlendiriyor.
 */
public class KillAuraCheck {

    private final LAnticheat plugin;
    private final AlertManager alerts;

    public KillAuraCheck(LAnticheat plugin) {
        this.plugin = plugin;
        this.alerts = plugin.getAlertManager();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    private boolean enabled() {
        return cfg().getBoolean("checks.killaura.enabled", true);
    }

    /** Her saldiri (EntityDamageByEntityEvent) tetiklendiginde cagrilir. */
    public void onAttack(Player attacker, Entity target, PlayerData data) {
        if (!enabled()) return;

        long now = System.currentTimeMillis();
        checkClickSpeed(attacker, data, now);
        checkRotationConsistency(attacker, data);
        checkMultiTarget(attacker, target, data, now);

        data.lastAttackTime = now;
    }

    /** Her hareket bakisinda (yaw/pitch degisimi) cagrilir, saldiridan bagimsiz orn tarama icin kullanilabilir. */
    public void trackRotation(PlayerData data, float yaw, float pitch) {
        int sampleSize = cfg().getInt("checks.killaura.rotation-sample-size", 20);
        data.yawSamples.addLast(yaw);
        data.pitchSamples.addLast(pitch);
        while (data.yawSamples.size() > sampleSize) data.yawSamples.pollFirst();
        while (data.pitchSamples.size() > sampleSize) data.pitchSamples.pollFirst();
    }

    private void checkClickSpeed(Player player, PlayerData data, long now) {
        int maxCps = cfg().getInt("checks.killaura.max-cps", 15);
        data.clickTimestamps.addLast(now);
        // 1 saniyeden eski tiklamalari at
        while (!data.clickTimestamps.isEmpty() && now - data.clickTimestamps.peekFirst() > 1000) {
            data.clickTimestamps.pollFirst();
        }
        int cps = data.clickTimestamps.size();
        if (cps > maxCps) {
            int v = data.addViolation("AutoClick");
            alerts.raise(player, "AutoClick", "cps=" + cps, v);
            checkKick(player, "AutoClick", v, cfg().getInt("checks.killaura.max-violations", 6));
        } else {
            data.decayViolation("AutoClick");
        }
    }

    private void checkRotationConsistency(Player player, PlayerData data) {
        int minSamples = 6;
        if (data.yawSamples.size() < minSamples) return;

        double stddev = MathUtils.standardDeviation(data.yawSamples);
        double threshold = cfg().getDouble("checks.killaura.rotation-low-stddev-threshold", 0.15);

        // Cok dusuk sapma = robotik/sabit donus paterni (gercek insan mikro-ayarlar yapar)
        if (stddev < threshold) {
            int v = data.addViolation("KillAura-Rotation");
            alerts.raise(player, "KillAura", "rotation stddev=" + String.format("%.3f", stddev), v);
            checkKick(player, "KillAura", v, cfg().getInt("checks.killaura.max-violations", 6));
        } else {
            data.decayViolation("KillAura-Rotation");
        }
    }

    private void checkMultiTarget(Player player, Entity target, PlayerData data, long now) {
        int windowSeconds = cfg().getInt("checks.killaura.multi-target-window-seconds", 2);
        int maxTargets = cfg().getInt("checks.killaura.multi-target-max-targets", 3);

        data.recentTargets.addLast(new long[]{target.getEntityId(), now});
        long windowMs = windowSeconds * 1000L;
        data.recentTargets.removeIf(entry -> now - entry[1] > windowMs);

        long distinctTargets = data.recentTargets.stream().mapToLong(e -> e[0]).distinct().count();
        if (distinctTargets > maxTargets) {
            int v = data.addViolation("MultiTarget");
            alerts.raise(player, "MultiTarget", "hedef=" + distinctTargets + "/" + windowSeconds + "s", v);
            checkKick(player, "MultiTarget", v, cfg().getInt("checks.killaura.max-violations", 6));
        }
    }

    private void checkKick(Player player, String checkName, int violations, int maxViolations) {
        if (!cfg().getBoolean("settings.kick-on-max-violations", true)) return;
        if (violations < maxViolations) return;

        String reason = plugin.getMessagesConfig().getString("kick.reason", "&cHile tespit edildi!")
                .replace("%check%", checkName);
        player.kickPlayer(org.bukkit.ChatColor.translateAlternateColorCodes('&', reason));
        alerts.info("&c" + player.getName() + " KICKED (" + checkName + ", " + violations + " violations)");
    }
}
