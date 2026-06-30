package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * KillAura A: Göz konumundan hedefin BoundingBox'ına olan gerçek mesafeyi
 * (köşeden değil, en yakın yüzeyden) ölçer. Reach'ten daha hassas çünkü
 * hedefin merkezine değil, hitbox'ın en yakın noktasına bakar.
 */
public class KillAuraA {

    public static boolean check(LAnticheat plugin, Player player, Entity target) {
        if (!plugin.getConfig().getBoolean("kontroller.KillAuraA.aktif", true)) return false;

        double maxDist = plugin.getConfig().getDouble("kontroller.KillAuraA.maksimum-mesafe", 3.4);
        double pingTolerance = plugin.getConfig().getDouble("kontroller.KillAuraA.ping-toleransi", 0.05);

        BoundingBox box = target.getBoundingBox();
        Vector eye = player.getEyeLocation().toVector();

        double dist = MathUtils.distanceToBoundingBox(eye, box);

        if (dist > maxDist + pingTolerance) {
            plugin.triggerAlert(player, "KillAura-A",
                    String.format("hitbox-mesafe=%.2f > %.2f", dist, maxDist));
            return true;
        }
        return false;
    }
          }
