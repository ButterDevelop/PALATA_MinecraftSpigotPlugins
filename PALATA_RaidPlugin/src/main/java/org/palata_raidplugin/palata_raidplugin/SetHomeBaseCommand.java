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

import java.util.Objects;

public class SetHomeBaseCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public SetHomeBaseCommand(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка, что команду запускает игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        final Player player = (Player) sender;
        final String playerName = player.getName();
        final String team = plugin.getGame().getPlayerTeam(playerName);

        // Проверка, является ли игрок капитаном
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Только капитан может установить центр дома.");
            return true;
        }

        final World world = player.getWorld();
        final String worldName = world.getName();

        // Проверка, создаётся ли home в основных мирах
        if (!"world".equals(worldName) && !"world_nether".equals(worldName) && !"world_the_end".equals(worldName)) {
            player.sendMessage(ChatColor.RED + "Вы можете устанавливать свой дом только в основных 3-х мирах.");
            return true;
        }

        // Проверка установки дома на главном острове в Энде
        if ("world_the_end".equals(worldName) &&
                plugin.getGame().isWithin2DRadius(player.getLocation(), new Location(world, 0, 0, 0), plugin.getGame().getEndWorldMainIslandRadius())) {
            player.sendMessage(ChatColor.RED + "Вы не можете установить свой дом на главном острове в Энде! Это территория Дракона.");
            return true;
        }

        // Проверка территориальных ограничений
        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить дом! Вы сейчас на территории чужой базы для рейда.");
            return true;
        }
        if (plugin.getGame().isWithinHomeRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить дом! Вы сейчас на территории чужого дома.");
            return true;
        }
        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), team)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить дом! Вы сейчас на территории своей базы для рейда.");
            return true;
        }

        final int radiusHome = plugin.getConfig().getInt("plugin.raid.privateRadiusHome");
        final int radiusRaid = plugin.getConfig().getInt("plugin.raid.privateRadiusRaid");
        Location nexusLocation = plugin.getGame().getNexusLocation(team);
        if (nexusLocation != null && plugin.getGame().isWithinRadius(player.getLocation(), nexusLocation, radiusHome + radiusRaid)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить дом! Территория этого дома и территория вашей базы для рейда пересекаются.");
            return true;
        }

        // Проверка времени последнего использования команды
        final long lastSetHomeBaseMillis = plugin.getConfig().getLong(team + ".sethomebase", -1L);
        final int requiredWaitTime = plugin.getConfig().getInt("plugin.raid.setHomeBaseCooldown");
        if (lastSetHomeBaseMillis != -1L && (System.currentTimeMillis() - lastSetHomeBaseMillis) / 1000 / 60 < requiredWaitTime) {
            player.sendMessage(ChatColor.RED + "Подождите несколько минут перед использованием данной команды, а именно: " + requiredWaitTime + ".");
            return true;
        }

        final Location playerLocation = player.getLocation();
        final Location homeLocationFirst = plugin.getGame().getHomeLocation(team, worldName);

        // Сохраняем новую точку установки центра дома в конфигурацию
        plugin.getConfig().set(team + "." + worldName + ".home.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + "." + worldName + ".home.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + "." + worldName + ".home.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + "." + worldName + ".home.world", worldName);
        plugin.saveConfig();

        final int baseRadius = 1;
        boolean canPlaceHome = true;
        Location homeLocation = plugin.getGame().getHomeLocation(team, worldName);

        // Проверяем, возможно ли разместить структуру дома
        if (homeLocation != null) {
            for (int x = -baseRadius; x <= baseRadius && canPlaceHome; x++) {
                for (int y = -baseRadius; y <= baseRadius && canPlaceHome; y++) {
                    for (int z = -baseRadius; z <= baseRadius; z++) {
                        Block block = world.getBlockAt(homeLocation.getBlockX() + x, homeLocation.getBlockY() + y, homeLocation.getBlockZ() + z);
                        if (block.getType() != Material.AIR) {
                            canPlaceHome = false;
                            break;
                        }
                    }
                }
            }
        }

        // Размещение структуры дома
        if (canPlaceHome) {
            if (homeLocationFirst != null) {
                plugin.getGame().removeFullHome(homeLocationFirst);
            }
            plugin.getGame().buildFullHome(team, worldName);

            player.sendMessage(ChatColor.GREEN + "Местоположение дома для вашей команды '" + team + "' было установлено как Ваша текущая локация.");
            player.sendMessage(ChatColor.GREEN + "Её координаты: "
                    + playerLocation.getBlockX() + " "
                    + (playerLocation.getBlockY() + 1) + " "
                    + playerLocation.getBlockZ());
            player.sendMessage(ChatColor.GREEN + "Центр дома был установлен.");

            plugin.getConfig().set(team + ".sethomebase", System.currentTimeMillis());
            plugin.saveConfig();

            plugin.getGame().teleportPlayerToSafePosition(player);
        } else {
            if (homeLocationFirst != null) {
                plugin.getConfig().set(team + "." + worldName + ".home.x", homeLocationFirst.getBlockX());
                plugin.getConfig().set(team + "." + worldName + ".home.y", homeLocationFirst.getBlockY());
                plugin.getConfig().set(team + "." + worldName + ".home.z", homeLocationFirst.getBlockZ());
                plugin.getConfig().set(team + "." + worldName + ".home.world", Objects.requireNonNull(homeLocationFirst.getWorld()).getName());
                plugin.saveConfig();
            }
            player.sendMessage(ChatColor.RED + "Невозможно разместить центр дома. Проверьте место для размещения.");
        }

        return true;
    }
}
