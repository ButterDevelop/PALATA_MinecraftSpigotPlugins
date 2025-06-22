package org.palata_banplayerondeath.palata_banplayerondeath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;

public class PALATA_BanPlayerOnDeath extends JavaPlugin implements Listener {
    private boolean isEnabled;
    private int minutesToBan;
    private long scheduleEnableAt; // timestamp, millis

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        // Загрузка параметров из config.yml
        minutesToBan       = getConfig().getInt("minutesToBan", 30);
        isEnabled          = getConfig().getBoolean("isEnabled", false);
        scheduleEnableAt   = getConfig().getLong("schedule.enableAt", -1L);

        // Если есть отложенное включение и оно ещё в будущем — запланировать
        if (scheduleEnableAt > System.currentTimeMillis()) {
            scheduleEnableTasks(scheduleEnableAt);
        } else if (scheduleEnableAt != -1L) {
            // прошедшее время — убрать
            getConfig().set("schedule.enableAt", null);
            saveConfig();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("deathban")) return false;
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "on":
                case "off":
                case "info":
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("on")) {
                        isEnabled = true;
                        sender.sendMessage(ChatColor.GREEN + "DeathBan включен.");
                    } else if (args[0].equalsIgnoreCase("off")) {
                        isEnabled = false;
                        sender.sendMessage(ChatColor.RED + "DeathBan выключен.");
                    } else { // info
                        sender.sendMessage(isEnabled
                                ? ChatColor.GREEN + "DeathBan в данный момент включен."
                                : ChatColor.RED   + "DeathBan в данный момент выключен.");
                    }
                    getConfig().set("isEnabled", isEnabled);
                    saveConfig();
                    return true;
            }
        }
        // Наша новая команда: /deathban schedule <minutes>
        if (args.length == 2 && args[0].equalsIgnoreCase("schedule")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                return true;
            }
            long mins;
            try {
                mins = Long.parseLong(args[1]);
                if (mins <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Неверное число минут: " + args[1]);
                return true;
            }
            // высчитываем время включения
            scheduleEnableAt = System.currentTimeMillis() + mins * 60 * 1000;
            getConfig().set("schedule.enableAt", scheduleEnableAt);
            saveConfig();

            // сразу планируем все задачи
            scheduleEnableTasks(scheduleEnableAt);

            Bukkit.broadcastMessage(ChatColor.GREEN + "DeathBan будет включен через " + mins + " минут.");
            return true;
        }

        // Команда unban
        if (args.length == 2 && args[0].equalsIgnoreCase("unban")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                return true;
            }

            // Пытаемся получить оффлайн-профиль игрока
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String path = "bannedPlayers." + target.getUniqueId();
            if (!getConfig().contains(path)) {
                sender.sendMessage(ChatColor.YELLOW + "Игрок " + args[1] + " не был забанен.");
                return true;
            }

            // Удаляем из конфига и сохраняем
            getConfig().set(path, null);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Снят бан с игрока " + args[1] + ".");
            return true;
        }

        return false;
    }

    private void scheduleEnableTasks(long enableAt) {
        long now      = System.currentTimeMillis();
        long delayMs  = enableAt - now;
        long ticks    = delayMs / 50; // 1 тик = 50 мс

        // Сообщение за 1 час
        long oneHourBeforeMs = enableAt - 60L * 60 * 1000;
        if (oneHourBeforeMs > now) {
            long ticks1h = (oneHourBeforeMs - now) / 50;
            new BukkitRunnable() {
                @Override public void run() {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "§l[DeathBan] §fВключится через §c1 час§f!");
                }
            }.runTaskLater(this, ticks1h);
        }

        // Сообщение за 30 минут
        long halfHourBeforeMs = enableAt - 30L * 60 * 1000;
        if (halfHourBeforeMs > now) {
            long ticks30m = (halfHourBeforeMs - now) / 50;
            new BukkitRunnable() {
                @Override public void run() {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "§l[DeathBan] §fВключится через §c30 минут§f!");
                }
            }.runTaskLater(this, ticks30m);
        }

        // Собственно включение
        new BukkitRunnable() {
            @Override public void run() {
                isEnabled = true;
                getConfig().set("isEnabled", true);
                getConfig().set("schedule.enableAt", null);
                saveConfig();
                Bukkit.broadcastMessage(ChatColor.GREEN + "§l[DeathBan] §fВключен!");
            }
        }.runTaskLater(this, ticks);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled) return;
        Player player = event.getEntity();
        long banUntil = System.currentTimeMillis() + (long)minutesToBan * 60 * 1000;
        // сохраняем в тот же файл, где у вас хранятся баны
        getConfig().set("bannedPlayers." + player.getUniqueId(), banUntil);
        saveConfig(); // или ваш saveConfigPlayers()

        player.sendMessage(ChatColor.RED + "Вы умерли. Вас выкинет с сервера через 10 секунд.");
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                String msg = ChatColor.RED + "Вы умерли и заблокированы на "
                        + minutesToBan + " минут. Координаты: " + x + " " + y + " " + z;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " " + msg);
            }
        }.runTaskLater(this, 200L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long until = getConfig().getLong("bannedPlayers." + player.getUniqueId(), -1L);
        if (until > System.currentTimeMillis()) {
            double remMins = (until - System.currentTimeMillis()) / 1000.0 / 60.0;
            String msg   = ChatColor.RED + String.format("Вы заблокированы еще на %.1f минут.", remMins);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " " + msg);
        } else if (until != -1L) {
            getConfig().set("bannedPlayers." + player.getUniqueId(), null);
            saveConfig();
        }
    }
}
