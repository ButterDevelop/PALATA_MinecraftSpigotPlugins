package org.palata_banplayerondeath.palata_banplayerondeath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class PALATA_BanPlayerOnDeath extends JavaPlugin implements Listener {
    private boolean isEnabled;
    private File configFile;
    private FileConfiguration config;
    private int _minutesToBan;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();  // Сохраняет конфигурационный файл по умолчанию, если он еще не существует

        Bukkit.getPluginManager().registerEvents(this, this);

        _minutesToBan = getConfig().getInt("minutesToBan");  // Читаем значение banTime из файла config.yml
        isEnabled = getConfig().getBoolean("isEnabled"); // Читаем значение isEnabled из файла config.yml

        configFile = new File(getDataFolder(), "banned_players.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("deathban")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                        return true;
                    }
                    isEnabled = true;
                    getConfig().set("isEnabled", isEnabled);
                    saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "DeathBan включен.");
                    return true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                        return true;
                    }
                    isEnabled = false;
                    getConfig().set("isEnabled", isEnabled);
                    saveConfig();
                    sender.sendMessage(ChatColor.RED + "DeathBan выключен.");
                    return true;
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (isEnabled) {
                        sender.sendMessage(ChatColor.GREEN + "DeathBan в данный момент включен.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "DeathBan в данный момент выключен.");
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isEnabled) {
            Player player = event.getEntity();
            config.set(player.getUniqueId().toString(), System.currentTimeMillis() + ((long) _minutesToBan * 60 * 1000));
            saveConfigPlayers();
            player.sendMessage(ChatColor.RED + "Вы умерли. Вас выкинет с сервера через 10 секунд.");

            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(ChatColor.RED + "Вы умерли и были заблокированы на сервере на " + _minutesToBan + " минут. Ваши координаты смерти: " +
                            x + " " +
                            y + " " +
                            z);
                }
            }.runTaskLater(this, 200L);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long timeRemaining = config.getLong(player.getUniqueId().toString(), -1L);

        if (timeRemaining != -1L && System.currentTimeMillis() < timeRemaining) {
            double timeRemained = ((timeRemaining - System.currentTimeMillis()) / 1000.0 / 60.0);
            String timeStringRounded = String.format("%.1f", timeRemained);
            player.kickPlayer(ChatColor.RED + "Вы заблокированы ещё на " + timeStringRounded + " минут(-ы).");
        } else {
            config.set(player.getUniqueId().toString(), null);
             saveConfigPlayers();
        }
    }

    public void saveConfigPlayers() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
