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

public class SetHomeBaseCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public SetHomeBaseCommand(PALATA_RaidPlugin plugin) {
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
            player.sendMessage(ChatColor.RED + "Только капитан может установить центр дома.");
            return true;
        }

        if (!player.getWorld().getName().equals("world")) {
            player.sendMessage(ChatColor.RED + "Дом можно установить только в обычном мире.");
            return true;
        }

        // Чтение времени последней команды /sethomebase из файла конфигурации
        long lastSetHomeBaseMillis = plugin.getConfig().getLong(team + ".sethomebase", -1L);
        // Проверка, прошло ли достаточно времени с последней команды
        long requiredWaitTime = plugin.getConfig().getInt("plugin.raid.setHomeBaseCooldown");
        if (lastSetHomeBaseMillis != -1L && (System.currentTimeMillis() - lastSetHomeBaseMillis) / 1000 / 60 < requiredWaitTime) {
            // Если не прошло достаточно времени, показываем сообщение
            player.sendMessage(ChatColor.RED + "Подождите несколько минут перед использованием данной команды, а именно: " + requiredWaitTime + ".");
            return true;
        }

        Location playerLocation = player.getLocation();

        Location homeLocationFirst = plugin.getGame().getHomeLocation(team);

        // Записываем координаты игрока в файл конфигурации для указанной команды
        plugin.getConfig().set(team + ".home.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + ".home.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + ".home.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + ".home.world", playerLocation.getWorld().getName());
        plugin.saveConfig();

        plugin.getGame().loadHomes();

        // Размещаем структуру нексуса
        World world = playerLocation.getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)

        Location homeLocation = plugin.getGame().getHomeLocation(team);
        boolean canPlaceHome = true;

        // Проверяем, возможно ли разместить структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(homeLocation.getBlockX() + x, homeLocation.getBlockY() + y, homeLocation.getBlockZ() + z);
                    // Проверяем остальные блоки базы (воздух)
                    if (block.getType() != Material.AIR) {
                        canPlaceHome = false;
                        break;
                    }
                }
            }
        }

        if (canPlaceHome) {
            if (homeLocationFirst != null) {
                plugin.getGame().removeFullHome(homeLocationFirst);
            }
            plugin.getGame().buildFullHome(plugin.getGame().getPlayerTeam(player.getName()));

            player.sendMessage(ChatColor.GREEN + "Местоположение базы для рейда вашей команды '" + team + "' было установлено как Ваша текущая локация.");
            player.sendMessage(ChatColor.GREEN + "Её координаты: " + playerLocation.getBlockX() + " " + (playerLocation.getBlockY() + 1) + " " + playerLocation.getBlockZ());
            player.sendMessage(ChatColor.GREEN + "Нексус был расположен.");

            plugin.getConfig().set(team + ".sethomebase", System.currentTimeMillis());
            plugin.saveConfig();

            // Перемещаем игрока, если нужно
            plugin.getGame().teleportPlayerToSafePosition(player);
        } else {
            if (homeLocationFirst != null) {
                plugin.getConfig().set(team + ".home.x", homeLocationFirst.getBlockX());
                plugin.getConfig().set(team + ".home.y", homeLocationFirst.getBlockY());
                plugin.getConfig().set(team + ".home.z", homeLocationFirst.getBlockZ());
                plugin.getConfig().set(team + ".home.world", homeLocationFirst.getWorld().getName());
                plugin.saveConfig();
            }

            player.sendMessage(ChatColor.RED + "Невозможно разместить центр дома. Проверьте место для размещения.");
        }

        return true;
    }

}
