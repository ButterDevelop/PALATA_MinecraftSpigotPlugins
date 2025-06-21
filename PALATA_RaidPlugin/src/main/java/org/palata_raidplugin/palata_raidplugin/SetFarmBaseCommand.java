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

public class SetFarmBaseCommand implements CommandExecutor {

    private final PALATA_RaidPlugin plugin;

    public SetFarmBaseCommand(PALATA_RaidPlugin plugin) {
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
            player.sendMessage(ChatColor.RED + "Только капитан может установить центр фермы.");
            return true;
        }

        final World world = player.getWorld();
        final String worldName = world.getName();

        // Проверка, создаётся ли home в основных мирах
        if (!"world".equals(worldName) && !"world_nether".equals(worldName) && !"world_the_end".equals(worldName)) {
            player.sendMessage(ChatColor.RED + "Вы можете устанавливать свою ферму только в основных 3-х мирах.");
            return true;
        }

        // Проверка установки фермы на главном острове в Энде
        if ("world_the_end".equals(worldName) &&
                plugin.getGame().isWithin2DRadius(player.getLocation(), new Location(world, 0, 0, 0), plugin.getGame().getEndWorldMainIslandRadius())) {
            player.sendMessage(ChatColor.RED + "Вы не можете установить свою ферму на главном острове в Энде! Это территория Дракона.");
            return true;
        }

        // Проверка территориальных ограничений
        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить ферму! Вы сейчас на территории чужой базы для рейда.");
            return true;
        }
        if (plugin.getGame().isWithinHomeRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить ферму! Вы сейчас на территории чужого дома.");
            return true;
        }
        if (plugin.getGame().isWithinFarmRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить ферму! Вы сейчас на территории чужой фермы.");
            return true;
        }
        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), team)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить ферму! Вы сейчас на территории своей базы для рейда.");
            return true;
        }

        final int radiusFarm = plugin.getConfig().getInt("plugin.raid.privateRadiusFarm");
        final int radiusRaid = plugin.getConfig().getInt("plugin.raid.privateRadiusRaid");
        Location nexusLocation = plugin.getGame().getNexusLocation(team);
        if (nexusLocation != null && plugin.getGame().isWithinRadius(player.getLocation(), nexusLocation, radiusFarm + radiusRaid)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить ферму! Территория этой фермы и территория вашей базы для рейда пересекаются.");
            return true;
        }

        // Проверка времени последнего использования команды
        final long lastSetFarmBaseMillis = plugin.getConfig().getLong(team + ".setfarmbase", -1L);
        final int requiredWaitTime = plugin.getConfig().getInt("plugin.raid.setFarmBaseCooldown");
        if (lastSetFarmBaseMillis != -1L && (System.currentTimeMillis() - lastSetFarmBaseMillis) / 1000 / 60 < requiredWaitTime) {
            player.sendMessage(ChatColor.RED + "Подождите несколько минут перед использованием данной команды, а именно: " + requiredWaitTime + ".");
            return true;
        }

        final Location playerLocation = player.getLocation();
        final Location farmLocationFirst = plugin.getGame().getFarmLocation(team, worldName);

        // Сохраняем новую точку установки центра фермы в конфигурацию
        plugin.getConfig().set(team + "." + worldName + ".farm.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + "." + worldName + ".farm.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + "." + worldName + ".farm.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + "." + worldName + ".farm.world", worldName);
        plugin.saveConfig();

        final int baseRadius = 1;
        boolean canPlaceFarm = true;
        Location farmLocation = plugin.getGame().getFarmLocation(team, worldName);

        // Проверяем, возможно ли разместить структуру фермы
        if (farmLocation != null) {
            for (int x = -baseRadius; x <= baseRadius && canPlaceFarm; x++) {
                for (int y = -baseRadius; y <= baseRadius && canPlaceFarm; y++) {
                    for (int z = -baseRadius; z <= baseRadius; z++) {
                        Block block = world.getBlockAt(farmLocation.getBlockX() + x, farmLocation.getBlockY() + y, farmLocation.getBlockZ() + z);
                        if (block.getType() != Material.AIR) {
                            canPlaceFarm = false;
                            break;
                        }
                    }
                }
            }
        }

        // Размещение структуры фермы
        if (canPlaceFarm) {
            if (farmLocationFirst != null) {
                plugin.getGame().removeFullHome(farmLocationFirst);
            }
            plugin.getGame().buildFullFarm(team, worldName);

            player.sendMessage(ChatColor.GREEN + "Местоположение фермы для вашей команды '" + team + "' было установлено как Ваша текущая локация.");
            player.sendMessage(ChatColor.GREEN + "Её координаты: "
                    + playerLocation.getBlockX() + " "
                    + (playerLocation.getBlockY() + 1) + " "
                    + playerLocation.getBlockZ());
            player.sendMessage(ChatColor.GREEN + "Центр фермы был установлен.");

            plugin.getConfig().set(team + ".setfarmbase", System.currentTimeMillis());
            plugin.saveConfig();

            plugin.getGame().teleportPlayerToSafePosition(player);
        } else {
            if (farmLocationFirst != null) {
                plugin.getConfig().set(team + "." + worldName + ".farm.x", farmLocationFirst.getBlockX());
                plugin.getConfig().set(team + "." + worldName + ".farm.y", farmLocationFirst.getBlockY());
                plugin.getConfig().set(team + "." + worldName + ".farm.z", farmLocationFirst.getBlockZ());
                plugin.getConfig().set(team + "." + worldName + ".farm.world", Objects.requireNonNull(farmLocationFirst.getWorld()).getName());
                plugin.saveConfig();
            }
            player.sendMessage(ChatColor.RED + "Невозможно разместить центр фермы. Проверьте место для размещения.");
        }

        return true;
    }
}