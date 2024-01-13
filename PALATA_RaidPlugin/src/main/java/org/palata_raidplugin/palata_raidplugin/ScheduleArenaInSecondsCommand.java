package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduleArenaInSecondsCommand implements CommandExecutor {

    private final ArenaManager arenaManager;

    public ScheduleArenaInSecondsCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
            return true;
        }

        Player player = (Player) sender;

        // Проверяем, началась ли уже арена
        if (arenaManager.isArenaActive()) {
            player.sendMessage(ChatColor.RED + "Арена уже началась!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Укажите количество секунд до начала арены.");
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Укажите правильное целое число секунд.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long nextArenaTime = currentTime + (seconds * 1000L); // Переводим секунды в миллисекунды

        // Запланировать арену
        arenaManager.scheduleNextArena(nextArenaTime);
        arenaManager.checkAndScheduleArena();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String formattedTime = dateFormat.format(new Date(nextArenaTime));

        player.sendMessage(ChatColor.GREEN + "Арена запланирована на " + formattedTime + ".");

        return true;
    }
}
