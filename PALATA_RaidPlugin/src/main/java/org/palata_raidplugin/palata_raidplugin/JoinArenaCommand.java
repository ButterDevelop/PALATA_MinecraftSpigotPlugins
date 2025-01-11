package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JoinArenaCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;
    private final ArenaManager arenaManager;

    public JoinArenaCommand(PALATA_RaidPlugin plugin, ArenaManager arenaManager) {
        this.plugin       = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }

        Player player = (Player) sender;
        long currentTime = System.currentTimeMillis();
        long nextArenaTime = arenaManager.getNextArenaTime();

        // Проверяем, началась ли уже арена
        if (arenaManager.isArenaActive()) {
            player.sendMessage(ChatColor.RED + "Арена уже началась!");
            return true;
        }

        // Если игрок не состоит в команде, то он не может участвовать
        if (plugin.getGame().getPlayerTeam(player.getName()) == null) {
            player.sendMessage(ChatColor.RED + "Участвовать в арене могут только игроки команд!");
            return true;
        }

        // Может быть человек уже присоединился ранее
        if (arenaManager.isPlayerAlreadyInArena(player)) {
            player.sendMessage(ChatColor.GOLD + "Вы уже присоединились к арене! Ждите начала!");
            return true;
        }

        // Проверяем, можно ли присоединиться к арене
        if (nextArenaTime - currentTime <= 600000) { // 600000 мс = 10 минут
            arenaManager.addPlayerToArena(player);
            player.sendMessage(ChatColor.GREEN + "Вы присоединились к арене!");
        } else {
            // Форматирование даты и времени начала арены
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String formattedDate = dateFormat.format(new Date(nextArenaTime));
            player.sendMessage(ChatColor.RED + "Следующая арена начнется в " + formattedDate + ".");
        }

        return true;
    }
}