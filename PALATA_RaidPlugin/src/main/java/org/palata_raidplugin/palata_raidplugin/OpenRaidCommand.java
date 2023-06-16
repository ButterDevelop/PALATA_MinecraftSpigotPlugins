package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenRaidCommand implements CommandExecutor {

    private final Game game;

    public OpenRaidCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (game.isRaidOpen()) {
            player.sendMessage(ChatColor.RED + "Рейд уже открыт для присоединения!");
            return true;
        }

        if (!game.isCaptain(game.getPlayerTeam(player.getName()), player)) {
            player.sendMessage(ChatColor.RED + "Только капитан команды может начать рейд.");
            return true;
        }

        if (game.getNexusLocation(game.getDefendingTeam(game.getPlayerTeam(player.getName()))) == null) {
            player.sendMessage(ChatColor.RED + "У другой команды ещё нет Нексуса!");
            return true;
        }

        game.openRaid(game.getPlayerTeam(player.getName()));
        player.sendMessage(ChatColor.GREEN + "В рейд теперь можно присоединиться.");
        return true;
    }
}
