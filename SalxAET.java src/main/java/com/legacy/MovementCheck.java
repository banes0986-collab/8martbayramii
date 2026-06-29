package com.legacy.salxaet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementCheck implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Kreatif modda, izleyicide veya uçma yetkisi olanları es geç
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight()) {
            return;
        }

        LAnticheat.PlayerData data = LAnticheat.getInstance().getPlayerDataMap().get(player.getUniqueId());
        if (data == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Yatay mesafe (X ve Z eksenindeki hız) hesaplama
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Dikey mesafe (Y eksenindeki yükselme/düşme hızı) hesaplama
        double deltaY = to.getY() - from.getY();

        // 1. KONTROL: SPEED (Yatay Hız Limiti Kontrolü)
        // Normal bir oyuncu iksir alsa bile blok üzerinde yürürken 0.75 birimlik hızı geçemez.
        if (horizontalDistance > 0.75 && !player.isFlying()) {
            data.addFlag();
            triggerAlert(player, "Speed (Hiz Hilesi)", "Hiz: " + String.format("%.2f", horizontalDistance));
            if (data.getFlags() > 8) {
                event.setTo(from); // Oyuncuyu geri çek (Rubberband)
            }
        }

        // 2. KONTROL: FLY / AIR-WALK (Havada Kalma Süresi Kontrolü)
        // Eğer oyuncu blokların üzerinde değilse (havadaysa) ve Y ekseninde imkansız bir hareket yapıyorsa
        if (!player.getLocation().getBlock().getType().isSolid() && 
            !player.getEyeLocation().getBlock().getType().isSolid()) {
            
            // Havada süzülme veya sürekli yukarı doğru bloksuz yükselme kontrolü
            if (deltaY == 0.0 && horizontalDistance > 0.1) {
                data.addFlag();
                triggerAlert(player, "Fly (Uçma Hilesi)", "Havada Suzulme");
                if (data.getFlags() > 5) {
                    event.setTo(from);
                }
            }
        }
    }

    private void triggerAlert(Player player, String checkName, String details) {
        String message = "§8[§dSalxAET§8] §f" + player.getName() + " §7adli oyuncu §e" + checkName + " §7shuphesi firlatti! §8(" + details + ")";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("salxaet.admin")) {
                p.sendMessage(message);
            }
        }
        Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ")");
    }
}
