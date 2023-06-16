package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StartRaidCommand implements CommandExecutor {

    private final Game game;

    public StartRaidCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!game.isCaptain(game.getPlayerTeam(player.getName()), player)) {
            player.sendMessage(ChatColor.RED + "Только капитан команды может начать рейд.");
            return true;
        }

        if (!game.isRaidOpen()) {
            player.sendMessage(ChatColor.RED + "Нет созданного рейда, чтобы его начать.");
            return true;
        }

        if (game.isRaidActive()) {
            player.sendMessage(ChatColor.RED + "Рейд уже активен!");
            return true;
        }

        // Получение текущей даты и времени
        LocalDateTime currentDateTime = LocalDateTime.now();
        // Форматирование даты и времени в строку
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String currentDateTimeString = currentDateTime.format(formatter);

        game.startRaid(game.getPlayerTeam(player.getName()));
        player.sendMessage(ChatColor.GREEN + "Рейд начнется через " + game.getRaidDelayMinutes() + " минут. Сейчас на часах " + currentDateTimeString + ".");
        return true;
    }
}
