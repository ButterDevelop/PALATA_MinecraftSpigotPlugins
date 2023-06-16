package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final Game game;

    public BlockBreakListener(Game game) {
        this.game = game;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!game.isRaidActive()) {
            return;
        }

        Player player = event.getPlayer();

        if (event.getBlock().getType() == Material.OBSIDIAN && game.isBlockInNexus(event.getBlock().getLocation(), game.getDefendingTeam(game.raidingTeam))) {
            if (player != null) {
                if (!game.raidPlayers.contains(player)) {
                    player.sendMessage(ChatColor.RED + "Вы не участвуете в рейде! Вы не можете ломать Нексус!");
                    event.setCancelled(true);
                    return;
                }
            }
            event.setDropItems(false);
            game.incrementObsidianDestroyed();

            int remainingObsidian = game.getRequiredDestroyCount() - game.getObsidianDestroyed();

            if (remainingObsidian <= 0) {
                game.endRaid();
                if (player != null) {
                    player.sendMessage(ChatColor.GREEN + "Рейд закончен. Вы разрушили весь Нексус!");
                    // Перемещаем игрока, если нужно
                    Location safeLocation = game.getSafeLocation(player.getLocation());
                    if (safeLocation != null) {
                        player.teleport(safeLocation);
                        player.sendMessage(ChatColor.YELLOW + "Вас переместили в безопасное место после окончания рейда.");
                    }
                }
            } else {
                if (player != null) player.sendMessage(ChatColor.YELLOW + "Вы разрушили часть Нексуса! " + remainingObsidian + " осталось.");
            }
        }
    }
}

