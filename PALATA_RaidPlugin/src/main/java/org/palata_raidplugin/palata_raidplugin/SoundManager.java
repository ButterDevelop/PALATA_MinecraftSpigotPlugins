package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SoundManager implements Listener {
    private final PALATA_RaidPlugin plugin;

    public SoundManager(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player attacker = victim.getKiller();

        // 1) При смерти любого игрока проигрываем ему звук "dota.death"
        victim.playSound(victim.getLocation(), "dota.death", 1.0F, 1.0F);

        // Получаем команду жертвы
        String victimTeam = plugin.getGame().getPlayerTeam(victim.getName());
        // 3) Команда жертвы слышит звук "dota.enemy_kills_player"
        if (victimTeam != null) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                if (plugin.getGame().areTwoPlayersInTheSameTeam(player.getName(), victim.getName())) {
                    player.playSound(player.getLocation(), "dota.enemy_kills_player", 1.0F, 1.0F);
                }
            }
        }

        if (attacker != null) {
            // Получаем команду атакующего
            String attackerTeam = plugin.getGame().getPlayerTeam(attacker.getName());
            if (!attacker.equals(victim) && !attackerTeam.equals(victimTeam)) {
                // 3) Команда убийцы слышит звук "dota.team_kills_player"
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    if (plugin.getGame().areTwoPlayersInTheSameTeam(player.getName(), attacker.getName())) {
                        player.playSound(player.getLocation(), "dota.team_kills_player", 1.0F, 1.0F);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // 2) При возрождении любого игрока проигрываем звук "dota.respawn"
        player.playSound(player.getLocation(), "dota.respawn", 1.0F, 1.0F);
    }
}
