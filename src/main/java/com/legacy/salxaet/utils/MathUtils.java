package com.legacy.salxaet.utils;

import java.util.Collection;

public class MathUtils {

    private MathUtils() {}

    public static double average(Collection<Float> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (float v : values) sum += v;
        return sum / values.size();
    }

    public static double standardDeviation(Collection<Float> values) {
        if (values.size() < 2) return 0;
        double mean = average(values);
        double sumSq = 0;
        for (float v : values) {
            sumSq += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    /** Iki aci arasindaki en kisa farki (0-180) dondurur. */
    public static float angleDiff(float a, float b) {
        float diff = Math.abs(a - b) % 360f;
        return diff > 180f ? 360f - diff : diff;
    }
}
