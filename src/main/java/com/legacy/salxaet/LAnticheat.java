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
        getLogger().info("SalxAET v1.1 - Gelişmiş Aura, Reach ve Elytra Fix!");
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

        // FİŞEK VE ELYTRA MUAFİYETİ: Eğer oyuncu elytradaysa ve fişek basmışsa hareket kontrollerini esnet/atla
        if (player.isGliding()) {
            long timeSinceBoost = System.currentTimeMillis() - data.lastFireworkBoost;
            // Fişek basıldıktan sonraki 3 saniye (3000ms) boyunca hız limitlerini çok yüksek tut veya kontrolü gevşet
            double maxElytraSpeed = (timeSinceBoost < 3000) ? 4.5 : getConfig().getDouble("checks.elytra.max-speed", 2.2);
            
            Location from = event.getFrom();
            Location to = event.getTo();
            double deltaX = to.getX() - from.getX();
            double deltaZ = to.getZ() - from.getZ();
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            if (getConfig().getBoolean("checks.elytra.enabled", true) && horizontalDistance > maxElytraSpeed) {
                triggerAlert(player, "Elytra Speed", "Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
            }
            return; // Elytradaysa normal yürüme/uçma kontrollerine sokma
        }

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

        // NOWEB KONTROLÜ
        if (standBlock == Material.COBWEB && getConfig().getBoolean("checks.noweb.enabled", true)) {
            if (horizontalDistance > 0.25) {
                triggerAlert(player, "NoWeb", "Agda Hiz: " + String.format("%.2f", horizontalDistance));
                event.setTo(from);
                return;
            }
        }

        // SPEED KONTROLÜ (Limitler boş log atmaması için yukarı çekildi)
        if (getConfig().getBoolean("checks.speed.enabled", true)) {
            double maxNormalSpeed = isOnGround ? 0.96 : 1.05; // Limitler artırıldı
            if (isJumping) maxNormalSpeed = 1.25; // Zıplama limiti artırıldı

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

        // FLY KONTROLÜ
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

    private void registerPacketChecks() {
        // FİŞEK KULLANIMINI YAKALAMA (Sahte Speed loglarını kesmek için)
        protocolManager.addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_PLACE, PacketType.Play.Client.USE_ITEM) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    if (data == null) return;

                    try {
                        org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                        if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
                            data.lastFireworkBoost = System.currentTimeMillis();
                        }
                    } catch (Exception ignored) {}
                }
            }
        );

        // SAVAŞ KONTROLLERİ (AURA, REACH, WALL)
        protocolManager.addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    if (data == null) return;

                    // CPS / Makro Kontrolü
                    if (getConfig().getBoolean("checks.killaura.enabled", true)) {
                        long now = System.currentTimeMillis();
                        long diff = now - data.getLastAttackTime();
                        if (data.getLastAttackTime() != 0 && diff < getConfig().getInt("checks.killaura.min-ms-delay", 40)) {
                            triggerAlert(player, "Killaura (CPS)", "Gecikme: " + diff + "ms");
                        }
                        data.setLastAttackTime(now);
                    }

                    try {
                        Entity target = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
                        if (target instanceof Player) {
                            Player victim = (Player) target;
                            
                            // REACH KONTROLÜ
                            if (getConfig().getBoolean("checks.reach.enabled", true)) {
                                double distance = player.getLocation().distance(victim.getLocation());
                                double maxReach = getConfig().getDouble("checks.reach.max-distance", 3.5);
                                if (distance > maxReach) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Reach", "Mesafe: " + String.format("%.2f", distance));
                                }
                            }

                            // HITBOX / GÖRÜŞ AÇISI KONTROLÜ
                            if (getConfig().getBoolean("checks.hitbox.enabled", true)) {
                                Vector toVictim = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                                Vector playerLook = player.getLocation().getDirection().normalize();
                                double angle = Math.toDegrees(playerLook.angle(toVictim));
                                
                                double maxAngle = getConfig().getDouble("checks.hitbox.max-angle", 115.0);
                                if (angle > maxAngle) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Killaura (Hitbox)", "Aci: " + String.format("%.1f", angle));
                                }
                            }

                            // DUVAR ARKASI VURMA ENGELİ (ANTI-WALL)
                            if (getConfig().getBoolean("checks.antiwall.enabled", true)) {
                                if (!player.hasLineOfSight(victim)) {
                                    event.setCancelled(true);
                                    triggerAlert(player, "Killaura (Wall)", "Duvar arkasi hasar engellendi!");
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        );

        // AURA ROTASYON KONTROLÜ (Anlık hile kilitlenmelerini yakalar)
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

                    // Değerler normalize edilerek aşırı dönüş sapmaları yakalanır
                    if (deltaYaw > 300.0F && deltaPitch > 140.0F) {
                        triggerAlert(player, "Killaura (Rotation)", "Saliselik Donus: " + (int)deltaYaw + "°");
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
        public long lastFireworkBoost = 0;

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
                                     
