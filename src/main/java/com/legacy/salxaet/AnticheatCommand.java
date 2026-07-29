package com.legacy.salxaet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AnticheatCommand implements CommandExecutor {

    private final LAnticheat plugin;

    public AnticheatCommand(LAnticheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("salxaet.admin")) {
            sendMsg(sender, "commands.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendMsg(sender, "commands.unknown-command");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.reloadMessages();
                sendMsg(sender, "commands.reloaded");
            }
            case "enable" -> {
                if (plugin.isAnticheatEnabled()) {
                    sendMsg(sender, "commands.already-active");
                } else {
                    plugin.setAnticheatEnabled(true);
                    sendMsg(sender, "commands.activated");
                }
            }
            case "disable" -> {
                if (!plugin.isAnticheatEnabled()) {
                    sendMsg(sender, "commands.already-disabled");
                } else {
                    plugin.setAnticheatEnabled(false);
                    sendMsg(sender, "commands.disabled");
                }
            }
            case "debug" -> {
                boolean newState = !plugin.getConfig().getBoolean("settings.debug", false);
                plugin.getConfig().set("settings.debug", newState);
                plugin.saveConfig();
                sendMsg(sender, newState ? "commands.debug-on" : "commands.debug-off");
            }
            case "status" -> {
                String template = plugin.getMessagesConfig().getString("commands.status", "&7Durum: %status%");
                String msg = template
                        .replace("%status%", plugin.isAnticheatEnabled() ? "&aAktif" : "&cDevre disi")
                        .replace("%debug%", plugin.getConfig().getBoolean("settings.debug", false) ? "&aAcik" : "&7Kapali");
                sender.sendMessage(prefix() + ChatColor.translateAlternateColorCodes('&', msg));
            }
            default -> sendMsg(sender, "commands.unknown-command");
        }
        return true;
    }

    private void sendMsg(CommandSender sender, String path) {
        String raw = plugin.getMessagesConfig().getString(path, "");
        sender.sendMessage(prefix() + ChatColor.translateAlternateColorCodes('&', raw));
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("prefix", "&8[&dSalxAET&8] "));
    }
}
