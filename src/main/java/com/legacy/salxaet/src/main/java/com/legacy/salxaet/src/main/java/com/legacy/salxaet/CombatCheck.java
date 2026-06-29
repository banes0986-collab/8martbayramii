package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CombatCheck {

    public static void register() {
        LAnticheat.getInstance().getProtocolManager().addPacketListener(
            new PacketAdapter(LAnticheat.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    LAnticheat.PlayerData data = LAnticheat.getInstance().getPlayerDataMap().get(player.getUniqueId());
                    
                    if (data == null) return;

                    long now = System.currentTimeMillis();
                    long lastAttack = data.getLastAttackTime();
                    
                    if (lastAttack != 0) {
                        long diff = now - lastAttack;
                        if (diff < 45) {
                            data.addFlag();
                            Bukkit.getScheduler().runTask(LAnticheat.getInstance(), () -> {
                                triggerAlert(player, "Killaura (Savas Hilesi)", "Milisaniye: " + diff + "ms");
                            });
                        }
                    }
                    data.setLastAttackTime(now);
                }
            }
        );
    }

    private static void triggerAlert(Player player, String checkName, String details) {
        String message = "§8[§dSalxAET§8] §f" + player.getName() + " §7adli oyuncu §c" + checkName + " §7shuphesi firlatti! §8(" + details + ")";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("salxaet.admin")) {
                p.sendMessage(message);
            }
        }
        Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ")");
    }
}
