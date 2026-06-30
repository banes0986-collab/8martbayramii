package com.legacy.salxaet.utils;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;

public class MathUtils {

    public static float yawDelta(float a, float b) {
        float delta = Math.abs(a - b) % 360f;
        if (delta > 180f) delta = 360f - delta;
        return delta;
    }

    public static double distanceToBoundingBox(Vector origin, BoundingBox box) {
        double dx = Math.max(0, Math.max(box.getMinX() - origin.getX(), origin.getX() - box.getMaxX()));
        double dy = Math.max(0, Math.max(box.getMinY() - origin.getY(), origin.getY() - box.getMaxY()));
        double dz = Math.max(0, Math.max(box.getMinZ() - origin.getZ(), origin.getZ() - box.getMaxZ()));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double variance(List<Float> values) {
        if (values == null || values.size() < 2) return 0.0;
        double sum = 0;
        for (float v : values) sum += v;
        double mean = sum / values.size();
        double sqSum = 0;
        for (float v : values) sqSum += (v - mean) * (v - mean);
        return sqSum / values.size();
    }

    public static double stddev(List<Float> values) {
        return Math.sqrt(variance(values));
    }
}
