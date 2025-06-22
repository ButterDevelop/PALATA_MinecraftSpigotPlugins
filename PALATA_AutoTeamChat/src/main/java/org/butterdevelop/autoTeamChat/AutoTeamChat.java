package org.butterdevelop.autoTeamChat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoTeamChat extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Регистрируем листенер
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ChatRedirectPlugin включён");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatRedirectPlugin выключен");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();

        if (msg.startsWith("!")) {
            // Глобальный чат: убираем '!' и оставляем дальше стандартную отправку
            String withoutBang = msg.substring(1);
            event.setMessage(withoutBang);
            // (Опционально) изменить префикс/формат:
            // event.setFormat("<" + player.getDisplayName() + "> " + withoutBang);
        } else {
            // По умолчанию — в командный чат
            event.setCancelled(true);
            // Так как мы в асинхронном событии, переключаемся на главный поток
            Bukkit.getScheduler().runTask(this, () ->
                    player.performCommand("teammsg " + msg)
            );
        }
    }
}
