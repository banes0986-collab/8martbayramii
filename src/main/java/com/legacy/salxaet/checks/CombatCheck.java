package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import com.legacy.salxaet.manager.AlertManager;
import com.legacy.salxaet.manager.PlayerData;
import com.legacy.salxaet.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CombatCheck {

    private final LAnticheat plugin;
    private final AlertManager alerts;

    public CombatCheck(LAnticheat plugin) {
        this.plugin = plugin;
        this.alerts = plugin.getAlertManager();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public void onAttack(Player attacker, Entity target, PlayerData data) {
        checkReach(attacker, target, data);
        checkHitboxAngle(attacker, target, data);
    }

    private void checkReach(Player attacker, Entity target, PlayerData data) {
        if (!cfg().getBoolean("checks.reach.enabled", true)) return;

        double distance = attacker.getEyeLocation().distance(target.getLocation());
        double max = cfg().getDouble("checks.reach.max-distance", 3.3);

        if (distance > max) {
            int v = data.addViolation("Reach");
            alerts.raise(attacker, "Reach", String.format("%.2f blok", distance), v);
        } else {
            data.decayViolation("Reach");
        }
    }

    private void checkHitboxAngle(Player attacker, Entity target, PlayerData data) {
        if (!cfg().getBoolean("checks.hitbox.enabled", true)) return;

        Location eye = attacker.getEyeLocation();
        Vector toTarget = target.getLocation().toVector().subtract(eye.toVector()).normalize();
        Vector looking = eye.getDirection().normalize();

        double dot = Math.max(-1.0, Math.min(1.0, looking.dot(toTarget)));
        double angleDegrees = Math.toDegrees(Math.acos(dot));
        double maxAngle = cfg().getDouble("checks.hitbox.max-angle-degrees", 100.0);

        if (angleDegrees > maxAngle) {
            int v = data.addViolation("Hitbox");
            alerts.raise(attacker, "Hitbox", String.format("aci=%.1f derece", angleDegrees), v);
        } else {
            data.decayViolation("Hitbox");
        }
    }
}
