package com.legacy.salxaet.checks.killaura;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.UUID;

public class KillAuraG implements Listener {

    private final HashMap<UUID, Float> lastAttackYaw = new HashMap<>();
    private final HashMap<UUID, Float> lastAttackPitch = new HashMap<>();

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled() || !(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        UUID pId = player.getUniqueId();
        
        float currentYaw = player.getLocation().getYaw();
        float currentPitch = player.getLocation().getPitch();

        if (lastAttackYaw.containsKey(pId)) {
            float lastYaw = lastAttackYaw.get(pId);
            float lastPitch = lastAttackPitch.get(pId);

            // İnsan eli vuruş yaparken kamerayı 0.0000000X hassasiyetle sabit tutamaz. Hileler tam merkeze kilitlenir.
            if (currentYaw == lastYaw && currentPitch == lastPitch && player.getVelocity().length() > 0.08) {
                plugin.triggerAlert(player, "KillAura (G/Heuristic)", "Sifir Kamera Sapmasi (HardLock)");
            }
        }

        lastAttackYaw.put(pId, currentYaw);
        lastAttackPitch.put(pId, currentPitch);
    }
}
