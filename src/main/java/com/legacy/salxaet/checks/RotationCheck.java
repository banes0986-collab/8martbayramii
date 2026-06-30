package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * RotationCheck: Yaw değişimlerinin standart sapmasını (SD) ölçer.
 * İnsan rotasyonu doğal olarak değişkendir (bazen büyük, bazen küçük
 * dönüşler). Botlar genelde matematiksel olarak çok tutarlı / düşük
 * varyanslı dönüşler üretir — bu "robotik" hareketin imzasıdır.
 */
public class RotationCheck {

    public static boolean check(LAnticheat plugin, Player player, PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.RotationCheck.aktif", true)) return false;

        int sampleCount = plugin.getConfig().getInt("kontroller.RotationCheck.ornek-sayisi", 20);
        double lowSdThreshold = plugin.getConfig().getDouble("kontroller.RotationCheck.dusuk-sd-esigi", 0.15);

        List<Float> deltas = dataManager.getYawDeltas(player.getUniqueId(), sampleCount);
        if (deltas.size() < sampleCount / 2) return false; // yeterli veri yok

        // Sadece gerçekten hareket varken (delta > 1°) ölç, durağan dönemleri sayma
        deltas.removeIf(d -> d < 1.0f);
        if (deltas.size() < 5) return false;

        double sd = MathUtils.stddev(deltas);

        if (sd < lowSdThreshold) {
            plugin.triggerAlert(player, "RotationCheck",
                    String.format("dusuk-varyans sd=%.3f < %.3f (robotik)", sd, lowSdThreshold));
            return true;
        }
        return false;
    }
}
