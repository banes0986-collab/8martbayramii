package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * KillAura F: Mesafe ve açıyı birlikte değerlendiren basit ama etkili
 * kontrol. Uzak mesafede + büyük açı farkıyla vuran botları yakalar
 * (genelde "legit" aura ayarlarının zayıf noktası).
 */
public class KillAuraF {

    public static boolean check(LAnticheat plugin, Player player, Entity target) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraF.aktif", true)) return false;

        double maxAngle = plugin.getConfig().getDouble("kontroller.KillAuraF.maksimum-aci", 40.0);
        double maxDist  = plugin.getConfig().getDouble("kontroller.KillAuraF.maksimum-mesafe", 3.0);

        double dist = player.getLocation().distance(target.getLocation());
        if (dist < maxDist) return false; // yakınsa daha toleranslı (açı önemsiz)

        double dx = target.getLocation().getX() - player.getLocation().getX();
        double dz = target.getLocation().getZ() - player.getLocation().getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = player.getLocation().getYaw();

        double delta = Math.abs(targetYaw - playerYaw) % 360;
        if (delta > 180) delta = 360 - delta;

        if (delta > maxAngle) {
            plugin.triggerAlert(player, "KillAura-F",
                    String.format("uzak-mesafe-aci=%.1f° (mesafe=%.1f)", delta, dist));
            return true;
        }
        return false;
    }
}
