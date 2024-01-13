package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

public class SoundKillManager implements Listener {
    private final PALATA_RaidPlugin plugin;

    public SoundKillManager(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim   = event.getEntity();
        Player attacker = victim.getKiller();

        if (attacker != null && !attacker.equals(victim)) {
            // Получаем имя атакующего игрока и жертвы
            String victimName   = attacker.getName();
            String attackerName = attacker.getName();

            // Проигрываем звук для жертвы
            String soundName;
            if (plugin.getGame().areTwoPlayersInTheSameTeam(attackerName, victimName)) {
                soundName = "killedby.teammate." + attackerName.toLowerCase();
            } else {
                soundName = "killedby.enemy." + attackerName.toLowerCase();
            }

            // Проигрываем кастомный звук для всех игроков на сервере
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.playSound(victim.getLocation(), soundName, 1.0F, 1.0F);
            }
        }
    }
}
