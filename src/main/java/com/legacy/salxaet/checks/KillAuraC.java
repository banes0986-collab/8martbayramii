package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.utils.RayUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * KillAura C: Saldırgan ile hedef arasında katı blok olup olmadığını
 * ray trace ile kontrol eder. Duvar arkasından vuran (WallHit/Aura
 * kombinasyonu) oyuncuları yakalar.
 */
public class KillAuraC {

    public static boolean check(LAnticheat plugin, Player player, Entity target) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraC.aktif", true)) return false;

        Location eyeLoc = player.getEyeLocation();
        BoundingBox targetBox = target.getBoundingBox();
        Location targetCenter = target.getLocation().add(0, target.getHeight() / 2.0, 0);

        boolean unblocked = RayUtils.isUnblocked(player.getWorld(), eyeLoc, targetCenter);

        if (!unblocked) {
            plugin.triggerAlert(player, "KillAura-C",
                    "duvar-arkasi-vurus tespit edildi (ray bloke)");
            return true;
        }
        return false;
    }
}
