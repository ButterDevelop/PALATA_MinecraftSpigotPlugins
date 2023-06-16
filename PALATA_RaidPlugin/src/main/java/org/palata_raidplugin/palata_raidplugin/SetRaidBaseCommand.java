package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetRaidBaseCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public SetRaidBaseCommand(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        String team = plugin.getGame().getPlayerTeam(playerName);

        // Проверяем, является ли игрок капитаном
        String playerTeam = plugin.getGame().getPlayerTeam(playerName);
        if (playerTeam == null || !playerTeam.equals(team)) {
            player.sendMessage(ChatColor.RED + "Только капитан может установить центр базы и Нексус.");
            return true;
        }

        Location playerLocation = player.getLocation();

        // Записываем координаты игрока в файл конфигурации для указанной команды
        plugin.getConfig().set(team + ".raidbase.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + ".raidbase.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + ".raidbase.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + ".raidbase.world", playerLocation.getWorld().getName());
        plugin.getConfig().set(team + ".nexus.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + ".nexus.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + ".nexus.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + ".nexus.world", playerLocation.getWorld().getName());
        plugin.saveConfig();

        plugin.getGame().loadBases();

        player.sendMessage(ChatColor.GREEN + "Местоположение базы для рейда вашей команды '" + team + "' было установлено как Ваша текущая локация.");
        player.sendMessage(ChatColor.GREEN + "Её координаты: " + playerLocation.getBlockX() + " " + (playerLocation.getBlockY() + 1) + " " + playerLocation.getBlockZ());

        // Размещаем структуру нексуса
        World world = playerLocation.getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)

        Location nexusLocation = plugin.getGame().getNexusLocation(team);
        boolean canPlaceNexus = true;

        // Проверяем, возможно ли разместить структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(nexusLocation.getBlockX() + x, nexusLocation.getBlockY() + y, nexusLocation.getBlockZ() + z);
                    // Проверяем остальные блоки базы (воздух)
                    if (block.getType() != Material.AIR) {
                        canPlaceNexus = false;
                        break;
                    }
                }
            }
        }

        if (canPlaceNexus) {
            plugin.getGame().buildFullNexus(plugin.getGame().getPlayerTeam(player.getName()));

            player.sendMessage(ChatColor.GREEN + "Нексус был расположен.");
        } else {
            player.sendMessage(ChatColor.RED + "Невозможно разместить Нексус. Проверьте место для размещения.");
        }

        // Перемещаем игрока, если нужно
        Location safeLocation = plugin.getGame().getSafeLocation(playerLocation);
        if (safeLocation != null) {
            player.teleport(safeLocation);
            player.sendMessage(ChatColor.YELLOW + "Вас переместили в безопасное место для постройки Нексуса.");
        }

        return true;
    }

}
