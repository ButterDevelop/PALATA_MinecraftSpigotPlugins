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
        // Проверка, что команду запускает игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        final Player player = (Player) sender;
        final String team = plugin.getGame().getPlayerTeam(player.getName());

        // Проверка включённости системы рейдов
        if (!plugin.getGame().getIsEnabled()) {
            player.sendMessage(ChatColor.RED + "Система рейдов сейчас выключена.");
            return true;
        }

        // Проверка, открыт ли уже рейд
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

        final long currentTime = System.currentTimeMillis();

        // Проверка времени последнего рейда для атакующей команды
        long lastRaidTimestamp = plugin.getConfig().getLong(team + ".lastRaid", -1L);
        long requiredWaitTime = plugin.getConfig().getInt("plugin.raid.shieldMinutes");
        if (lastRaidTimestamp != -1L && (currentTime - lastRaidTimestamp) / 1000 / 60 < requiredWaitTime) {
            player.sendMessage(ChatColor.RED + "Вы не можете начать новый рейд, пока не прошло " + requiredWaitTime + " минут с последнего.");
            return true;
        }

        // Проверка времени последнего рейда для защищающейся команды
        String defendingTeam = plugin.getGame().getDefendingTeam(team);
        long lastRaidTimestampDefendingTeam = plugin.getConfig().getLong(defendingTeam + ".lastRaid", -1L);
        long requiredWaitTimeDefending = plugin.getConfig().getInt("plugin.raid.shieldMinutesDefending");
        if (lastRaidTimestampDefendingTeam != -1L && (currentTime - lastRaidTimestampDefendingTeam) / 1000 / 60 < requiredWaitTimeDefending) {
            player.sendMessage(ChatColor.RED + "Вы не можете сразу начать рейд после рейда на вас.");
            player.sendMessage(ChatColor.RED + "Требуется небольшой перерыв в размере минут: " + requiredWaitTimeDefending + ".");
            return true;
        }

        // Проверка, является ли игрок капитаном своей команды
        if (!plugin.getGame().isCaptain(team, player)) {
            player.sendMessage(ChatColor.RED + "Только капитан команды может начать рейд.");
            return true;
        }

        // Проверка наличия нексуса у защищающейся команды
        if (plugin.getGame().getNexusLocation(defendingTeam) == null) {
            player.sendMessage(ChatColor.RED + "У другой команды ещё нет Нексуса!");
            return true;
        }

        // Открытие рейда
        plugin.getGame().openRaid(team);
        player.sendMessage(ChatColor.GREEN + "В рейд теперь можно присоединиться.");

        final int openRaidDurationMinutes = plugin.getGame().getOpenRaidDurationMinutes();
        final BukkitScheduler scheduler = Bukkit.getScheduler();

        // Планирование задачи отмены рейда после истечения времени
        scheduler.runTaskLater(plugin, () -> {
            if (plugin.getGame().isRaidOpen() && !plugin.getGame().isRaidActive() && !plugin.getGame().isRaidStarted()) {
                String lastOpenedTeam = plugin.getGame().getLastRaidOpenedTheTeam();
                if (lastOpenedTeam != null && lastOpenedTeam.equals(team)) {
                    plugin.getGame().cancelRaid(team, false);
                }
            }
        }, (long) openRaidDurationMinutes * 60 * 20); // Перевод минут в тики

        return true;
    }
}
