package com.legacy.salxaet.manager;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private static final int MAX_ROTATION_HISTORY = 40;
    private static final long MAX_BACKTRACK_MS = 1000L;

    private final Map<UUID, Deque<Float>> yawHistory    = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Float>> pitchHistory  = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<HistoricalBox>> boxHistory = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> clickTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Float> preAttackYaw   = new ConcurrentHashMap<>();
    private final Map<UUID, Float> preAttackPitch = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> targetHistory = new ConcurrentHashMap<>();

    public void recordYaw(UUID uuid, float yaw) {
        Deque<Float> dq = yawHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        if (dq.size() >= MAX_ROTATION_HISTORY) dq.pollFirst();
        dq.addLast(yaw);
    }

    public void recordPitch(UUID uuid, float pitch) {
        Deque<Float> dq = pitchHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        if (dq.size() >= MAX_ROTATION_HISTORY) dq.pollFirst();
        dq.addLast(pitch);
    }

    public float getYawTicksAgo(UUID uuid, int ticksAgo) {
        Deque<Float> dq = yawHistory.getOrDefault(uuid, new ArrayDeque<>());
        if (dq.isEmpty()) return 0f;
        Float[] arr = dq.toArray(new Float[0]);
        int idx = arr.length - 1 - ticksAgo;
        if (idx < 0) idx = 0;
        return arr[idx];
    }

    public List<Float> getYawDeltas(UUID uuid, int samples) {
        Deque<Float> dq = yawHistory.getOrDefault(uuid, new ArrayDeque<>());
        if (dq.size() < 2) return Collections.emptyList();
        Float[] arr = dq.toArray(new Float[0]);
        List<Float> deltas = new ArrayList<>();
        int start = Math.max(0, arr.length - samples);
        for (int i = start + 1; i < arr.length; i++) {
            deltas.add(Math.abs(arr[i] - arr[i - 1]));
        }
        return deltas;
    }

    public void recordBoundingBox(UUID uuid, BoundingBox box, Location loc) {
        Deque<HistoricalBox> dq = boxHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        while (!dq.isEmpty() && now - dq.peekFirst().timestamp > MAX_BACKTRACK_MS) {
            dq.pollFirst();
        }
        dq.addLast(new HistoricalBox(now, box.clone(), loc.clone()));
    }

    public HistoricalBox getBoxMsAgo(UUID uuid, long msAgo) {
        Deque<HistoricalBox> dq = boxHistory.getOrDefault(uuid, new ArrayDeque<>());
        if (dq.isEmpty()) return null;
        long target = System.currentTimeMillis() - msAgo;
        HistoricalBox best = null;
        long bestDiff = Long.MAX_VALUE;
        for (HistoricalBox hb : dq) {
            long diff = Math.abs(hb.timestamp - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = hb;
            }
        }
        return best;
    }

    public double recordHitAndGetCPS(UUID uuid) {
        long now = System.currentTimeMillis();
        Deque<Long> dq = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        dq.addLast(now);
        while (!dq.isEmpty() && now - dq.peekFirst() > 1000L) {
            dq.pollFirst();
        }
        return dq.size();
    }

    public void setPreAttackRotation(UUID uuid, float yaw, float pitch) {
        preAttackYaw.put(uuid, yaw);
        preAttackPitch.put(uuid, pitch);
    }

    public float getPreAttackYaw(UUID uuid)   { return preAttackYaw.getOrDefault(uuid, 0f); }
    public float getPreAttackPitch(UUID uuid) { return preAttackPitch.getOrDefault(uuid, 0f); }

    public long getLastAttackTime(UUID uuid)       { return lastAttackTime.getOrDefault(uuid, 0L); }
    public void setLastAttackTime(UUID uuid, long t) { lastAttackTime.put(uuid, t); }

    public void recordTarget(UUID attacker, UUID target) {
        Map<UUID, Long> targets = targetHistory.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());
        targets.put(target, System.currentTimeMillis());
    }

    public int getRecentTargetCount(UUID attacker, long windowMs) {
        Map<UUID, Long> targets = targetHistory.getOrDefault(attacker, Collections.emptyMap());
        long now = System.currentTimeMillis();
        targets.entrySet().removeIf(e -> now - e.getValue() > windowMs);
        return targets.size();
    }

    public void remove(UUID uuid) {
        yawHistory.remove(uuid);
        pitchHistory.remove(uuid);
        boxHistory.remove(uuid);
        clickTimestamps.remove(uuid);
        lastAttackTime.remove(uuid);
        preAttackYaw.remove(uuid);
        preAttackPitch.remove(uuid);
        targetHistory.remove(uuid);
    }

    public static class HistoricalBox {
        public final long timestamp;
        public final BoundingBox box;
        public final Location location;

        public HistoricalBox(long timestamp, BoundingBox box, Location location) {
            this.timestamp = timestamp;
            this.box = box;
            this.location = location;
        }
    }
                                                  }
