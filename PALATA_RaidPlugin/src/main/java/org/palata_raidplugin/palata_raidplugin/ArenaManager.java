package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

// Класс для хранения информации об арене
class ArenaConfig {
    private final World copyWorld;
    private final World world;
    private final Location copyStart;
    private final Location copyEnd;
    private final Location spawnRed;
    private final Location spawnBlue;
    private final Map<Location, Material> originalArenaBlocks = new HashMap<>();

    public ArenaConfig(ConfigurationSection section) {
        String worldName = section.getString("world");
        String copyWorldName = section.getString("copyWorld");
        world = Bukkit.getWorld(worldName);
        copyWorld = Bukkit.getWorld(copyWorldName);

        this.copyStart = new Location(copyWorld,
                section.getInt("copyStart.x"),
                section.getInt("copyStart.y"),
                section.getInt("copyStart.z"));

        this.copyEnd = new Location(copyWorld,
                section.getInt("copyEnd.x"),
                section.getInt("copyEnd.y"),
                section.getInt("copyEnd.z"));

        this.spawnRed = new Location(world,
                section.getInt("spawnRed.x"),
                section.getInt("spawnRed.y"),
                section.getInt("spawnRed.z"));

        this.spawnBlue = new Location(world,
                section.getInt("spawnBlue.x"),
                section.getInt("spawnBlue.y"),
                section.getInt("spawnBlue.z"));

        loadOriginalArenaBlocks();
    }

    private void loadOriginalArenaBlocks() {
        int x1 = copyStart.getBlockX();
        int y1 = copyStart.getBlockY();
        int z1 = copyStart.getBlockZ();
        int x2 = copyEnd.getBlockX();
        int y2 = copyEnd.getBlockY();
        int z2 = copyEnd.getBlockZ();

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    originalArenaBlocks.put(loc, loc.getBlock().getType());
                }
            }
        }
    }


    public World getCopyWorld() {
        return copyWorld;
    }

    public World getWorld() {
        return world;
    }

    public Location getCopyStart() {
        return copyStart;
    }

    public Location getCopyEnd() {
        return copyEnd;
    }

    public Location getSpawnRed() {
        return spawnRed;
    }

    public Location getSpawnBlue() {
        return spawnBlue;
    }

    public Map<Location, Material> getOriginalArenaBlocks() {
        return originalArenaBlocks;
    }
}

public class ArenaManager implements Listener {
    private final PALATA_RaidPlugin plugin;
    private final Set<Player> arenaPlayers = new HashSet<>();
    private boolean isArenaActive = false;
    private BukkitRunnable arenaTask = null;
    private BukkitRunnable chatTask = null;
    private BukkitRunnable arenaCountdownTask = null;
    private final int killScore;
    private final int winScore;
    private final Map<Player, Location> playerLocations = new HashMap<>();
    private long nextArenaTime;

    private int currentArenaIndex = 1; // Индекс текущей арены
    private List<ArenaConfig> arenas = new ArrayList<>(); // Список конфигураций арен

    public ArenaManager(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;

        // Отложенная инициализация конфигураций арен
        //Bukkit.getScheduler().runTaskLater(plugin, this::loadArenaConfigs, 20L * 90); // 90 секунд задержки

        currentArenaIndex = plugin.getConfig().getInt("arena.currentArenaIndex", 1);
        killScore         = plugin.getConfig().getInt("arena.killScore", 1);
        winScore          = plugin.getConfig().getInt("arena.winScore", 5);

        checkAndScheduleArena();
    }

    private void loadArenaConfigs() {
        // Загрузка конфигураций арен
        for (int i = 1; plugin.getConfig().getConfigurationSection("arena_" + i) != null; i++) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("arena_" + i);
            if (section != null) {
                ArenaConfig arenaConfig = new ArenaConfig(section);
                arenas.add(arenaConfig);
            }
        }
    }

    public void checkAndScheduleArena() {
        nextArenaTime = plugin.getConfig().getLong("arena.nextStartTime", 0);
        long currentTime = System.currentTimeMillis();

        // Время арены не было установлено
        if (nextArenaTime == 0) {
            scheduleNextArena();
        }

        if (nextArenaTime <= currentTime) {
            // Время арены наступило или не было установлено
            startArena();
        } else {
            long delay = nextArenaTime - currentTime;

            // Если до начала арены остается 10 минут или меньше, запускаем уведомления
            if (delay <= 600000) { // 600000 миллисекунд = 10 минут
                startArenaCountdown();
            } else {
                // Запланируем начало обратного отсчета за 10 минут до начала арены
                Bukkit.getScheduler().runTaskLater(plugin, this::startArenaCountdown, (delay - 600000) / 50);
            }

            // Запланируем начало арены через оставшееся время
            Bukkit.getScheduler().runTaskLater(plugin, this::startArena, delay / 50); // Переводим миллисекунды в тики
        }
    }

    private void startArenaCountdown() {
        arenaCountdownTask = new BukkitRunnable() {
            final long currentTime = System.currentTimeMillis();
            final long timeUntilStart = nextArenaTime - currentTime; // Время до начала в миллисекундах

            // Рассчитываем количество минут до начала
            int countdownMinutes = (int) (timeUntilStart / 60000); // 60000 миллисекунд = 1 минута

            @Override
            public void run() {
                if (countdownMinutes > 0) {
                    Bukkit.getOnlinePlayers().forEach(player ->
                            player.sendMessage(ChatColor.GOLD + "Арена начнется через " + countdownMinutes +
                                    " минут(ы). Напишите /joinarena, чтобы присоединиться!"));

                    countdownMinutes--;
                } else {
                    cancel(); // Отменяем задачу, когда время истекло
                }
            }
        };
        arenaCountdownTask.runTaskTimer(plugin, 0, 20 * 60); // Запускаем каждую минуту (20 тиков = 1 секунда)
    }

    public void scheduleNextArena(long localNextArenaTime) {
        nextArenaTime = localNextArenaTime;
        plugin.getConfig().set("arena.nextStartTime", localNextArenaTime);
        plugin.saveConfig();

        clearTask(chatTask);
        clearTask(arenaCountdownTask);

        // Форматирование даты и времени
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String formattedDate = dateFormat.format(new Date(nextArenaTime));

        chatTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    Bukkit.getOnlinePlayers().forEach(player ->
                            player.sendMessage(ChatColor.GREEN + "Следующая арена начнётся в " + formattedDate + "!"));
                }
            }
        };
        chatTask.run();
    }

    public void scheduleNextArena() {
        long interval = plugin.getConfig().getLong("arena.intervalHours", 24) * 60 * 60 * 1000; // Часы в миллисекундах
        long localNextArenaTime = System.currentTimeMillis() + interval;

        scheduleNextArena(localNextArenaTime);
    }

    public void startArena() {
        if (isArenaActive) {
            return;
        }

        isArenaActive = true;

        if (isOneTeamRemaining() || arenaPlayers.isEmpty()) {
            finishArena();
            return;
        }

        ArenaConfig currentArena = arenas.get(currentArenaIndex - 1);
        if (currentArena == null) {
            finishArena();
            return;
        }

        // Сохраняем местоположение и телепортируем игроков на арену
        Iterator<Player> iterator = arenaPlayers.iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (player.isDead()) {
                player.sendMessage(ChatColor.RED + "Вы мертвы, поэтому не будете телепортированы на арену.");
                iterator.remove();
            } else {
                playerLocations.put(player, player.getLocation());
                if (Objects.equals(plugin.getGame().getPlayerTeam(player.getName()), "RED")) {
                    player.teleport(currentArena.getSpawnRed());
                } else {
                    player.teleport(currentArena.getSpawnBlue());
                }
            }
        }

        int arenaDurationSeconds = plugin.getConfig().getInt("arena.durationSeconds", 300);
        clearTask(arenaTask);
        arenaTask = new BukkitRunnable() {
            int countdown = arenaDurationSeconds;

            @Override
            public void run() {
                if (countdown <= 0 || isOneTeamRemaining()) {
                    finishArena();
                    cancel();
                    return;
                }

                if (countdown > 0 && countdown <= 10) { // Последние 10 секунд - наносим урон
                    arenaPlayers.forEach(player -> {
                        player.sendMessage(ChatColor.RED + "До конца арены осталось секунд: " + countdown + ". Всем живым наносится урон!");
                        player.damage(4.0); // Наносим урон
                    });
                }

                if (countdown % 60 == 0) {
                    if (countdown == 0) {
                        arenaPlayers.forEach(player -> player.sendMessage(ChatColor.GREEN + "Арена завершается!"));
                    } else {
                        arenaPlayers.forEach(player -> player.sendMessage(ChatColor.GREEN + "До конца арены осталось секунд: " + countdown));
                    }
                }

                countdown--;
            }
        };
        arenaTask.runTaskTimer(plugin, 0, 20);
    }

    private boolean isOneTeamRemaining() {
        Set<String> remainingTeams = new HashSet<>();
        arenaPlayers.forEach(player -> remainingTeams.add(plugin.getGame().getPlayerTeam(player.getName())));
        return remainingTeams.size() <= 1;
    }

    public void finishArena() {
        if (!isArenaActive) {
            return;
        }

        isArenaActive = false;

        String winningTeam;
        if (isOneTeamRemaining()) {
            winningTeam = arenaPlayers.stream()
                    .map(player -> plugin.getGame().getPlayerTeam(player.getName()))
                    .findFirst().orElse(null);
        } else {
            winningTeam = null;
        }

        if (winningTeam != null) {
            arenaPlayers.forEach(player -> {
                if (plugin.getGame().getPlayerTeam(player.getName()).equals(winningTeam)) {
                    player.sendMessage(ChatColor.GREEN + "Ваша команда победила на арене!");
                } else {
                    player.sendMessage(ChatColor.RED + "Ваша команда проиграла на арене.");
                }
            });

            // Добавление очков команде
            plugin.getGame().addScore(winningTeam, winScore);
        } else {
            arenaPlayers.forEach(player -> player.sendMessage(ChatColor.YELLOW + "Арена закончилась ничьей!"));
        }

        scheduleNextArena();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Телепортация игроков обратно
                arenaPlayers.forEach(player -> {
                    Location originalLocation = playerLocations.get(player);
                    if (originalLocation != null) {
                        player.teleport(originalLocation);
                    }
                });
                playerLocations.clear();
                arenaPlayers.clear();

                // После завершения арены, переходим к следующей
                currentArenaIndex++;
                if (currentArenaIndex > arenas.size()) {
                    currentArenaIndex = 1; // Возвращаемся к первой арене, если достигли конца списка
                }
                plugin.getConfig().set("arena.currentArenaIndex", currentArenaIndex);

                checkAndScheduleArena();

                copyArena();
            }
        };
        task.runTaskLater(plugin, 30 * 20L);
    }

    public void copyArena() {
        ArenaConfig currentArena = arenas.get(currentArenaIndex - 1);
        if (currentArena == null) return;

        if (currentArena.getWorld() == null) return;

        // Удаление всех мобов и сущностей в мире арены
        for (Entity entity : currentArena.getWorld().getEntities()) {
            if (!(entity instanceof Player)) { // Игнорируем игроков
                entity.remove();
            }
        }

        if (currentArena.getCopyWorld() == null) {
            return;
        }

        int x1 = currentArena.getCopyStart().getBlockX();
        int y1 = currentArena.getCopyStart().getBlockY();
        int z1 = currentArena.getCopyStart().getBlockZ();
        int x2 = currentArena.getCopyEnd().getBlockX();
        int y2 = currentArena.getCopyEnd().getBlockY();
        int z2 = currentArena.getCopyEnd().getBlockZ();

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location sourceLoc = new Location(currentArena.getCopyWorld(), x, y, z);
                    Location targetLoc = new Location(currentArena.getWorld(), x, y, z);

                    targetLoc.getBlock().setType(sourceLoc.getBlock().getType());
                    targetLoc.getBlock().setBlockData(sourceLoc.getBlock().getBlockData());
                }
            }
        }
    }

    public boolean isPlayerAlreadyInArena(Player player) {
        return arenaPlayers.contains(player);
    }

    public void addPlayerToArena(Player player) {
        if (isArenaActive) {
            return;
        }

        arenaPlayers.add(player);
    }

    public boolean isArenaActive() {
        return isArenaActive;
    }

    public long getNextArenaTime() {
        return nextArenaTime;
    }

    public void clearTask(BukkitRunnable task) {
        if (task != null) {
            // Проверяем, была ли задача уже запланирована и не отменена
            try {
                if (task.getTaskId() != -1 && !task.isCancelled()) {
                    task.cancel();
                }
            } catch (IllegalStateException e) {
                // Это означает, что задача еще не была запланирована
                // Не нужно ничего делать в этом случае
            }
            task = null; // Сбрасываем ссылку на задачу после её отмены
        }
    }

    // Загрузка конфигураций арен при загрузке мира
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        // Проверяем, соответствует ли загруженный мир одному из нужных нам миров
        for (int i = 1; plugin.getConfig().getConfigurationSection("arena_" + i) != null; i++) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("arena_" + i);
            if (section != null) {
                String arenaWorldName = section.getString("world");
                if (worldName.equals(arenaWorldName)) {
                    ArenaConfig arenaConfig = new ArenaConfig(section);
                    arenas.add(arenaConfig);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Если игрок находится в арене и арена активна
        if (isArenaActive && arenaPlayers.contains(player)) {
            // Удаляем игрока из списка участников арены
            arenaPlayers.remove(player);

            // Проверяем, осталась ли одна команда
            if (isOneTeamRemaining()) {
                finishArena();
            }

            // Возвращаем игрока на его исходное местоположение
            Location originalLocation = playerLocations.get(player);
            if (originalLocation != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(originalLocation));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (int i = 1; plugin.getConfig().getConfigurationSection("arena_" + i) != null; i++) {
            ArenaConfig currentArena = arenas.get(i - 1);
            if (currentArena == null) continue;

            // Если игрок находится в мире арены и арена активна
            if (player.getWorld().equals(currentArena.getWorld())) {
                if (player.getBedSpawnLocation() == null) {
                    World world = Bukkit.getServer().getWorld("world");
                    if (world == null) {
                        player.kickPlayer("По какой-то причине невозможно переместить вас в обычный мир." +
                                "Скорее всего, это ошибка сервера. Сообщите администратору.");
                        return;
                    }

                    // Телепортируем игрока на локацию спавна основного мира
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(world.getSpawnLocation()));
                } else {
                    // Телепортируем игрока на локацию его спавна
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(player.getBedSpawnLocation()));
                }

                player.sendMessage(ChatColor.YELLOW + "Вы были перемещены на свою точку возрождения.");
            }
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)  {
        ArenaConfig currentArena = arenas.get(currentArenaIndex - 1);
        if (currentArena == null) return;

        Player victim = event.getEntity();

        // Получаем убийцу игрока
        Player attacker = victim.getKiller();
        // Проверяем, убит ли игрок другим игроком
        if (attacker != null && !attacker.equals(victim)) {
            // Добавляем очки убийце, если они не в одной команде
            if (!plugin.getGame().areTwoPlayersInTheSameTeam(attacker.getName(), victim.getName())) {
                plugin.getGame().addScore(plugin.getGame().getPlayerTeam(attacker.getName()), killScore);
            }
        }

        // Проверяем, участвует ли игрок в арене
        if (victim.getWorld().getName().equals(currentArena.getWorld().getName())) {
            // Игрок умер на арене, удаляем его из списка участников
            arenaPlayers.remove(victim);

            if (isArenaActive) {
                victim.sendMessage(ChatColor.RED + "Вы погибли и были исключены из арены.");
            }

            // Телепортируем его на предыдущее местоположение (если это необходимо)
            Location originalLocation = playerLocations.get(victim);
            if (originalLocation != null) {
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        victim.sendMessage(ChatColor.GREEN + "Вы были телепортированы обратно.");
                        Bukkit.getScheduler().runTask(plugin, () -> victim.teleport(originalLocation));
                    }
                };
                task.runTaskLater(plugin, 20L);
            }

            // Проверяем, осталась ли одна команда
            if (isArenaActive && isOneTeamRemaining()) {
                finishArena();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block  = event.getBlock();
        Location loc = block.getLocation();

        if (loc.getWorld() == null) {
            return;
        }

        ArenaConfig currentArena = arenas.get(currentArenaIndex - 1);
        if (currentArena == null) return;

        String arenaWorldName = currentArena.getWorld().getName();

        if (loc.getWorld().getName().contains(arenaWorldName) &&
                currentArena.getOriginalArenaBlocks().containsKey(loc) &&
                currentArena.getOriginalArenaBlocks().get(loc) != Material.AIR) {
            event.setDropItems(false);
        }
    }
}
