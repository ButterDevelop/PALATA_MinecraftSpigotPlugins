package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PALATA_RaidPlugin plugin;

    public PlayerJoinListener(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig(); // Получаем конфигурацию плагина
        String realName = config.getString("players." + player.getName()); // Получаем реальное имя игрока из конфигурации
        if (realName != null) { // Если имя не null, используем его для сообщения
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Здарова, " + realName + ". Рад тебя видеть.");
        } else { // Если имя null, используем имя игрока для сообщения
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Здарова, " + player.getName() + ". Рад тебя видеть.");
        }
        plugin.getGame().updateScoreboard();
        player.setScoreboard(plugin.getGame().getScoreboard());
    }


}