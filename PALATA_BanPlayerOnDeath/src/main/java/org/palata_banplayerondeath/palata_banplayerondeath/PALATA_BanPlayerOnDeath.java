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
    private boolean isEnabled = true;
    private File configFile;
    private FileConfiguration config;
    private int _minutesToBan;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        saveDefaultConfig();  // Сохраняет конфигурационный файл по умолчанию, если он еще не существует
        _minutesToBan = getConfig().getInt("minutesToBan");  // Читаем значение banTime из файла config.yml

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
                    isEnabled = true;
                    sender.sendMessage(ChatColor.GREEN + "DeathBan включен.");
                    return true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    isEnabled = false;
                    sender.sendMessage(ChatColor.RED + "DeathBan выключен.");
                    return true;
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) throws InterruptedException {
        if (isEnabled) {
            Player player = event.getEntity();
            config.set(player.getUniqueId().toString(), System.currentTimeMillis() + ((long) _minutesToBan * 60 * 1000));
            saveConfig();
            player.sendMessage(ChatColor.RED + "Вы умерли. Вас выкинет с сервера через 3 секунды.");

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
            }.runTaskLater(this, 60L);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long timeRemaining = config.getLong(player.getUniqueId().toString(), -1L);

        if (timeRemaining != -1L && System.currentTimeMillis() < timeRemaining) {
            player.kickPlayer(ChatColor.RED + "Вы заблокированы ещё на " + ((timeRemaining - System.currentTimeMillis()) / 1000 / 60) + " минут.");
        } else {
            config.set(player.getUniqueId().toString(), null);
            saveConfig();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
