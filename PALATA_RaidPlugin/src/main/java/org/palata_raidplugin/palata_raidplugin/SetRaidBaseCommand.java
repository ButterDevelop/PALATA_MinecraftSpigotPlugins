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
        // Проверка, что команду запускает игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        final Player player = (Player) sender;
        final String playerName = player.getName();
        final String team = plugin.getGame().getPlayerTeam(playerName);

        // Проверка наличия команды у капитана (здесь логика может зависеть от вашей реализации)
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Только капитан может установить Нексус.");
            return true;
        }

        // Проверка мира
        if (!"world".equals(player.getWorld().getName())) {
            player.sendMessage(ChatColor.RED + "Базу можно установить только в обычном мире.");
            return true;
        }

        // Проверка активности рейда
        if (plugin.getGame().isRaidActive()) {
            player.sendMessage(ChatColor.RED + "Нельзя сменить базу во время рейда.");
            return true;
        }

        // Проверки территории
        if (plugin.getGame().isWithinNexusRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить Нексус! Вы сейчас на территории чужой базы для рейда.");
            return true;
        }
        if (plugin.getGame().isWithinHomeRadius(player.getLocation(), plugin.getGame().getDefendingTeam(team))) {
            player.sendMessage(ChatColor.RED + "Невозможно установить Нексус! Вы сейчас на территории чужого дома.");
            return true;
        }
        if (plugin.getGame().isWithinHomeRadius(player.getLocation(), team)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить Нексус! Вы сейчас на территории своего дома.");
            return true;
        }

        final int radiusHome = plugin.getConfig().getInt("plugin.raid.privateRadiusHome");
        final int radiusRaid = plugin.getConfig().getInt("plugin.raid.privateRadiusRaid");
        Location homeLocation = plugin.getGame().getHomeLocation(team, player.getWorld().getName());
        if (homeLocation != null && plugin.getGame().isWithinRadius(player.getLocation(), homeLocation, radiusHome + radiusRaid)) {
            player.sendMessage(ChatColor.RED + "Невозможно установить дом! Территория этого дома и территория вашей базы для рейда пересекаются.");
            return true;
        }

        // Проверка времени последнего использования команды
        final long lastSetRaidBaseMillis = plugin.getConfig().getLong(team + ".setraidbase", -1L);
        final int requiredWaitTime = plugin.getConfig().getInt("plugin.raid.setRaidBaseCooldown");
        if (lastSetRaidBaseMillis != -1L && (System.currentTimeMillis() - lastSetRaidBaseMillis) / 1000 / 60 < requiredWaitTime) {
            player.sendMessage(ChatColor.RED + "Подождите несколько минут перед использованием данной команды, а именно: " + requiredWaitTime + ".");
            return true;
        }

        final Location playerLocation = player.getLocation();
        final World world = playerLocation.getWorld();
        final int baseRadius = 1;

        // Сохраняем координаты нексуса
        plugin.getConfig().set(team + ".nexus.x", playerLocation.getBlockX());
        plugin.getConfig().set(team + ".nexus.y", playerLocation.getBlockY() + 1);
        plugin.getConfig().set(team + ".nexus.z", playerLocation.getBlockZ());
        plugin.getConfig().set(team + ".nexus.world", world.getName());
        plugin.saveConfig();

        plugin.getGame().loadBases();

        // Проверяем возможность разместить нексус
        final Location nexusLocation = plugin.getGame().getNexusLocation(team);
        boolean canPlaceNexus = true;
        if (nexusLocation != null) {
            for (int x = -baseRadius; x <= baseRadius && canPlaceNexus; x++) {
                for (int y = -baseRadius; y <= baseRadius && canPlaceNexus; y++) {
                    for (int z = -baseRadius; z <= baseRadius; z++) {
                        Block block = world.getBlockAt(nexusLocation.getBlockX() + x, nexusLocation.getBlockY() + y, nexusLocation.getBlockZ() + z);
                        if (block.getType() != Material.AIR) {
                            canPlaceNexus = false;
                            break;
                        }
                    }
                }
            }
        }

        if (canPlaceNexus) {
            final Location existingNexusLocation = plugin.getGame().getNexusLocation(team);
            if (existingNexusLocation != null) {
                plugin.getGame().removeFullNexus(existingNexusLocation);
            }
            plugin.getGame().buildFullNexus(team);

            player.sendMessage(ChatColor.GREEN + "Местоположение базы для рейда вашей команды '" + team + "' было установлено как Ваша текущая локация.");
            player.sendMessage(ChatColor.GREEN + "Её координаты: "
                    + playerLocation.getBlockX() + " "
                    + (playerLocation.getBlockY() + 1) + " "
                    + playerLocation.getBlockZ());
            player.sendMessage(ChatColor.GREEN + "Нексус был расположен.");

            plugin.getConfig().set(team + ".setraidbase", System.currentTimeMillis());
            plugin.saveConfig();

            plugin.getGame().teleportPlayerToSafePosition(player);
        } else {
            final Location previousNexus = plugin.getGame().getNexusLocation(team);
            if (previousNexus != null) {
                plugin.getConfig().set(team + ".nexus.x", previousNexus.getBlockX());
                plugin.getConfig().set(team + ".nexus.y", previousNexus.getBlockY());
                plugin.getConfig().set(team + ".nexus.z", previousNexus.getBlockZ());
                plugin.getConfig().set(team + ".nexus.world", previousNexus.getWorld().getName());
                plugin.saveConfig();
            }
            player.sendMessage(ChatColor.RED + "Невозможно разместить Нексус. Проверьте место для размещения.");
        }

        return true;
    }
}
