package com.balikac.anticheat;

import com.balikac.anticheat.checks.AutoTotemCheck;
import com.balikac.anticheat.checks.ClientBrandCheck;
import com.balikac.anticheat.checks.CrystalAuraCheck;
import com.balikac.anticheat.checks.KillAuraCheck;
import com.balikac.anticheat.checks.XrayCheck;
import com.balikac.anticheat.commands.BalikACCommand;
import com.balikac.anticheat.data.PlayerDataManager;
import com.balikac.anticheat.listeners.PlayerConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BalikAC extends JavaPlugin {

    private static BalikAC instance;
    private PlayerDataManager dataManager;

    private AutoTotemCheck autoTotemCheck;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        dataManager = new PlayerDataManager();

        registerListeners();
        registerCommands();

        getLogger().info("BalikAC (1.21.8) etkinleştirildi. Aktif checkler: KillAura, AutoTotem, CrystalAura, Xray, ClientBrand");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.clear();
        }
        getLogger().info("BalikAC devre dışı bırakıldı.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new KillAuraCheck(this), this);
        getServer().getPluginManager().registerEvents(new CrystalAuraCheck(this), this);
        getServer().getPluginManager().registerEvents(new XrayCheck(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        autoTotemCheck = new AutoTotemCheck(this);
        getServer().getPluginManager().registerEvents(autoTotemCheck, this);
        autoTotemCheck.startMonitoring();

        ClientBrandCheck clientBrandCheck = new ClientBrandCheck(this);
        clientBrandCheck.register();
    }

    private void registerCommands() {
        BalikACCommand commandExecutor = new BalikACCommand(this);
        this.getCommand("balikac").setExecutor(commandExecutor);
    }

    public static BalikAC getInstance() {
        return instance;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }
}
