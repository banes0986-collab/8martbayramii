package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * MultiTarget: Kısa bir zaman penceresi içinde (örn. 2 saniye) kaç farklı
 * varlığa vurulduğunu sayar. KillAura kullanan oyuncular genelde menzil
 * içindeki tüm hedeflere otomatik vurduğu için, normal bir oyuncunun
 * tek bir hedefe odaklanma davranışından sapar.
 */
public class MultiTargetCheck {

    public static boolean check(LAnticheat plugin, Player player, Entity target,
                                  PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.MultiTargetCheck.aktif", true)) return false;

        int windowSec   = plugin.getConfig().getInt("kontroller.MultiTargetCheck.pencere-saniye", 2);
        int maxTargets  = plugin.getConfig().getInt("kontroller.MultiTargetCheck.maksimum-hedef", 3);
        long windowMs   = windowSec * 1000L;

        dataManager.recordTarget(player.getUniqueId(), target.getUniqueId());
        int recentTargets = dataManager.getRecentTargetCount(player.getUniqueId(), windowMs);

        if (recentTargets > maxTargets) {
            plugin.triggerAlert(player, "MultiTarget",
                    String.format("%d-farkli-hedef / %d saniye", recentTargets, windowSec));
            return true;
        }
        return false;
    }
}
