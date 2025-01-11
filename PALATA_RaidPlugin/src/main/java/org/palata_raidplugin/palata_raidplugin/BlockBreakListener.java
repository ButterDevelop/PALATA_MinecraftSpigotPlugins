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
        // Если рейд не активен, выходим
        if (!game.isRaidActive()) {
            return;
        }

        final Player player = event.getPlayer();
        final Material blockType = event.getBlock().getType();
        final Location blockLocation = event.getBlock().getLocation();
        final String defendingTeam = game.getDefendingTeam(game.raidingTeam);

        // Проверяем, является ли блок обсидианом и находится ли он в нексусе команды-защитника
        if (blockType != Material.OBSIDIAN || !game.isBlockInNexus(blockLocation, defendingTeam)) {
            return;
        }

        // Если игрок не участвует в рейде, отменяем разрушение
        if (!game.raidPlayers.contains(player)) {
            player.sendMessage(ChatColor.RED + "Вы не участвуете в рейде! Вы не можете ломать Нексус!");
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        game.incrementObsidianDestroyed();

        final int remainingObsidian = game.getRequiredDestroyCount() - game.getObsidianDestroyed();

        if (remainingObsidian <= 0) {
            game.endRaid();
            player.sendMessage(ChatColor.GREEN + "Рейд закончен. Вы разрушили весь Нексус!");

            // Перемещение игрока в безопасное место после окончания рейда
            final Location safeLocation = game.getSafeLocation(player.getLocation());
            if (safeLocation != null) {
                player.teleport(safeLocation);
                player.sendMessage(ChatColor.YELLOW + "Вас переместили в безопасное место после окончания рейда.");
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "Вы разрушили часть Нексуса! " + remainingObsidian + " осталось.");
        }
    }
}
