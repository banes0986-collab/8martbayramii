package com.legacy.salxaet.manager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public PlayerData get(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), id -> new PlayerData());
    }

    public void remove(Player player) {
        dataMap.remove(player.getUniqueId());
    }

    public void clear() {
        dataMap.clear();
    }
}
