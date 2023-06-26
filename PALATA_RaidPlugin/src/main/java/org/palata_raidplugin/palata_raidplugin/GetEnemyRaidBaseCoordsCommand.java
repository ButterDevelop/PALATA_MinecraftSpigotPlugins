package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetEnemyRaidBaseCoordsCommand implements CommandExecutor {
    private Game game;
    public GetEnemyRaidBaseCoordsCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        Player player = (Player) sender;

        String defendingTeam = game.getDefendingTeam(game.getPlayerTeam(player.getName()));
        Location defendingTeamNexus = game.getNexusLocation(defendingTeam);
        if (defendingTeamNexus == null) {
            player.sendMessage(ChatColor.RED + "У вражеской команды пока что нет Нексуса.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Координаты вражеского Нексуса: x = " +
                defendingTeamNexus.getBlockX() + ", y = " +
                defendingTeamNexus.getBlockY() + ", z = " +
                defendingTeamNexus.getBlockZ() + ".");

        return true;
    }

}
