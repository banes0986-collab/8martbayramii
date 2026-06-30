package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * KillAura G: "Backtrack" mantığı — saldırı paketinin sunucuya ulaştığı
 * andaki gecikmeyi telafi etmek için hedefin yakın geçmişteki konumuna
 * (200ms öncesine kadar) bakar. Bu, ping nedeniyle false-positive'leri
 * azaltırken yine de aşırı reach/aura'yı yakalamaya devam eder.
 */
public class KillAuraG {

    public static boolean check(LAnticheat plugin, Player player, Entity target,
                                  PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraG.aktif", true)) return false;

        double maxAngle = plugin.getConfig().getDouble("kontroller.KillAuraG.maksimum-aci", 100.0);
        double minDist  = plugin.getConfig().getDouble("kontroller.KillAuraG.min-mesafe", 0.3);

        // Hedefin 150ms öncesindeki kutusunu al (backtrack toleransı)
        PlayerDataManager.HistoricalBox pastBox = dataManager.getBoxMsAgo(target.getUniqueId(), 150L);
        BoundingBox boxToCheck = (pastBox != null) ? pastBox.box : target.getBoundingBox();

        Vector eye = player.getEyeLocation().toVector();
        double dist = MathUtils.distanceToBoundingBox(eye, boxToCheck);

        if (dist < minDist) return false;

        // Açı kontrolü (geçmiş kutunun merkezine göre)
        double centerX = (boxToCheck.getMinX() + boxToCheck.getMaxX()) / 2.0;
        double centerZ = (boxToCheck.getMinZ() + boxToCheck.getMaxZ()) / 2.0;

        double dx = centerX - player.getLocation().getX();
        double dz = centerZ - player.getLocation().getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = player.getLocation().getYaw();

        double delta = Math.abs(targetYaw - playerYaw) % 360;
        if (delta > 180) delta = 360 - delta;

        if (delta > maxAngle) {
            plugin.triggerAlert(player, "KillAura-G",
                    String.format("backtrack-aci=%.1f° (gecmis-konum)", delta));
            return true;
        }
        return false;
    }
}
