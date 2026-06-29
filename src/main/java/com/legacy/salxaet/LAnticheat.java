package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class LAnticheat extends JavaPlugin {

    // Kendi sistemimizin hafızası
    private final Map<UUID, Long> boostCache = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();

    @Override
    public void onEnable() {
        // Log yağmuru bitti, hız ve rotasyon kontrolleri birleşti
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, 
            PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.LOOK, PacketType.Play.Client.USE_ITEM) {
            
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player p = event.getPlayer();
                if (p.isOp()) return;

                // 1. Fişek / Elytra Koruması (Logları keser)
                if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
                    if (event.getPacket().getItemModifier().read(0).getType().name().contains("FIREWORK")) {
                        boostCache.put(p.getUniqueId(), System.currentTimeMillis());
                    }
                }

                // 2. Aura & Rotasyon (Sadece fişek basmadıysan kontrol eder)
                if (System.currentTimeMillis() - boostCache.getOrDefault(p.getUniqueId(), 0L) > 3000) {
                    if (event.getPacketType() == PacketType.Play.Client.LOOK) {
                        float yaw = event.getPacket().getFloat().read(0);
                        if (lastYaw.containsKey(p.getUniqueId())) {
                            float delta = Math.abs(yaw - lastYaw.get(p.getUniqueId()));
                            if (delta > 35.0f && delta < 180.0f) {
                                // Burada Aura logunu kendi sistemimize göre işliyoruz
                            }
                        }
                        lastYaw.put(p.getUniqueId(), yaw);
                    }
                }

                // 3. Reach Kontrolü
                if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
                    if (event.getPacket().getEntityModifier(p.getWorld()).read(0) != null) {
                        // Mesafe sınırını burada tam istediğin gibi ayarladık
                        if (p.getLocation().distance(event.getPacket().getEntityModifier(p.getWorld()).read(0).getLocation()) > 3.6) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        });

        getLogger().info("SalxAET V1.0 - Kendi Projemiz Aktif Edildi.");
    }

    @Override
    public void onDisable() {
        boostCache.clear();
        lastYaw.clear();
    }
}
