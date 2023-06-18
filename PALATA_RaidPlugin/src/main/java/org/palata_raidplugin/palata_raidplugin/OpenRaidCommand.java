package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

public class OpenRaidCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public OpenRaidCommand(PALATA_RaidPlugin plugin) {
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

        if (!plugin.getGame().getIsEnabled()) {
            player.sendMessage(ChatColor.RED + "Система рейдов сейчас выключена.");
            return true;
        }

        if (plugin.getGame().isRaidOpen()) {
            if (plugin.getGame().getLastRaidOpenedTheTeam().equals(team)) {
                player.sendMessage(ChatColor.RED + "Рейд уже открыт для присоединения!");
            } else {
                int duration = plugin.getGame().getOpenRaidDurationMinutes();
                player.sendMessage(ChatColor.RED + "Рейд уже открыт другой командой! Они либо его начнут, либо он отменится!");
                player.sendMessage(ChatColor.YELLOW + "Рейд сам отменяется через " + duration + " минут.");
            }
            return true;
        }

        // Чтение времени последнего рейда из файла конфигурации
        long lastRaidTimestamp = plugin.getConfig().getLong(team + ".lastRaid", -1L);

        // Проверка, прошло ли достаточно времени с последнего рейда
        long requiredWaitTime = plugin.getConfig().getInt("plugin.raid.shieldMinutes");
        if (lastRaidTimestamp != -1L && (System.currentTimeMillis() - lastRaidTimestamp) / 1000 / 60 < requiredWaitTime) {
            // Если не прошло достаточно времени с последнего рейда, показываем сообщение
            player.sendMessage(ChatColor.RED + "Вы не можете начать новый рейд, пока не прошло " + requiredWaitTime + " минут с последнего.");
            return true;
        }

        long lastRaidTimestampDefendingTeam = plugin.getConfig().getLong(plugin.getGame().getDefendingTeam(team) + ".lastRaid", -1L);
        long requiredWaitTimeDefendingTeam = plugin.getConfig().getInt("plugin.raid.shieldMinutesDefending");
        if (lastRaidTimestampDefendingTeam != -1L && (System.currentTimeMillis() - lastRaidTimestampDefendingTeam) / 1000 / 60 < requiredWaitTimeDefendingTeam) {
            // Если не прошло достаточно времени с последнего рейда, показываем сообщение
            player.sendMessage(ChatColor.RED + "Вы не можете сразу начать рейд после рейда на вас.");
            player.sendMessage(ChatColor.RED + "Требуется небольшой перерыв в размере минут: " + requiredWaitTimeDefendingTeam + ".");
            return true;
        }

        if (!plugin.getGame().isCaptain(plugin.getGame().getPlayerTeam(player.getName()), player)) {
            player.sendMessage(ChatColor.RED + "Только капитан команды может начать рейд.");
            return true;
        }

        if (plugin.getGame().getNexusLocation(plugin.getGame().getDefendingTeam(team)) == null) {
            player.sendMessage(ChatColor.RED + "У другой команды ещё нет Нексуса!");
            return true;
        }

        plugin.getGame().openRaid(team);
        player.sendMessage(ChatColor.GREEN + "В рейд теперь можно присоединиться.");

        int openRaidDurationMinutes = plugin.getGame().getOpenRaidDurationMinutes();

        // Запустите асинхронную задачу через указанное количество минут
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            // Проверяем, не начала ли рейд другая команда, и эта команда всё ещё наша
            if (plugin.getGame().isRaidOpen() && !plugin.getGame().isRaidActive() && !plugin.getGame().isRaidStarted()) {
                if (plugin.getGame().getLastRaidOpenedTheTeam() != null && plugin.getGame().getLastRaidOpenedTheTeam().equals(team)) {
                    plugin.getGame().cancelRaid(team, false);
                }
            }
        }, (long) openRaidDurationMinutes * 60 * 20); // Конвертируйте минуты в тики (20 тиков = 1 секунда)

        return true;
    }
}
