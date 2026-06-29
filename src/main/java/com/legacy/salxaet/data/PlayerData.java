package com.legacy.salxaet.data;

import org.bukkit.entity.Player;

public class PlayerData {
    private final Player player;
    private int violationFlags = 0;
    private int totalLogs = 0;
    private long lastAttackTime = 0;
    public long lastFireworkBoost = 0;

    public float lastYaw = 0.0F;
    public float lastPitch = 0.0F;

    public PlayerData(Player player) { this.player = player; }
    public Player getPlayer() { return player; }
    
    public int getFlags() { return violationFlags; }
    public void addFlag() { this.violationFlags += 2; }
    public void decreaseFlag() { if (this.violationFlags > 0) this.violationFlags--; }
    public void resetFlags() { this.violationFlags = 0; }
    
    public int getTotalLogs() { return totalLogs; }
    public void incrementTotalLogs() { this.totalLogs++; }
    public void resetTotalLogs() { this.totalLogs = 0; }
    
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
}
