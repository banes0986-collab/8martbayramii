package com.legacy.salxaet.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class RayUtils {

    /**
     * Atıcının gözünden hedefe doğru ray atar.
     * Arada blok yoksa true döner (görüş hattı açık).
     */
    public static boolean isUnblocked(World world, Location from, Location to) {
        if (world == null || from == null || to == null) return true;
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length < 0.01) return true;
        dir.normalize();

        RayTraceResult result = world.rayTraceBlocks(
                from, dir, length,
                FluidCollisionMode.NEVER,
                true
        );
        return result == null || result.getHitBlock() == null;
    }

    /**
     * BoundingBox ile ray kesişimi kontrol eder.
     * Hedefe tam hitbox üzerinden isabet ediliyor mu?
     */
    public static boolean rayIntersectsBox(Location eyeLoc, BoundingBox box) {
        if (eyeLoc == null || box == null) return false;

        // Genişletilmiş hitbox (0.1 blok tolerans)
        BoundingBox expanded = box.clone().expand(0.1);

        Vector eye = eyeLoc.toVector();
        Vector dir = eyeLoc.getDirection().normalize();

        // Slab yöntemi ile AABB kesişimi
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        double[] mins = {expanded.getMinX(), expanded.getMinY(), expanded.getMinZ()};
        double[] maxs = {expanded.getMaxX(), expanded.getMaxY(), expanded.getMaxZ()};
        double[] eyeArr = {eye.getX(), eye.getY(), eye.getZ()};
        double[] dirArr = {dir.getX(), dir.getY(), dir.getZ()};

        for (int i = 0; i < 3; i++) {
            if (Math.abs(dirArr[i]) < 1e-8) {
                if (eyeArr[i] < mins[i] || eyeArr[i] > maxs[i]) return false;
            } else {
                double t1 = (mins[i] - eyeArr[i]) / dirArr[i];
                double t2 = (maxs[i] - eyeArr[i]) / dirArr[i];
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return false;
            }
        }
        return tMax >= 0;
    }
}
