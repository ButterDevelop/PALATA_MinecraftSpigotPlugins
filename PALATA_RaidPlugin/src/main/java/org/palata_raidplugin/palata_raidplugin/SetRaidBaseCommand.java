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
            player.sendMessage(ChatColor.RED + "Только капитан может установить Нексус.");
            return true;
        }

        if (!player.getWorld().getName().equals("world")) {
            player.sendMessage(ChatColor.RED + "Базу можно установить только в обычном мире.");
            return true;
        }

        if (plugin.getGame().isRaidActive()) {
            player.sendMessage(ChatColor.RED + "Нельзя сменить базу во время рейда.");
            return true;
        }

        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить Нексус! Вы сейчас на территории чужой базы для рейда.");
            return true;
        }

        if (plugin.getGame().isWithinHomeRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить Нексус! Вы сейчас на территории чужого дома.");
            return true;
        }

        // Чтение времени последней команды /setraidbase из файла конфигурации
        long lastSetRaidBaseMillis = plugin.getConfig().getLong(team + ".setraidbase", -1L);
        // Проверка, прошло ли достаточно времени с последней команды
        long requiredWaitTime = plugin.getConfig().getInt("plugin.raid.setRaidBaseCooldown");
        if (lastSetRaidBaseMillis != -1L && (System.currentTimeMillis() - lastSetRaidBaseMillis) / 1000 / 60 < requiredWaitTime) {
            // Если не прошло достаточно времени, показываем сообщение
            player.sendMessage(ChatColor.RED + "Подождите несколько минут перед использованием данной команды, а именно: " + requiredWaitTime + ".");
            return true;
        }

        Location playerLocation = player.getLocation();

        Location nexusLocationFirst = plugin.getGame().getNexusLocation(team);

        // Записываем координаты игрока в файл конфигурации для указанной команды
        plugin.getConfig().set(team + ".nexus.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + ".nexus.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + ".nexus.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + ".nexus.world", playerLocation.getWorld().getName());
        plugin.saveConfig();

        plugin.getGame().loadBases();

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
            if (nexusLocationFirst != null) {
                plugin.getGame().removeFullNexus(nexusLocationFirst);
            }
            plugin.getGame().buildFullNexus(plugin.getGame().getPlayerTeam(player.getName()));

            player.sendMessage(ChatColor.GREEN + "Местоположение базы для рейда вашей команды '" + team + "' было установлено как Ваша текущая локация.");
            player.sendMessage(ChatColor.GREEN + "Её координаты: " + playerLocation.getBlockX() + " " + (playerLocation.getBlockY() + 1) + " " + playerLocation.getBlockZ());
            player.sendMessage(ChatColor.GREEN + "Нексус был расположен.");

            plugin.getConfig().set(team + ".setraidbase", System.currentTimeMillis());
            plugin.saveConfig();

            // Перемещаем игрока, если нужно
            plugin.getGame().teleportPlayerToSafePosition(player);
        } else {
            if (nexusLocationFirst != null) {
                plugin.getConfig().set(team + ".nexus.x", nexusLocationFirst.getBlockX());
                plugin.getConfig().set(team + ".nexus.y", nexusLocationFirst.getBlockY());
                plugin.getConfig().set(team + ".nexus.z", nexusLocationFirst.getBlockZ());
                plugin.getConfig().set(team + ".nexus.world", nexusLocationFirst.getWorld().getName());
                plugin.saveConfig();
            }

            player.sendMessage(ChatColor.RED + "Невозможно разместить Нексус. Проверьте место для размещения.");
        }

        return true;
    }

}
