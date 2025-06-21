package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrivateCheck implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public PrivateCheck(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        String team = plugin.getGame().getPlayerTeam(player.getName());
        Location playerLocation = player.getLocation();

        // Если игрок на территории своей базы для рейда
        if (plugin.getGame().isWithinNexusRadius(playerLocation, team)) {
            player.sendMessage(ChatColor.BLUE + "Вы сейчас на территории своей базы для рейда.");
            return true;
        }

        // Если человек на территории своего дома
        if (plugin.getGame().isWithinHomeRadius(playerLocation, team)) {
            player.sendMessage(ChatColor.BLUE + "Вы сейчас на территории своего дома.");
            return true;
        }

        // Если человек на территории своего дома
        if (plugin.getGame().isWithinFarmRadius(playerLocation, team)) {
            player.sendMessage(ChatColor.BLUE + "Вы сейчас на территории своей фермы.");
            return true;
        }

        // Если игрок сейчас на территории чужой базы для рейда
        if (plugin.getGame().isWithinNexusRadius(playerLocation, plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.YELLOW + "Вы сейчас на территории чужой базы для рейда.");
            return true;
        }

        // Если игрок на территории чужого дома
        if (plugin.getGame().isWithinHomeRadius(playerLocation, plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.YELLOW + "Вы сейчас на территории чужого дома.");
            return true;
        }

        // Если игрок на территории чужой фермы
        if (plugin.getGame().isWithinFarmRadius(playerLocation, plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.YELLOW + "Вы сейчас на территории чужой фермы.");
            return true;
        }

        // Нет привата
        player.sendMessage(ChatColor.GREEN + "Вы сейчас не находитесь ни на чьей территории.");

        return true;
    }

}
