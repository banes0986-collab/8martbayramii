package com.legacy.salxaet;

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
            sender.sendMessage(plugin.PREFIX + plugin.MSG_NO_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.PREFIX + "&7Kullanım: /salxaet <reload|enable|disable>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadMessages();
                sender.sendMessage(plugin.PREFIX + plugin.MSG_RELOADED);
                break;

            case "enable":
                if (plugin.isAnticheatEnabled()) {
                    sender.sendMessage(plugin.PREFIX + plugin.MSG_ALREADY_ACTIVE);
                } else {
                    plugin.setAnticheatEnabled(true);
                    sender.sendMessage(plugin.PREFIX + plugin.MSG_ACTIVATED);
                }
                break;

            case "disable":
                if (!plugin.isAnticheatEnabled()) {
                    sender.sendMessage(plugin.PREFIX + plugin.MSG_ALREADY_DISABLED);
                } else {
                    plugin.setAnticheatEnabled(false);
                    sender.sendMessage(plugin.PREFIX + plugin.MSG_DISABLED);
                }
                break;

            default:
                sender.sendMessage(plugin.PREFIX + "&7Kullanım: /salxaet <reload|enable|disable>");
        }

        return true;
    }
}
