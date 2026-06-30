package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.entity.Player;

/**
 * KillAura B: Saldırı anındaki yaw ile birkaç tik önceki yaw arasındaki
 * farkı kontrol eder. Aura kullanan botlar genelde saldırı anında
 * aniden hedefe kilitlenip rotasyonu "snap" eder (insan gibi yumuşak dönmez).
 */
public class KillAuraB {

    public static boolean check(LAnticheat plugin, Player player, PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraB.aktif", true)) return false;

        double minDist  = plugin.getConfig().getDouble("kontroller.KillAuraB.min-mesafe", 0.0);
        double snapAngle = plugin.getConfig().getDouble("kontroller.KillAuraB.snap-acisi", 65.0);

        float currentYaw = player.getLocation().getYaw();
        float currentPitch = player.getLocation().getPitch();

        // 3 tik önceki rotasyonu al
        float pastYaw = dataManager.getYawTicksAgo(player.getUniqueId(), 3);

        float yawDelta = MathUtils.yawDelta(currentYaw, pastYaw);

        if (yawDelta > snapAngle) {
            plugin.triggerAlert(player, "KillAura-B",
                    String.format("rotasyon-sicrama=%.1f° (3 tik icinde)", yawDelta));
            return true;
        }
        return false;
    }
}
