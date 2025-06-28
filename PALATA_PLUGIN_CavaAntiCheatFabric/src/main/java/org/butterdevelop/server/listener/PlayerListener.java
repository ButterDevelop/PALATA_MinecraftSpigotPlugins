package org.butterdevelop.server.listener;

import org.bukkit.ChatColor;
import org.butterdevelop.server.Config;
import org.butterdevelop.server.ServerAntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ConcurrentModificationException;
import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final ServerAntiCheat instance;
    private final Config config;

    public PlayerListener(ServerAntiCheat instance, Config config) {
        this.instance = instance;
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var kickMessage = "Unable to verify the presence of the client anti cheat";

        // Добавление игрока в набор для проверки
        instance.getPlayerSet().add(player);

        // Запуск отложенной задачи для проверки присутствия античита
        long delayTicks = config.getConfig().getLong("delay-before-kick") * 20L;
        Bukkit.getScheduler().runTaskLater(instance, () -> {
            try {
                if (instance.getPlayerSet().remove(player)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " " + kickMessage);
                    instance.log(Level.WARNING, player.getName() + " kicked for: " + kickMessage);
                }
            } catch (ConcurrentModificationException e) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " " + kickMessage);
                instance.log(Level.WARNING, player.getName() + " kicked for: " + kickMessage);
            }
        }, delayTicks);
    }
}
