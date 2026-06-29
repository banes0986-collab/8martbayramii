package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class LAnticheat extends JavaPlugin {

    public static LAnticheat instance;
    public HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
    private ProtocolManager protocolManager;
    private boolean isAnticheatEnabled = true;

    private File messagesFile;
    public FileConfiguration messagesConfig;

    public String PREFIX;
    public String ALERT_FORMAT;
    public String MSG_NO_PERMISSION;
    public String MSG_ALREADY_ACTIVE;
    public String MSG_ACTIVATED;
    public String MSG_ALREADY_DISABLED;
    public String MSG_DISABLED;
    public String MSG_RELOADED;
    public String KICK_REASON;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createMessagesConfig();
        loadMessages();

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketChecks();
        } else {
            getLogger().severe("ProtocolLib bulunamadi! Paket tabanli kontroller devre disi.");
        }

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getCommand("salxaet").setExecutor(new AnticheatCommand(this));

        // Her 10 saniyede ihlal azalt
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (PlayerData data : playerDataMap.values()) {
                data.decreaseFlag();
            }
        }, 200L, 200L);

        getLogger().info("SalxAET v2.0 aktif!");
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

    public void reloadMessages() {
        reloadConfig();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        loadMessages();
    }

    private void loadMessages() {
        FileConfiguration cfg = messagesConfig;
        PREFIX             = color(cfg.getString("prefix", "&8[&dSalxAET&8] "));
        ALERT_FORMAT       = color(cfg.getString("alert-format", "&f%player% &7-> &e%check% &8| &7%details%"));
        MSG_NO_PERMISSION  = color(cfg.getString("commands.no-permission",    "&cYetkiniz yok!"));
        MSG_ALREADY_ACTIVE = color(cfg.getString("commands.already-active",   "&eSalxAET zaten aktif!"));
        MSG_ACTIVATED      = color(cfg.getString("commands.activated",        "&aHile koruması devreye alındı!"));
        MSG_ALREADY_DISABLED=color(cfg.getString("commands.already-disabled", "&eSalxAET zaten devre dışı!"));
        MSG_DISABLED       = color(cfg.getString("commands.disabled",         "&cHile koruması kapatıldı!"));
        MSG_RELOADED       = color(cfg.getString("commands.reloaded",         "&aAyarlar yenilendi!"));
        KICK_REASON        = color(cfg.getString("kick.reason",
                "&c&l[SalxAET]\n\n&7Hile tespit edildi!\n&bSebep: &eHile Kullanımı"));
    }

    private void registerPacketChecks() {

        // Firework boost takibi
        protocolManager.addPacketListener(new PacketAdapter(
                this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                Player player = event.getPlayer();
                PlayerData data = playerDataMap.get(player.getUniqueId());
                if (data == null) return;
                try {
                    org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null && item.getType() == org.bukkit.Material.FIREWORK_ROCKET) {
                        data.lastFireworkBoost = System.currentTimeMillis();
                    }
                } catch (Exception ignored) {}
            }
        });

        // USE_ENTITY → KillAura CPS + Reach + Hitbox
        protocolManager.addPacketListener(new PacketAdapter(
                this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                Player player = event.getPlayer();
                PlayerData data = playerDataMap.get(player.getUniqueId());
                if (data == null) return;
                if (player.hasPermission("salxaet.bypass") || player.isOp()) return;

                try {
                    // 1.21'de action integer olarak gelir: 1 = ATTACK
                    int actionId = event.getPacket().getIntegers().read(1);
                    if (actionId != 1) return; // Sadece ATTACK

                    // ── KillAura CPS ──────────────────────────────────────
                    if (getConfig().getBoolean("checks.killaura.enabled", true)) {
                        long now = System.currentTimeMillis();
                        long delay = now - data.getLastAttackTime();
                        int minDelay = getConfig().getInt("checks.killaura.min-ms-delay", 40);

                        if (data.getLastAttackTime() != 0 && delay < minDelay) {
                            triggerAlert(player, "KillAura", "CPS delay=" + delay + "ms < " + minDelay + "ms");
                        }
                        data.setLastAttackTime(now);
                    }

                    // ── Reach ─────────────────────────────────────────────
                    if (getConfig().getBoolean("checks.reach.enabled", true)) {
                        Entity target = event.getPacket()
                                .getEntityModifier(player.getWorld()).read(0);
                        if (target != null) {
                            double dist = player.getLocation().distance(target.getLocation());
                            double maxDist = getConfig().getDouble("checks.reach.max-distance", 3.5);
                            if (player.isGliding()) maxDist += 1.5;

                            if (dist > maxDist) {
                                event.setCancelled(true);
                                triggerAlert(player, "Reach",
                                        String.format("%.2f > %.2f blok", dist, maxDist));
                            }
                        }
                    }

                    // ── Hitbox açı ────────────────────────────────────────
                    if (getConfig().getBoolean("checks.hitbox.enabled", true)) {
                        Entity target = event.getPacket()
                                .getEntityModifier(player.getWorld()).read(0);
                        if (target != null) {
                            double angle = getAngleBetween(player, target);
                            double maxAngle = getConfig().getDouble("checks.hitbox.max-angle", 115.0);
                            if (angle > maxAngle) {
                                event.setCancelled(true);
                                triggerAlert(player, "Hitbox",
                                        String.format("aci=%.1f derece", angle));
                            }
                        }
                    }

                } catch (Exception e) {
                    // Paket parse hatasi - sessizce gec
                }
            }
        });

        // Rotasyon paketi → KillAura aimbot tespiti
        protocolManager.addPacketListener(new PacketAdapter(
                this, ListenerPriority.NORMAL,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                if (!getConfig().getBoolean("checks.killaura.check-rotation", true)) return;

                Player player = event.getPlayer();
                PlayerData data = playerDataMap.get(player.getUniqueId());
                if (data == null) return;
                if (player.hasPermission("salxaet.bypass") || player.isOp()) return;

                try {
                    float yaw   = event.getPacket().getFloat().read(0);
                    float pitch = event.getPacket().getFloat().read(1);

                    float deltaYaw = Math.abs(yaw - data.lastYaw);

                    // Ani büyük tam sayı dönüş → aimbot işareti
                    if (deltaYaw > 45f && deltaYaw < 180f) {
                        if (yaw % 1.0f == 0.0f && pitch % 1.0f == 0.0f) {
                            triggerAlert(player, "KillAura",
                                    String.format("Rotation snap: yaw=%.1f pitch=%.1f", yaw, pitch));
                        }
                    }

                    data.lastYaw   = yaw;
                    data.lastPitch = pitch;
                } catch (Exception ignored) {}
            }
        });
    }

    private double getAngleBetween(Player player, Entity target) {
        org.bukkit.Location pLoc = player.getEyeLocation();
        org.bukkit.Location tLoc = target.getLocation().add(0, target.getHeight() / 2.0, 0);

        double dx = tLoc.getX() - pLoc.getX();
        double dz = tLoc.getZ() - pLoc.getZ();

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double delta = Math.abs(targetYaw - pLoc.getYaw()) % 360;
        if (delta > 180) delta = 360 - delta;
        return delta;
    }

    public void triggerAlert(Player player, String checkName, String details) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        data.addFlag();
        data.incrementTotalLogs();

        String safeDetails = (details == null || details.isEmpty()) ? "tespit edildi" : details;

        if (getConfig().getBoolean("settings.send-alerts", true)) {
            String msg = PREFIX + ALERT_FORMAT
                    .replace("%player%", player.getName())
                    .replace("%check%", checkName)
                    .replace("%details%", safeDetails);

            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.hasPermission("salxaet.admin")) {
                    op.sendMessage(msg);
                }
            }
        }

        if (getConfig().getBoolean("settings.log-to-console", true)) {
            getLogger().info("[UYARI] " + player.getName()
                    + " -> " + checkName + " (" + safeDetails + ")");
        }

        if (data.getTotalLogs() >= 15) {
            final String reason = KICK_REASON;
            Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(reason));
            data.resetTotalLogs();
        }
    }

    public boolean isAnticheatEnabled() { return isAnticheatEnabled; }
    public void setAnticheatEnabled(boolean val) { isAnticheatEnabled = val; }

    public static String color(String msg) {
        return msg == null ? "" : org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    // ── PlayerData iç sınıfı ─────────────────────────────────────────────────

    public static class PlayerData {
        private final Player player;
        private int violationFlags = 0;
        private int totalLogs = 0;
        private long lastAttackTime = 0L;
        public  long lastFireworkBoost = 0L;
        public  float lastYaw = 0f;
        public  float lastPitch = 0f;
        public  org.bukkit.Location lastLocation = null;
        public  long lastMoveTime = 0L;
        public  int airTicks = 0;

        public PlayerData(Player player) { this.player = player; }

        public Player getPlayer()  { return player; }
        public int  getFlags()     { return violationFlags; }
        public void addFlag()      { violationFlags = Math.min(violationFlags + 1, 100); }
        public void decreaseFlag() { if (violationFlags > 0) violationFlags--; }
        public void resetFlags()   { violationFlags = 0; }

        public int  getTotalLogs()       { return totalLogs; }
        public void incrementTotalLogs() { totalLogs++; }
        public void resetTotalLogs()     { totalLogs = 0; }

        public long getLastAttackTime()        { return lastAttackTime; }
        public void setLastAttackTime(long t)  { lastAttackTime = t; }
    }
}
