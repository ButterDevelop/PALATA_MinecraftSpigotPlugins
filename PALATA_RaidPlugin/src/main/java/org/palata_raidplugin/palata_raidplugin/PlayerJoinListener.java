package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerJoinListener implements Listener {
    private final PALATA_RaidPlugin plugin;

    private Map<String, String> playerNames = new HashMap<>();

    public PlayerJoinListener(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;

        // Получаем список строк из конфигурации
        List<String> playersList = plugin.getConfig().getStringList("players");

        // Обрабатываем каждую строку
        for (String player : playersList) {
            // Разделяем строку по символу ':'
            String[] parts = player.split(":");

            // Если строка корректно разделена на две части, сохраняем их в словарь
            if (parts.length == 2) {
                playerNames.put(parts[0], parts[1]);
            }
        }

        // Теперь словарь playerNames содержит имена игроков и их реальные имена
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Получаем реальное имя из словаря
        String realName = playerNames.get(player.getName());

        if (realName != null) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Здарова, " + realName + ". Рад тебя видеть.");
        } else {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Здарова, " + player.getName() + ". Рад тебя видеть.");
        }

        plugin.getGame().updateScoreboard();
        player.setScoreboard(plugin.getGame().getScoreboard());
    }


}