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
        if (player.hasPermission("salxaet.bypass") || player.isOp()) return;

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        // Sadece pozisyon değişmişse devam et
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        long now = System.currentTimeMillis();
        long timeDelta = (data.lastMoveTime == 0) ? 50 : (now - data.lastMoveTime);
        if (timeDelta <= 0) timeDelta = 1;
        data.lastMoveTime = now;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Hız çarpanı hesapla
        double speedMultiplier = 1.0;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amp = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            speedMultiplier += 0.2 * (amp + 1);
        }
        if (isOnIce(player)) speedMultiplier += 0.5;

        // ── 1. Speed ─────────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.speed.enabled", true)
                && !player.isFlying()
                && !player.isGliding()
                && !player.isInsideVehicle()) {

            double threshold   = plugin.getConfig().getDouble("checks.speed.threshold", 0.96);
            double maxSpeed    = threshold * speedMultiplier;
            double normalizedSpeed = horizontalDist / (timeDelta / 50.0);

            if (normalizedSpeed > maxSpeed + 0.15) {
                int maxViol = plugin.getConfig().getInt("checks.speed.max-violations", 8);
                data.addFlag();
                if (data.getFlags() >= maxViol) {
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

        // ── 2. Fly ───────────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.fly.enabled", true)
                && !player.getAllowFlight()
                && !player.isGliding()
                && !player.isInsideVehicle()
                && !isNearClimbable(player)) {

            if (!player.isOnGround() && dy > 0.05) {
                boolean hasJumpBoost = player.hasPotionEffect(PotionEffectType.JUMP_BOOST);
                if (!hasJumpBoost) {
                    data.airTicks++;
                    int maxViol = plugin.getConfig().getInt("checks.fly.max-violations", 5);
                    if (data.airTicks > 10) {
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

        // ── 3. NoWeb ─────────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.noweb.enabled", true)) {
            if (isInsideWebBlock(player) && horizontalDist > 0.12) {
                long boostDelta = now - data.lastFireworkBoost;
                if (boostDelta > 3000) {
                    event.setCancelled(true);
                    plugin.triggerAlert(player, "NoWeb",
                            String.format("hiz=%.3f (max 0.12)", horizontalDist));
                }
            }
        }

        // ── 4. AntiWall ──────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.antiwall.enabled", true)) {
            if (data.lastLocation != null
                    && data.lastLocation.getWorld() != null
                    && data.lastLocation.getWorld().equals(to.getWorld())) {

                double moveDist = data.lastLocation.distance(to);
                if (moveDist > 0.3 && moveDist < 5.0) {
                    String wallResult = checkWallPass(data.lastLocation, to);
                    if (wallResult != null) {
                        event.setCancelled(true);
                        plugin.triggerAlert(player, "AntiWall", wallResult);
                    }
                }
            }
        }

        // ── 5. Elytra hız ────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("checks.elytra.enabled", true)
                && player.isGliding()) {

            long boostDelta = now - data.lastFireworkBoost;
            if (boostDelta > 3000) {
                double totalSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double maxElytra  = plugin.getConfig().getDouble("checks.elytra.max-speed", 2.2);
                if (totalSpeed > maxElytra) {
                    plugin.triggerAlert(player, "Elytra",
                            String.format("hiz=%.2f > %.2f", totalSpeed, maxElytra));
                }
            }
        }

        data.lastLocation = to.clone();
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    private boolean isOnIce(Player player) {
        Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
        Material t = below.getType();
        return t == Material.ICE || t == Material.PACKED_ICE || t == Material.BLUE_ICE;
    }

    private boolean isInsideWebBlock(Player player) {
        return player.getLocation().getBlock().getType() == Material.COBWEB;
    }

    private boolean isNearClimbable(Player player) {
        Material t = player.getLocation().getBlock().getType();
        return t == Material.LADDER || t == Material.VINE
                || t == Material.SCAFFOLDING || t == Material.WEEPING_VINES
                || t == Material.TWISTING_VINES || t == Material.CAVE_VINES;
    }

    /**
     * İki nokta arasında katı blok geçişi var mı?
     * Varsa blok adını döndürür, yoksa null döner.
     */
    private String checkWallPass(Location from, Location to) {
        if (from.getWorld() == null) return null;

        double dist = from.distance(to);
        int steps = (int) Math.ceil(dist / 0.4);
        if (steps < 2) return null;

        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;

        int solidCount = 0;
        String lastSolid = "";

        for (int i = 1; i < steps; i++) {
            Location sample = from.clone().add(dx * i, dy * i, dz * i);
            Block block = sample.getBlock();
            Material mat = block.getType();

            if (mat.isSolid() && !isPassable(mat)) {
                solidCount++;
                lastSolid = mat.name();
                if (solidCount >= 2) {
                    return "blok=" + lastSolid;
                }
            }
        }
        return null;
    }

    private boolean isPassable(Material type) {
        switch (type) {
            case SHORT_GRASS:
            case TALL_GRASS:
            case FERN:
            case LARGE_FERN:
            case DEAD_BUSH:
            case SNOW:
            case SUGAR_CANE:
            case BAMBOO:
            case VINE:
            case LADDER:
            case COBWEB:
            case WATER:
            case LAVA:
            case AIR:
                return true;
            default:
                return false;
        }
    }
                }
    
