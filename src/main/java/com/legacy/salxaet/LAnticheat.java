package com.legacy.salxaet;

import com.legacy.salxaet.checks.CombatCheck;
import com.legacy.salxaet.checks.KillAuraCheck;
import com.legacy.salxaet.manager.AlertManager;
import com.legacy.salxaet.manager.PlayerDataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class LAnticheat extends JavaPlugin {

    private static LAnticheat instance;

    private boolean anticheatEnabled = true;

    private PlayerDataManager dataManager;
    private AlertManager alertManager;
    private KillAuraCheck killAuraCheck;
    private CombatCheck combatCheck;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        createMessagesFile();
        reloadMessages();

        this.dataManager = new PlayerDataManager();
        this.alertManager = new AlertManager(this);
        this.killAuraCheck = new KillAuraCheck(this);
        this.combatCheck = new CombatCheck(this);

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib bulunamadi! Eklenti calisir ama paket "
                    + "tabanli ek dogrulamalar olmadan, sadece Bukkit event'leri ile calisacak.");
        }

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        var cmd = getCommand("salxaet");
        if (cmd != null) {
            cmd.setExecutor(new AnticheatCommand(this));
        }

        getLogger().info("SalxAET v2.0 aktif! (birlesik KillAura + hareket kontrolleri)");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.clear();
        }
        getLogger().info("SalxAET devre disi birakildi.");
    }

    private void createMessagesFile() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            if (getDataFolder().mkdirs()) {
                getLogger().fine("Veri klasoru olusturuldu.");
            }
            saveResource("messages.yml", false);
        }
    }

    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public static LAnticheat getInstance() {
        return instance;
    }

    public boolean isAnticheatEnabled() {
        return anticheatEnabled;
    }

    public void setAnticheatEnabled(boolean enabled) {
        this.anticheatEnabled = enabled;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public KillAuraCheck getKillAuraCheck() {
        return killAuraCheck;
    }

    public CombatCheck getCombatCheck() {
        return combatCheck;
    }
}
