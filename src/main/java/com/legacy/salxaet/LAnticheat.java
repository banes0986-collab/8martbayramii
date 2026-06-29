package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin implements Listener, CommandExecutor {

    private static LAnticheat instance;
    private ProtocolManager protocolManager;
    private final HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        createMessagesConfig();
        
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketChecks();
        } else {
            getLogger().severe("ProtocolLib bulunamadi! Paket tabanli kontroller devre disi.");
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("salxaet").setExecutor(this);

        getLogger().info("========================================");
        getLogger().info("SalxAET v1.0 - Full Savas ve Hareket Korumasi!");
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
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("salxaet.admin")) {
                sender.sendMessage(getMessage("prefix") + "§cYetkiniz yok!");
                return true;
            }
            reloadPluginConfigs();
            sender.sendMessage(getMessage("prefix") + "§aAyarlar basariyla yenilendi!");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Kullanim: /salxaet reload");
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight()) return;
        if (player.isInsideVehicle()) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = to.getY() - from.getY();

        Material standBlock = player.getLocation().getBlock().getType();
        Material footBlock = player.getLocation().clone().add(0, -0.1, 0).getBlock().getType();
        boolean isOnGround = footBlock.isSolid() || player.isOnGround();

        boolean isJumping = deltaY > 0.419 && !isOnGround;

        if (standBlock == Material.COBWEB && getConfig().getBoolean("checks.noweb.enabled", true)) {
            if (horizontalDistance > 0.22) {
                triggerAlert(player, "NoWeb", "Agda Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
                return;
            }
        }

        if (player.isGliding() && getConfig().getBoolean("checks.elytra.enabled", true)) {
            double maxElytraSpeed = getConfig().getDouble("checks.elytra.max-speed", 1.8);
            if (horizontalDistance > maxElytraSpeed) {
                triggerAlert(player, "Elytra Flight", "Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
                return;
            }
        }

        if (!player.isGliding()) {
            if (getConfig().getBoolean("checks.speed.enabled", true)) {
                double maxNormalSpeed = isOnGround ? 0.88 : 0.98;
                if (isJumping) maxNormalSpeed = 1.15;

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
                if (Math.abs(deltaY) < 0.001 && horizontalDistance > 0.28) {
                    data.addFlag();
                    triggerAlert(player, "Fly", "Havada Suzulme");
                    
                    if (getConfig().getBoolean("checks.fly.rubberband", true) && data.getFlags() > getConfig().getInt("checks.fly.max-violations", 5)) {
                        event.setTo(from);
                        data.resetFlags();
                        return;
                    }
                }
            }
        }
        
        if (data.getFlags() > 0 && Math.random() < 0.1) {
            data.decreaseFlag();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!getConfig().getBoolean("checks.nuker.enabled", true)) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        long now = System.currentTimeMillis();
        if (now - data.lastBlockBreakTime < 1000) {
            data.blockBreaksInSecond++;
            int maxBreaks = getConfig().getInt("checks.nuker.max-breaks-per-sec", 15);
            if (data.blockBreaksInSecond > maxBreaks) {
                event.setCancelled(true);
                triggerAlert(player, "Nuker", "Saniyede " + data.blockBreaksInSecond + " blok!");
            }
        } else {
            data.lastBlockBreakTime = now;
            data.blockBreaksInSecond = 1;
        }
    }

    private void registerPacketChecks() {
        protocolManager.addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    if (data == null) return;

                    if (getConfig().getBoolean("checks.killaura.enabled", true)) {
                        long now = System.currentTimeMillis();
                        long diff = now - data.getLastAttackTime();
                        if (data.getLastAttackTime() != 0 && diff < getConfig().getInt("checks.killaura.min-ms-delay", 45)) {
                            triggerAlert(player, "Killaura (CPS)", "Gecikme: " + diff + "ms");
                        }
                        data.setLastAttackTime(now);
                    }

                    try {
                        Entity target = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
                        if (target instanceof Player) {
                            Player victim = (Player) target;
                            
                            if (getConfig().getBoolean("checks.reach.enabled", true)) {
                                double distance = player.getLocation().distance(victim.getLocation());
                                double maxReach = getConfig().getDouble("checks.reach.max-distance", 3.4);
                                if (distance > maxReach) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Reach", "Mesafe: " + String.format("%.2f", distance));
                                }
                            }

                            if (getConfig().getBoolean("checks.hitbox.enabled", true)) {
                                Vector toVictim = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                                Vector playerLook = player.getLocation().getDirection().normalize();
                                double angle = Math.toDegrees(playerLook.angle(toVictim));
                                
                                double maxAngle = getConfig().getDouble("checks.hitbox.max-angle", 110.0);
                                if (angle > maxAngle) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Killaura (Hitbox)", "Aci Sapmasi: " + String.format("%.1f", angle));
                                }
                            }

                            if (getConfig().getBoolean("checks.antiwall.enabled", true)) {
                                if (!player.hasLineOfSight(victim)) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Killaura (Wall)", "Duvar arkasindan vurdu!");
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        );

        protocolManager.addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.LOOK) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (!getConfig().getBoolean("checks.killaura.check-rotation", true)) return;

                    Player player = event.getPlayer();
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    if (data == null) return;

                    float yaw = event.getPacket().getFloat().read(0);
                    float pitch = event.getPacket().getFloat().read(1);

                    float deltaYaw = Math.abs(yaw - data.lastYaw);
                    float deltaPitch = Math.abs(pitch - data.lastPitch);

                    if (deltaYaw > 290.0F && deltaPitch > 130.0F) {
                        triggerAlert(player, "Killaura (Rotation)", "Anormal Donus: " + (int)deltaYaw);
                    }

                    data.lastYaw = yaw;
                    data.lastPitch = pitch;
                }
            }
        );
    }

    private void triggerAlert(Player player, String checkName, String details) {
        String prefix = getMessage("prefix");
        String format = getMessage("alert-format")
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%details%", details);
        
        String finalMessage = prefix + format;

        if (getConfig().getBoolean("settings.send-alerts", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp() || p.hasPermission("salxaet.admin")) {
                    p.sendMessage(finalMessage);
                }
            }
        }
        
        if (getConfig().getBoolean("settings.log-to-console", true)) {
            Bukkit.getLogger().warning("[SalxAET] " + player.getName() + " -> " + checkName + " (" + details + ")");
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
        private long lastAttackTime = 0;
        
        public long lastBlockBreakTime = 0;
        public int blockBreaksInSecond = 0;

        public float lastYaw = 0.0F;
        public float lastPitch = 0.0F;

        public PlayerData(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        public int getFlags() { return violationFlags; }
        public void addFlag() { this.violationFlags += 2; }
        public void decreaseFlag() { if (this.violationFlags > 0) this.violationFlags--; }
        public void resetFlags() { this.violationFlags = 0; }
        
        public long getLastAttackTime() { return lastAttackTime; }
        public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    }
                             }
        
