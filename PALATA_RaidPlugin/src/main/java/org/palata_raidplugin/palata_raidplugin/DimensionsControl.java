package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Менеджер контроля открытия порталов в Нижний мир и Край по времени с момента старта игры.
 * Сохраняет время старта в конфиге, чтобы после перезагрузки сервер продолжал работать корректно.
 * Обрабатывает команды /dimensionalcontrol start и /dimensionalcontrol stop.
 */
public class DimensionsControl implements Listener, CommandExecutor {

    private final PALATA_RaidPlugin plugin;
    private long gameStartTime;
    private final long netherDelayMillis;
    private final long endDelayMillis;

    public DimensionsControl(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
        this.netherDelayMillis = plugin.getConfig()
                .getLong("dimensions.nether.openAfterSeconds", 7200) * 1000L;
        this.endDelayMillis = plugin.getConfig()
                .getLong("dimensions.end.openAfterSeconds", 14400) * 1000L;
        this.gameStartTime = plugin.getConfig().getLong("dimensions.gameStartTime", 0L);
    }

    /**
     * Форматирует длительность в HH:mm:ss
     */
    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Запускает отсчёт, сохраняет время старта
     */
    private void startCountdown() {
        this.gameStartTime = System.currentTimeMillis();
        plugin.getConfig().set("dimensions.gameStartTime", this.gameStartTime);
        plugin.saveConfig();
    }

    /**
     * Останавливает отсчёт, сбрасывая время старта
     */
    private void stopCountdown() {
        this.gameStartTime = 0L;
        plugin.getConfig().set("dimensions.gameStartTime", 0L);
        plugin.saveConfig();
    }

    /**
     * Обработка команд /dimensionalcontrol start|stop
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String action = args[0].toLowerCase();
            if (action.equals("start")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только игрок может запустить отсчёт измерений.");
                    return true;
                }
                startCountdown();
                long netherSecs = netherDelayMillis / 1000;
                long endSecs = endDelayMillis / 1000;
                String netherFmt = formatDuration(netherSecs);
                String endFmt = formatDuration(endSecs);
                Bukkit.broadcastMessage(ChatColor.GREEN + "[DimensionsControl] Отсчёт запущен! " +
                        "Nether через " + netherSecs + " сек (" + netherFmt + "), " +
                        "End через " + endSecs + " сек (" + endFmt + ").");
                return true;
            } else if (action.equals("stop")) {
                stopCountdown();
                Bukkit.broadcastMessage(ChatColor.GREEN + "[DimensionsControl] Отсчёт остановлен. Порталы открыты.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "Использование: /dimensionalcontrol <start|stop>");
        return true;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        TeleportCause cause = event.getCause();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        // Блокировка порталов в Nether и End при активном отсчёте
        if (gameStartTime > 0) {
            if (cause == TeleportCause.NETHER_PORTAL) {
                if (now < gameStartTime + netherDelayMillis) {
                    long rem = (gameStartTime + netherDelayMillis - now) / 1000;
                    String fmt = formatDuration(rem);
                    player.sendMessage(ChatColor.RED + "Портал в Нижний мир не активен. Осталось: "
                            + rem + " сек (" + fmt + ").");
                    event.setCancelled(true);
                    return;
                }
            } else if (cause == TeleportCause.END_PORTAL) {
                if (now < gameStartTime + endDelayMillis) {
                    long rem = (gameStartTime + endDelayMillis - now) / 1000;
                    String fmt = formatDuration(rem);
                    player.sendMessage(ChatColor.RED + "Портал в Край не активен. Осталось: "
                            + rem + " сек (" + fmt + ").");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Обработка портала в Край: генерация и телепорт
        if (cause == TeleportCause.END_PORTAL) {
            String team = plugin.getGame().getPlayerTeam(player.getName());
            if (team == null) {
                player.sendMessage(ChatColor.RED + "Вы не состоите в команде - в Край нельзя.");
                event.setCancelled(true);
                return;
            }
            String base = "dimensions.end.spawn." + team + ".";
            String worldName = plugin.getConfig().getString(base + "world", "world_the_end");
            double x = plugin.getConfig().getDouble(base + "x");
            double y = plugin.getConfig().getDouble(base + "y");
            double z = plugin.getConfig().getDouble(base + "z");
            World endWorld = Bukkit.getWorld(worldName);
            if (endWorld == null) endWorld = Bukkit.getWorld("world_the_end");

            // Построение платформы согласно Minecraft Wiki
            int size = 5;
            int platformY = (int) y - 1;
            int half = size / 2;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    int bx = (int) x + dx;
                    int bz = (int) z + dz;
                    // Заменяем любой блок (в т.ч. end stone) на обсидиан
                    assert endWorld != null;
                    endWorld.getBlockAt(bx, platformY, bz).setType(Material.OBSIDIAN);
                    // Очищаем пространство над платформой на высоту 3 блока
                    for (int dy = 1; dy <= 3; dy++) {
                        endWorld.getBlockAt(bx, platformY + dy, bz).setType(Material.AIR);
                    }
                }
            }

            // Точка спавна игрока одна единица выше платформы, повёрнута на запад (yaw=90)
            Location spawnLoc = new Location(endWorld, x, y, z, 90.0f, 0.0f);
            event.setTo(spawnLoc);
        }

        // Портал в Нижний мир: после задержки работает как обычно
    }
}
