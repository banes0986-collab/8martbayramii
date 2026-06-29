package com.legacy.salxaet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
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

    // Mesaj dosyası
    private File messagesFile;
    public FileConfiguration messagesConfig;

    // Prefix & mesajlar (config'den yüklenir)
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

        // ProtocolLib bağlantısı
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketChecks();
        } else {
            getLogger().severe("ProtocolLib bulunamadi! Paket tabanli kontroller devre disi.");
        }

        // Event listeners
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Komut
        getCommand("salxaet").setExecutor(new AnticheatCommand(this));

        // Violation azaltma görevi (her 10 saniyede bir)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (PlayerData data : playerDataMap.values()) {
                data.decreaseFlag();
            }
        }, 200L, 200L);

        getLogger().info("========================================");
        getLogger().info(" SalxAET v2.0 - Aktif!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        playerDataMap.clear();
    }

    // ─── Mesaj dosyası yönetimi ───────────────────────────────────────────────

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
        PREFIX        = color(cfg.getString("prefix", "&8[&dSalxAET&8] "));
        ALERT_FORMAT  = color(cfg.getString("alert-format",
                "&f%player% &7adlı oyuncu &e%check% &7şüphesi fırlattı! &8(%details%)"));
        MSG_NO_PERMISSION   = color(cfg.getString("commands.no-permission",   "&cYetkiniz yok!"));
        MSG_ALREADY_ACTIVE  = color(cfg.getString("commands.already-active",  "&eSalxAET zaten aktif!"));
        MSG_ACTIVATED       = color(cfg.getString("commands.activated",       "&aHile koruması devreye alındı!"));
        MSG_ALREADY_DISABLED= color(cfg.getString("commands.already-disabled","&eSalxAET zaten devre dışı!"));
        MSG_DISABLED        = color(cfg.getString("commands.disabled",        "&cHile koruması kapatıldı!"));
        MSG_RELOADED        = color(cfg.getString("commands.reloaded",        "&aAyarlar yenilendi!"));
        KICK_REASON         = color(cfg.getString("kick.reason",
                "&c&l[SalxAET]\n\n&7Sunucudan uzaklaştırıldınız!\n&bSebep: &eHile Kullanımı"));
    }

    // ─── ProtocolLib paket dinleyicileri ─────────────────────────────────────

    private void registerPacketChecks() {

        // --- Paket 1: Elytra boost (FIREWORK_ROCKET_SHOOT) ─────────────────
        protocolManager.addPacketListener(new PacketAdapter(
                this, ListenerPriority.NORMAL,
                PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                Player player = event.getPlayer();
                PlayerData data = playerDataMap.get(player.getUniqueId());
                if (data == null) return;

                try {
                    org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == org.bukkit.Material.FIREWORK_ROCKET) {
                        data.lastFireworkBoost = System.currentTimeMillis();
                    }
                } catch (Exception ignored) {}
            }
        });

        // --- Paket 2: Saldırı (USE_ENTITY) → KillAura CPS + Reach ─────────
        protocolManager.addPacketListener(new PacketAdapter(
                this, ListenerPriority.NORMAL,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!isAnticheatEnabled) return;
                Player player = event.getPlayer();
                PlayerData data = playerDataMap.get(player.getUniqueId());
                if (data == null) return;

                try {
                    // Sadece ATTACK paketlerini işle
                    EnumWrappers.EntityUseAction action = event.getPacket()
                            .getEnumModifier(EnumWrappers.EntityUseAction.class, 1).read(0);
                    if (action != EnumWrappers.EntityUseAction.ATTACK) return;

                    // ── KillAura CPS kontrolü ────────────────────────────
                    if (getConfig().getBoolean("checks.killaura.enabled", true)) {
                        long now = System.currentTimeMillis();
                        long delay = now - data.getLastAttackTime();
                        int minDelay = getConfig().getInt("checks.killaura.min-ms-delay", 40);

                        if (delay < minDelay) {
                            triggerAlert(player, "KillAura (CPS)", "delay=" + delay + "ms");
                        }
                        data.setLastAttackTime(now);
                    }

                    // ── Reach kontrolü ───────────────────────────────────
                    if (getConfig().getBoolean("checks.reach.enabled", true)) {
                        Entity target = event.getPacket()
                                .getEntityModifier(player.getWorld()).read(0);
                        if (target != null) {
                            double dist = player.getLocation().distance(target.getLocation());
                            double maxDist = getConfig().getDouble("checks.reach.max-distance", 3.5);
                            // Elytra ile uçanlar için tolerans
                            if (player.isGliding()) maxDist += 1.5;

                            if (dist > maxDist) {
                                event.setCancelled(true);
                                triggerAlert(player, "Reach",
                                        String.format("%.2f > %.2f blok", dist, maxDist));
                            }
                        }
                    }

                    // ── Hitbox açı kontrolü ──────────────────────────────
                    if (getConfig().getBoolean("checks.hitbox.enabled", true)) {
                        Entity target = event.getPacket()
                                .getEntityModifier(player.getWorld()).read(0);
                        if (target != null) {
                            double angle = getAngleBetween(player, target);
                            double maxAngle = getConfig().getDouble("checks.hitbox.max-angle", 115.0);
                            if (angle > maxAngle) {
                                event.setCancelled(true);
                                triggerAlert(player, "Hitbox",
                                        String.format("açı=%.1f°", angle));
                            }
                        }
                    }

                } catch (Exception ignored) {}
            }
        });

        // --- Paket 3: Rotasyon (LOOK / POSITION_LOOK) → KillAura Rotation ──
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

                try {
                    float yaw   = event.getPacket().getFloat().read(0);
                    float pitch = event.getPacket().getFloat().read(1);

                    float deltaYaw   = Math.abs(yaw   - data.lastYaw);
                    float deltaPitch = Math.abs(pitch - data.lastPitch);

                    // Ani tam sayı dönüşü → bot/aimbot işareti
                    if (deltaYaw > 0.01f && deltaYaw < 180f) {
                        boolean yawExact   = (yaw   % 1.0f == 0.0f);
                        boolean pitchExact = (pitch % 1.0f == 0.0f);
                        if (yawExact && pitchExact && deltaYaw > 45f) {
                            triggerAlert(player, "KillAura (Rotation)",
                                    String.format("Δyaw=%.1f Δpitch=%.1f", deltaYaw, deltaPitch));
                        }
                    }

                    data.lastYaw   = yaw;
                    data.lastPitch = pitch;
                } catch (Exception ignored) {}
            }
        });
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    /**
     * İki entity arasındaki yatay görüş açısını hesaplar.
     */
    private double getAngleBetween(Player player, Entity target) {
        org.bukkit.Location playerLoc = player.getEyeLocation();
        org.bukkit.Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

        double dx = targetLoc.getX() - playerLoc.getX();
        double dz = targetLoc.getZ() - playerLoc.getZ();

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = playerLoc.getYaw();

        double delta = Math.abs(targetYaw - playerYaw) % 360;
        if (delta > 180) delta = 360 - delta;
        return delta;
    }

    /**
     * Uyarı gönderir ve ihlal sayısını artırır.
     * Belirli eşiğe ulaşırsa oyuncuyu atar.
     */
    public void triggerAlert(Player player, String checkName, String details) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        data.addFlag();
        data.incrementTotalLogs();

        if (getConfig().getBoolean("settings.send-alerts", true)) {
            String msg = PREFIX + ALERT_FORMAT
                    .replace("%player%", player.getName())
                    .replace("%check%", checkName)
                    .replace("%details%", details);

            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.hasPermission("salxaet.admin")) {
                    op.sendMessage(msg);
                }
            }
        }

        if (getConfig().getBoolean("settings.log-to-console", true)) {
            getLogger().info("[UYARI] " + player.getName() + " -> " + checkName + " (" + details + ")");
        }

        // 15 toplam ihlalde at
        if (data.getTotalLogs() >= 15) {
            final String reason = KICK_REASON;
            Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(reason));
            data.resetTotalLogs();
        }
    }

    // ─── Getter / setter ─────────────────────────────────────────────────────

    public boolean isAnticheatEnabled() { return isAnticheatEnabled; }
    public void setAnticheatEnabled(boolean val) { isAnticheatEnabled = val; }

    public static String color(String msg) {
        return msg == null ? "" : org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    // ─── İç sınıf: PlayerData ────────────────────────────────────────────────

    public static class PlayerData {
        private final Player player;
        private int violationFlags = 0;
        private int totalLogs      = 0;
        private long lastAttackTime  = 0L;
        public  long lastFireworkBoost = 0L;
        public  float lastYaw   = 0f;
        public  float lastPitch = 0f;

        // Hareket kontrolü için
        public org.bukkit.Location lastLocation = null;
        public long lastMoveTime = 0L;
        public int airTicks = 0;

        public PlayerData(Player player) { this.player = player; }

        public Player getPlayer()  { return player; }
        public int  getFlags()     { return violationFlags; }
        public void addFlag()      { violationFlags = Math.min(violationFlags + 1, 100); }
        public void decreaseFlag() { if (violationFlags > 0) violationFlags--; }
        public void resetFlags()   { violationFlags = 0; }

        public int  getTotalLogs()      { return totalLogs; }
        public void incrementTotalLogs(){ totalLogs++; }
        public void resetTotalLogs()    { totalLogs = 0; }

        public long getLastAttackTime()       { return lastAttackTime; }
        public void setLastAttackTime(long t) { lastAttackTime = t; }
    }
}
