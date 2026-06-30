package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.PlayerDataManager;
import org.bukkit.entity.Player;

/**
 * AutoClick: Saniyedeki tıklama sayısını (CPS) ölçer.
 * İnsan elinin fiziksel sınırının üzerinde sürekli tıklama, autoclicker/macro
 * kullanımının işaretidir.
 */
public class AutoClickCheck {

    public static boolean check(LAnticheat plugin, Player player, PlayerDataManager dataManager) {
        if (!plugin.getConfig().getBoolean("kontroller.AutoClickCheck.aktif", true)) return false;

        int maxCps = plugin.getConfig().getInt("kontroller.AutoClickCheck.maksimum-cps", 16);

        double cps = dataManager.recordHitAndGetCPS(player.getUniqueId());

        if (cps > maxCps) {
            plugin.triggerAlert(player, "AutoClick",
                    String.format("cps=%.0f > %d", cps, maxCps));
            return true;
        }
        return false;
    }
}
