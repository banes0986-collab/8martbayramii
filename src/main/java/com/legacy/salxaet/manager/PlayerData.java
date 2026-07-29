package com.legacy.salxaet.manager;

import org.bukkit.Location;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Bir oyuncunun tum check gecmisini ve ihlal sayaclarini tutar.
 */
public class PlayerData {

    private final Map<String, Integer> violations = new HashMap<>();

    // Hareket takibi
    public Location lastLocation;
    public long lastMoveTime;
    public final Deque<Long> moveIntervalsMs = new ArrayDeque<>(); // Timer hack tespiti icin

    // Combat / KillAura takibi
    public long lastAttackTime;
    public final Deque<Long> clickTimestamps = new ArrayDeque<>();
    public final Deque<Float> yawSamples = new ArrayDeque<>();
    public final Deque<Float> pitchSamples = new ArrayDeque<>();
    public final Deque<long[]> recentTargets = new ArrayDeque<>(); // [entityId, timestamp]

    public int getViolations(String check) {
        return violations.getOrDefault(check, 0);
    }

    public int addViolation(String check) {
        int updated = getViolations(check) + 1;
        violations.put(check, updated);
        return updated;
    }

    public void resetViolation(String check) {
        violations.put(check, 0);
    }

    public void decayViolation(String check) {
        int current = getViolations(check);
        if (current > 0) {
            violations.put(check, current - 1);
        }
    }
}
