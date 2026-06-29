package com.legacy.salxaet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.FluidCollisionMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin implements Listener, CommandExecutor {

    private static LAnticheat instance;
    private final HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private boolean isAnticheatEnabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        createMessagesConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("salxaet").setExecutor(this);

        getLogger().info("========================================");
        getLogger().info("SalxAET v1.4 - MainThread Core Devreye Alindi!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        playerDataMap.clear();
    }

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadPluginConfigs() {
        reloadConfig();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String path) {
        String msg = messagesConfig.getString(path, "");
        if (msg.isEmpty()) {
            if (path.equals("prefix")) return "§8[§dSalxAET§8] ";
            if (path.equals("alert-format")) return "§f%player% §7adli oyuncu §e%check% §7shuphesi firlatti! §8(%details%)";
            if (path.equals("commands.no-permission")) return "§cYetkiniz yok!";
            if (path.equals("commands.already-active")) return "§eSalxAET zaten aktif durumda!";
            if (path.equals("commands.activated")) return "§aHile korumasi basariyla devreye sokuldu!";
            if (path.equals("commands.already-disabled")) return "§eSalxAET zaten devre disi!";
            if (path.equals("commands.disabled")) return "§cHile korumasi tamamen kapatildi!";
            if (path.equals("commands.reloaded")) return "§aAyarlar basariyla yenilendi!";
            if (path.equals("kick.reason")) return "§c§l[SalxAET]\n\n§7Bu sunucudan uzaklastirildiniz!\n§bSebep: §eHile Kullanimi (Şüpheli Paket Gönderimi)\n\n§7Eğer bunun bir hata olduğunu düşünüyorsanız yetkililere bildirin.";
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("salxaet.admin")) {
            sender.sendMessage(getMessage("prefix") + getMessage("commands.no-permission"));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("ac")) {
                if (isAnticheatEnabled) {
                    sender.sendMessage(getMessage("prefix") + getMessage("commands.already-active"));
                    return true;
                }
                isAnticheatEnabled = true;
                sender.sendMessage(getMessage("prefix") + getMessage("commands.activated"));
                return true;
            }
            
            if (args[0].equalsIgnoreCase("kapat")) {
                if (!isAnticheatEnabled) {
                    sender.sendMessage(getMessage("prefix") + getMessage("commands.already-disabled"));
                    return true;
                }
                isAnticheatEnabled = false;
                sender.sendMessage(getMessage("prefix") + getMessage("commands.disabled"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadPluginConfigs();
                sender.sendMessage(getMessage("prefix") + getMessage("commands.reloaded"));
                return true;
            }
        }
        
        sender.sendMessage(ChatColor.RED + "Kullanim: /salxaet <ac | kapat | reload>");
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isAnticheatEnabled) return;
        Player player = event.getPlayer();
        
        // FİŞEK TESPİTİ (Tamamen Güvenli MainThread Üzerinden)
        if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
            PlayerData data = playerDataMap.get(player.getUniqueId());
            if (data != null) {
                data.lastFireworkBoost = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!isAnticheatEnabled) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight()) return;
        if (player.isInsideVehicle()) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // KILLAURA ANLIK ROTASYON (SNAP) SENSÖRÜ
        float yaw = to.getYaw();
        float pitch = to.getPitch();
        float deltaYaw = Math.abs(yaw - data.lastYaw);
        float deltaPitch = Math.abs(pitch - data.lastPitch);

        if (deltaYaw > 55.0F && deltaYaw < 300.0F && deltaPitch > 25.0F) {
            // Eğer oyuncu son 1 saniye içinde birine vurduysa ve kafası aniden döndüyse yakala
            if (System.currentTimeMillis() - data.getLastAttackTime() < 1000) {
                triggerAlert(player, "Killaura (Rotation)", "Ani Donus: " + (int)deltaYaw + "°");
            }
        }
        data.lastYaw = yaw;
        data.lastPitch = pitch;

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;

        // ELYTRA KORUMASI
        if (player.isGliding()) {
            long timeSinceBoost = System.currentTimeMillis() - data.lastFireworkBoost;
            if (timeSinceBoost < 3500) return; 
            
            double maxElytraSpeed = getConfig().getDouble("checks.elytra.max-speed", 2.2);
            double deltaX = to.getX() - from.getX();
            double deltaZ = to.getZ() - from.getZ();
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            if (getConfig().getBoolean("checks.elytra.enabled", true) && horizontalDistance > maxElytraSpeed) {
                triggerAlert(player, "Elytra Speed", "Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
            }
            return;
        }

        long groundBoostDiff = System.currentTimeMillis() - data.lastFireworkBoost;
        if (groundBoostDiff < 3000) return;

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = to.getY() - from.getY();

        Material standBlock = player.getLocation().getBlock().getType();
        Material footBlock = player.getLocation().clone().add(0, -0.1, 0).getBlock().getType();
        boolean isOnGround = footBlock.isSolid() || player.isOnGround();

        boolean isJumping = deltaY > 0.419 && !isOnGround;

        if (standBlock == Material.COBWEB && getConfig().getBoolean("checks.noweb.enabled", true)) {
            if (horizontalDistance > 0.25) {
                triggerAlert(player, "NoWeb", "Agda Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
                return;
            }
        }

        if (getConfig().getBoolean("checks.speed.enabled", true)) {
            double maxNormalSpeed = isOnGround ? 0.96 : 1.05;
            if (isJumping) maxNormalSpeed = 1.25;

            if (horizontalDistance > maxNormalSpeed) {
                data.addFlag();
                triggerAlert(player, "Speed", "Hiz: " + String.format("%.2f", horizontalDistance));
                
                if (getConfig().getBoolean("checks.speed.rubberband", true) && data.getFlags() > getConfig().getInt("checks.speed.max-violations", 8)) {
                    event.setTo(from);
                    data.resetFlags();
                    return;
                }
            }
        }

        if (getConfig().getBoolean("checks.fly.enabled", true) && !isOnGround && !isJumping) {
            if (Math.abs(deltaY) < 0.001 && horizontalDistance > 0.35) {
                data.addFlag();
                triggerAlert(player, "Fly", "Havada Suzulme");
                
                if (getConfig().getBoolean("checks.fly.rubberband", true) && data.getFlags() > getConfig().getInt("checks.fly.max-violations", 5)) {
                    event.setTo(from);
                    data.resetFlags();
                    return;
                }
            }
        }
        
        if (data.getFlags() > 0 && Math.random() < 0.1) {
            data.decreaseFlag();
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isAnticheatEnabled) return;
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        // 1. Gelişmiş CPS / Hız Kontrolü
        if (getConfig().getBoolean("checks.killaura.enabled", true)) {
            long now = System.currentTimeMillis();
            long diff = now - data.getLastAttackTime();
            if (data.getLastAttackTime() != 0 && diff < getConfig().getInt("checks.killaura.min-ms-delay", 45)) {
                event.setCancelled(true);
                triggerAlert(player, "Killaura (CPS)", "Gecikme: " + diff + "ms");
                return;
            }
            data.setLastAttackTime(now);
        }

        // 2. Reach Kontrolü (Vuruş Mesafesi)
        if (getConfig().getBoolean("checks.reach.enabled", true)) {
            double distance = player.getLocation().distance(victim.getLocation());
            double maxReach = getConfig().getDouble("checks.reach.max-distance", 3.3);
            if (player.isGliding()) maxReach += 1.2; 

            if (distance > maxReach) {
                event.setCancelled(true);
                triggerAlert(player, "Reach/ElyTarget", "Mesafe: " + String.format("%.2f", distance));
                return;
            }
        }

        // 3. Hitbox Kontrolü (Açı Filtresi - 35 Dereceye Sertleştirildi)
        if (getConfig().getBoolean("checks.hitbox.enabled", true)) {
            Vector toVictim = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            Vector playerLook = player.getLocation().getDirection().normalize();
            double angle = Math.toDegrees(playerLook.angle(toVictim));
            
            double maxAngle = getConfig().getDouble("checks.hitbox.max-angle", 35.0);
            if (angle > maxAngle) {
                event.setCancelled(true);
                triggerAlert(player, "Killaura (Hitbox)", "Aci Sapmasi: " + String.format("%.1f", angle) + "°");
                return;
            }
        }

        // 4. KUSURSUZ DUVAR ARKASI HASAR ENGELLEYİCİ (Sunucu Ana İş Parçacığında Çalışır)
        if (getConfig().getBoolean("checks.antiwall.enabled", true)) {
            Location eyeLoc = player.getEyeLocation();
            Location targetLoc = victim.getEyeLocation();
            double distance = eyeLoc.distance(targetLoc);
            Vector dir = targetLoc.toVector().subtract(eyeLoc.toVector()).normalize();

            // İki oyuncu arasındaki blokları tarar
            RayTraceResult ray = player.getWorld().rayTraceBlocks(eyeLoc, dir, distance, FluidCollisionMode.NEVER, true);
            if (ray != null && ray.getHitBlock() != null && ray.getHitBlock().getType().isSolid()) {
                event.setCancelled(true);
                triggerAlert(player, "Killaura (Wall)", "Duvar arkasi engel: " + ray.getHitBlock().getType().name());
            }
        }
    }

    private void triggerAlert(Player player, String checkName, String details) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        data.incrementTotalLogs();

        String prefix = getMessage("prefix");
        String format = getMessage("alert-format")
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%details%", details);
        
        String finalMessage = prefix + format + " §8[" + data.getTotalLogs() + "/15]";

        if (getConfig().getBoolean("settings.send-alerts", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp() || p.hasPermission("salxaet.admin")) {
                    p.sendMessage(finalMessage);
                }
            }
        }
        
        if (getConfig().getBoolean("settings.log-to-console", true)) {
            Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ") [" + data.getTotalLogs() + "/15]");
        }

        if (data.getTotalLogs() >= 15) {
            data.resetTotalLogs();
            String kickMessage = getMessage("kick.reason");
            Bukkit.getScheduler().runTask(this, () -> {
                player.kickPlayer(kickMessage);
            });
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataMap.put(player.getUniqueId(), new PlayerData(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }

    public static LAnticheat getInstance() { return instance; }

    public static class PlayerData {
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
            }
                                   
