package com.legacy.salxaet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MovementListener implements Listener {

    private final LAnticheat plugin;

    public MovementListener(LAnticheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.isAnticheatEnabled()) return;

        Player player = event.getPlayer();
        LAnticheat.PlayerData data = plugin.playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        // Yönetici / uçuş izni olanları atla
        if (player.hasPermission("salxaet.bypass")) return;
        if (player.isOp()) return;

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        // Sadece pozisyon değişmişse devam et
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        long now = System.currentTimeMillis();
        long timeDelta = (data.lastMoveTime == 0) ? 50 : (now - data.lastMoveTime);
        if (timeDelta == 0) timeDelta = 1;
        data.lastMoveTime = now;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // ── Hız toleransları hesapla ──────────────────────────────────────────
        double speedMultiplier = 1.0;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amp = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            speedMultiplier += 0.2 * (amp + 1);
        }
        boolean onIce = isOnIce(player);
        if (onIce) speedMultiplier += 0.5;

        // ── 1. Speed kontrolü ─────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.speed.enabled", true)
                && !player.isFlying()
                && !player.isGliding()
                && !player.isInsideVehicle()) {

            double threshold = plugin.getConfig().getDouble("checks.speed.threshold", 0.96);
            double maxSpeed  = threshold * speedMultiplier;

            // Tik başına normalize et (sunucu gecikmesi toleransı)
            double normalizedSpeed = horizontalDist / (timeDelta / 50.0);

            if (normalizedSpeed > maxSpeed + 0.15) {
                int maxViol = plugin.getConfig().getInt("checks.speed.max-violations", 8);
                data.addFlag();
                if (data.getFlags() >= maxViol) {
                    // Rubber-band: eski konuma geri çek
                    if (plugin.getConfig().getBoolean("checks.speed.rubberband", true)) {
                        event.setTo(from.clone());
                    }
                    plugin.triggerAlert(player, "Speed",
                            String.format("%.3f > %.3f b/t", normalizedSpeed, maxSpeed));
                }
            } else {
                data.decreaseFlag();
            }
        }

        // ── 2. Fly kontrolü ───────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.fly.enabled", true)
                && !player.getAllowFlight()
                && !player.isGliding()
                && !player.isInsideVehicle()
                && !isNearClimbable(player)) {

            boolean onGround = player.isOnGround();

            if (!onGround && dy > 0.05) {
                // Zıplama potion kontrolü
                boolean hasJumpBoost = player.hasPotionEffect(PotionEffectType.JUMP_BOOST);
                if (!hasJumpBoost) {
                    data.airTicks++;
                    if (data.airTicks > 10) { // ~0.5 saniye havada
                        int maxViol = plugin.getConfig().getInt("checks.fly.max-violations", 5);
                        data.addFlag();
                        if (data.getFlags() >= maxViol) {
                            if (plugin.getConfig().getBoolean("checks.fly.rubberband", true)) {
                                event.setTo(from.clone());
                            }
                            plugin.triggerAlert(player, "Fly",
                                    "airTicks=" + data.airTicks);
                        }
                    }
                }
            } else {
                data.airTicks = 0;
            }
        }

        // ── 3. NoWeb (ağ içinde hareket) ─────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.noweb.enabled", true)) {
            if (isInsideWebBlock(player) && horizontalDist > 0.12) {
                // Elytra boost yakınsa atla
                long boostDelta = now - data.lastFireworkBoost;
                if (boostDelta > 3000) {
                    event.setCancelled(true);
                    plugin.triggerAlert(player, "NoWeb",
                            String.format("%.3f > 0.12 b/t (ağ içi)", horizontalDist));
                }
            }
        }

        // ── 4. AntiWall (duvar arkası hareket) ────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.antiwall.enabled", true)) {
            if (data.lastLocation != null) {
                if (isTeleportThroughWall(data.lastLocation, to)) {
                    event.setCancelled(true);
                    plugin.triggerAlert(player, "AntiWall",
                            "Katı bloktan geçiş tespit edildi");
                }
            }
        }

        // ── 5. Elytra hız kontrolü ────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.elytra.enabled", true)
                && player.isGliding()) {

            long boostDelta = now - data.lastFireworkBoost;
            boolean boosted = boostDelta < 3000; // Boost 3 saniye boyunca tolerans

            if (!boosted) {
                double totalSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double maxElytra  = plugin.getConfig().getDouble("checks.elytra.max-speed", 2.2);
                if (totalSpeed > maxElytra) {
                    plugin.triggerAlert(player, "Elytra Speed",
                            String.format("%.2f > %.2f b/t", totalSpeed, maxElytra));
                }
            }
        }

        data.lastLocation = to.clone();
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    private boolean isOnIce(Player player) {
        Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
        Material type = below.getType();
        return type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE;
    }

    private boolean isInsideWebBlock(Player player) {
        Block block = player.getLocation().getBlock();
        return block.getType() == Material.COBWEB;
    }

    private boolean isNearClimbable(Player player) {
        Block block = player.getLocation().getBlock();
        Material type = block.getType();
        return type == Material.LADDER || type == Material.VINE
                || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES
                || type == Material.TWISTING_VINES || type == Material.CAVE_VINES;
    }

    /**
     * İki nokta arasındaki çizgide katı bir blok olup olmadığını kontrol eder.
     * AntiWall'ın temelidir.
     */
    private boolean isTeleportThroughWall(Location from, Location to) {
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return false;

        double dist = from.distance(to);
        // Çok kısa mesafeler için kontrol yapma (normal hareket)
        if (dist < 0.5 || dist > 5.0) return false;

        // Çizgi boyunca örnekleme
        int steps = (int) Math.ceil(dist / 0.5);
        Vector direction = to.toVector().subtract(from.toVector()).normalize();

        int solidCount = 0;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            Location sample = from.clone().add(
                    direction.getX() * dist * t,
                    direction.getY() * dist * t,
                    direction.getZ() * dist * t
            );
            Block block = sample.getBlock();
            if (block.getType().isSolid() && !isPassableForPlayer(block.getType())) {
                solidCount++;
                if (solidCount >= 2) return true; // En az 2 solid blok → kesin duvar
            }
        }
        return false;
    }

    /**
     * Oyuncunun içinden geçebileceği "solid" blok istisnalarını tanımlar.
     */
    private boolean isPassableForPlayer(Material type) {
        return type == Material.GRASS || type == Material.TALL_GRASS
                || type == Material.FERN || type == Material.LARGE_FERN
                || type == Material.DEAD_BUSH || type == Material.SNOW
                || type == Material.SUGAR_CANE || type == Material.BAMBOO
                || type == Material.VINE || type == Material.LADDER
                || type == Material.COBWEB || type == Material.WATER
                || type == Material.LAVA;
    }
}
