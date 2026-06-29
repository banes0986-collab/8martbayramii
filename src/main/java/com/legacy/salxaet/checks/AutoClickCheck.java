package com.legacy.salxaet.checks;

import com.legacy.salxaet.LAnticheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

public class AutoClickCheck implements Listener {

    private final HashMap<UUID, Long> lastClickTime = new HashMap<>();
    private final HashMap<UUID, Long> lastClickDiff = new HashMap<>();
    private final HashMap<UUID, Integer> identicalPatterns = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        LAnticheat plugin = LAnticheat.getInstance();
        if (!plugin.isAnticheatEnabled() || event.getAction() != Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastClickTime.containsKey(uuid)) {
            long currentDiff = now - lastClickTime.get(uuid);
            long prevDiff = lastClickDiff.getOrDefault(uuid, 0L);

            // Tıklama süreleri ardışık olarak milimetrik eşitse makrodur
            if (currentDiff == prevDiff && currentDiff > 15 && currentDiff < 150) {
                int patternCount = identicalPatterns.getOrDefault(uuid, 0) + 1;
                if (patternCount > 6) {
                    plugin.triggerAlert(player, "AutoClicker (Macro)", "Milisaniye Sabitligi: " + currentDiff + "ms");
                    identicalPatterns.put(uuid, 0);
                } else {
                    identicalPatterns.put(uuid, patternCount);
                }
            } else {
                if (identicalPatterns.getOrDefault(uuid, 0) > 0) {
                    identicalPatterns.put(uuid, identicalPatterns.get(uuid) - 1);
                }
            }
            lastClickDiff.put(uuid, currentDiff);
        }
        lastClickTime.put(uuid, now);
    }
}
