package com.legacy.salxaet.data;

import org.bukkit.entity.Player;

public class PlayerData {
    private final Player player;
    private long lastAttackTime = 0;
    private int totalLogs = 0;

    public PlayerData(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public int getTotalLogs() {
        return totalLogs;
    }

    public void incrementTotalLogs() {
        this.totalLogs++;
    }

    public void resetTotalLogs() {
        this.totalLogs = 0;
    }
}
