package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelRaidCommand implements CommandExecutor {

    private final Game game;
    public CancelRaidCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!game.isRaidOpen()) {
            player.sendMessage(ChatColor.RED + "Рейд ещё не открыт!");
            return true;
        }

        if (!game.isCaptain(game.getPlayerTeam(player.getName()), player)) {
            player.sendMessage(ChatColor.RED + "Только капитан команды может отменить рейд.");
            return true;
        }

        if (game.isRaidStarted()) {
            player.sendMessage(ChatColor.RED + "Рейд скоро начнётся, невозможно его отменить!");
            return true;
        }

        if (game.isRaidActive()) {
            player.sendMessage(ChatColor.RED + "Рейд уже начался, невозможно его отменить!");
            return true;
        }

        game.cancelRaid(game.getPlayerTeam(player.getName()), true);
        player.sendMessage(ChatColor.GREEN + "Рейд был успешно отменён.");
        return true;
    }
}
