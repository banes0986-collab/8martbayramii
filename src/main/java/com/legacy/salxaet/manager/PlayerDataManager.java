package com.legacy.salxaet.manager;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Oyuncu başına tüm geçmiş verileri tutar:
 * yaw/pitch geçmişi, tıklama zamanları, hedef geçmişi, hitbox geçmişi.
 */
public class PlayerDataManager {

    // Rotasyon geçmişi maksimum eleman sayısı
    private static final int MAX_ROTATION_HISTORY = 40;
    // Hitbox geçmişi için maksimum süre (ms)
    private static final long MAX_BACKTRACK_MS = 1000L;

    // Yaw geçmişi: UUID → Deque<Float>
    private final Map<UUID, Deque<Float>> yawHistory    = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Float>> pitchHistory  = new ConcurrentHashMap<>();

    // Hitbox geçmişi: UUID → Deque<HistoricalBox>
    private final Map<UUID, Deque<HistoricalBox>> boxHistory = new ConcurrentHashMap<>();

    // Tıklama zamanları (CPS için): UUID → Deque<Long>
    private final Map<UUID, Deque<Long>> clickTimestamps = new ConcurrentHashMap<>();

    // Son saldırı zamanı
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();

    // Saldırı öncesi yaw/pitch
    private final Map<UUID, Float> preAttackYaw   = new ConcurrentHashMap<>();
    private final Map<UUID, Float> preAttackPitch = new ConcurrentHashMap<>();

    // Multi-target: UUID saldırgan → (UUID hedef → son saldırı ms)
    private final Map<UUID, Map<UUID, Long>> targetHistory = new ConcurrentHashMap<>();

    // ─── Rotasyon kayıt & sorgulama ──────────────────────────────────────────

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

    /** ticksAgo tik önceki yaw değerini döndürür (yoksa mevcut) */
    public float getYawTicksAgo(UUID uuid, int ticksAgo) {
        Deque<Float> dq = yawHistory.getOrDefault(uuid, new ArrayDeque<>());
        if (dq.isEmpty()) return 0f;
        Float[] arr = dq.toArray(new Float[0]);
        int idx = arr.length - 1 - ticksAgo;
        if (idx < 0) idx = 0;
        return arr[idx];
    }

    /** Son N tikteki yaw delta listesini döndürür */
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

    // ─── Hitbox (BoundingBox) geçmişi ────────────────────────────────────────

    public void recordBoundingBox(UUID uuid, BoundingBox box, Location loc) {
        Deque<HistoricalBox> dq = boxHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        // Eski kutuları temizle
        while (!dq.isEmpty() && now - dq.peekFirst().timestamp > MAX_BACKTRACK_MS) {
            dq.pollFirst();
        }
        dq.addLast(new HistoricalBox(now, box.clone(), loc.clone()));
    }

    /** ms önce oyuncunun BoundingBox'ını döndürür (en yakın olanı) */
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

    // ─── CPS (tıklama hızı) ──────────────────────────────────────────────────

    /** Tıklamayı kaydeder ve son 1 saniyedeki CPS'i döndürür */
    public double recordHitAndGetCPS(UUID uuid) {
        long now = System.currentTimeMillis();
        Deque<Long> dq = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        dq.addLast(now);
        // 1 saniyeden eskiyi temizle
        while (!dq.isEmpty() && now - dq.peekFirst() > 1000L) {
            dq.pollFirst();
        }
        return dq.size();
    }

    // ─── Saldırı öncesi rotasyon ─────────────────────────────────────────────

    public void setPreAttackRotation(UUID uuid, float yaw, float pitch) {
        preAttackYaw.put(uuid, yaw);
        preAttackPitch.put(uuid, pitch);
    }

    public float getPreAttackYaw(UUID uuid)   { return preAttackYaw.getOrDefault(uuid, 0f); }
    public float getPreAttackPitch(UUID uuid) { return preAttackPitch.getOrDefault(uuid, 0f); }

    // ─── Son saldırı zamanı ──────────────────────────────────────────────────

    public long getLastAttackTime(UUID uuid)       { return lastAttackTime.getOrDefault(uuid, 0L); }
    public void setLastAttackTime(UUID uuid, long t) { lastAttackTime.put(uuid, t); }

    // ─── Multi-target ─────────────────────────────────────────────────────────

    public void recordTarget(UUID attacker, UUID target) {
        Map<UUID, Long> targets = targetHistory.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());
        targets.put(target, System.currentTimeMillis());
    }

    /** windowMs ms içinde kaç farklı hedefe saldırdı? */
    public int getRecentTargetCount(UUID attacker, long windowMs) {
        Map<UUID, Long> targets = targetHistory.getOrDefault(attacker, Collections.emptyMap());
        long now = System.currentTimeMillis();
        targets.entrySet().removeIf(e -> now - e.getValue() > windowMs);
        return targets.size();
    }

    // ─── Temizlik ─────────────────────────────────────────────────────────────

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

    // ─── İç sınıf ─────────────────────────────────────────────────────────────

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
  
