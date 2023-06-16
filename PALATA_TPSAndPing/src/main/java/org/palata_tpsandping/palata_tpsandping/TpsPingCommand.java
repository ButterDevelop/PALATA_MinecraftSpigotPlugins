package org.palata_tpsandping.palata_tpsandping;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpsPingCommand implements CommandExecutor {

    private final PALATA_TPSAndPing plugin;

    public TpsPingCommand(PALATA_TPSAndPing plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("ping")) {
            Player player = (Player) sender;
            int ping = player.getPing();
            player.sendMessage(ChatColor.GREEN + "Ваш пинг: " + ping);

            return true;
        }
        return false;
    }

}

