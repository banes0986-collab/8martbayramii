package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * KillAura D: Saldırı anında oyuncunun BAKTIĞI yön ile hedefin gerçek
 * konumu arasındaki açıyı, oyuncunun hareket yönüyle karşılaştırır.
 * Gerçek oyuncular hareket ederken bile hedefe doğru tutarlı bakar;
 * aura botları çoğunlukla yanlış yöne bakarken "anlık" vurur (head-snap).
 */
public class KillAuraD {

    public static boolean check(LAnticheat plugin, Player player, Entity target,
                                  PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraD.aktif", true)) return false;

        double minDist = plugin.getConfig().getDouble("kontroller.KillAuraD.min-mesafe", 0.5);
        double maxAngle = plugin.getConfig().getDouble("kontroller.KillAuraD.maksimum-aci", 90.0);

        double dist = player.getLocation().distance(target.getLocation());
        if (dist < minDist) return false; // çok yakınsa açı hesabı güvenilmez

        // Saldırı öncesi kaydedilmiş yaw ile gerçek bakış açısı arasındaki fark
        float preYaw = dataManager.getPreAttackYaw(player.getUniqueId());
        float currentYaw = player.getLocation().getYaw();

        double dx = target.getLocation().getX() - player.getLocation().getX();
        double dz = target.getLocation().getZ() - player.getLocation().getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));

        double delta = Math.abs(targetYaw - preYaw) % 360;
        if (delta > 180) delta = 360 - delta;

        if (delta > maxAngle) {
            plugin.triggerAlert(player, "KillAura-D",
                    String.format("saldiri-oncesi-aci=%.1f° > %.1f°", delta, maxAngle));
            return true;
        }
        return false;
    }
}
