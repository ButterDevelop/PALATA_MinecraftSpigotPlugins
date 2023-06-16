package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinRaidCommand implements CommandExecutor {

    private final Game game;

    public JoinRaidCommand(Game game) {
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
            player.sendMessage(ChatColor.RED + "В данный момент нет рейда, к которому можно было бы присоединиться.");
            return true;
        }

        game.addPlayerToRaid(player);
        player.sendMessage(ChatColor.GREEN + "Вы успешно присоединились к рейду.");
        return true;
    }
}
