package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Класс для хранения информации об арене.
 * Хранит мир с ареной, мир-копию, координаты для копирования,
 * спавны для RED/BLUE и оригинальные блоки арены.
 */
class ArenaConfig {

    private final World copyWorld;
    private final World world;
    private final Location copyStart;
    private final Location copyEnd;
    private final Location spawnRed;
    private final Location spawnBlue;
    private final Map<Location, Material> originalArenaBlocks = new HashMap<>();

    public ArenaConfig(final ConfigurationSection section) {
        final String worldName = section.getString("world");
        final String copyWorldName = section.getString("copyWorld");

        assert worldName != null;
        this.world = Bukkit.getWorld(worldName);
        assert copyWorldName != null;
        this.copyWorld = Bukkit.getWorld(copyWorldName);

        this.copyStart = new Location(
                this.copyWorld,
                section.getInt("copyStart.x"),
                section.getInt("copyStart.y"),
                section.getInt("copyStart.z")
        );

        this.copyEnd = new Location(
                this.copyWorld,
                section.getInt("copyEnd.x"),
                section.getInt("copyEnd.y"),
                section.getInt("copyEnd.z")
        );

        this.spawnRed = new Location(
                this.world,
                section.getInt("spawnRed.x"),
                section.getInt("spawnRed.y"),
                section.getInt("spawnRed.z")
        );

        this.spawnBlue = new Location(
                this.world,
                section.getInt("spawnBlue.x"),
                section.getInt("spawnBlue.y"),
                section.getInt("spawnBlue.z")
        );

        // Сохраняем оригинальные блоки арены в `originalArenaBlocks`
        loadOriginalArenaBlocks();
    }

    /**
     * Сохраняем типы всех блоков из основной арены (world) в заданном прямоугольном регионе.
     */
    private void loadOriginalArenaBlocks() {
        if (world == null) return;
        final int x1 = copyStart.getBlockX();
        final int y1 = copyStart.getBlockY();
        final int z1 = copyStart.getBlockZ();
        final int x2 = copyEnd.getBlockX();
        final int y2 = copyEnd.getBlockY();
        final int z2 = copyEnd.getBlockZ();

        final int minX = Math.min(x1, x2);
        final int minY = Math.min(y1, y2);
        final int minZ = Math.min(z1, z2);
        final int maxX = Math.max(x1, x2);
        final int maxY = Math.max(y1, y2);
        final int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Location loc = new Location(world, x, y, z);
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

/**
 * Класс для управления аренами.
 */
public class ArenaManager implements Listener {

    private final PALATA_RaidPlugin plugin;

    // Список игроков, участвующих в арене
    private final Set<Player> arenaPlayers = new HashSet<>();

    // Флаги состояния арены
    private boolean isArenaActive = false;

    // Различные задачи (таймеры), которые могут быть запущены
    private BukkitRunnable arenaTask = null;
    private BukkitRunnable chatTask = null;
    private BukkitRunnable arenaCountdownTask = null;

    private final int killScore; // Очки за убийство
    private final int winScore;  // Очки за победу

    private final Map<Player, Location> playerLocations = new HashMap<>();

    private long nextArenaTime;

    // Индекс текущей арены (номер из конфига)
    private int currentArenaIndex = 1;

    private final int arenasAmount;

    // Список доступных арен
    private List<ArenaConfig> arenas = new ArrayList<>();

    // -------------------- Конструктор --------------------

    public ArenaManager(final PALATA_RaidPlugin plugin) {
        this.plugin = plugin;

        // Читаем базовые настройки из конфига
        this.currentArenaIndex = plugin.getConfig().getInt("arena.currentArenaIndex", 1);
        this.killScore         = plugin.getConfig().getInt("arena.killScore", 1);
        this.winScore          = plugin.getConfig().getInt("arena.winScore", 5);
        this.arenasAmount      = plugin.getConfig().getInt("arena.arenasAmount", 3);

        // Заранее пытаемся запланировать арену
        checkAndScheduleArena();
    }

    // -------------------- Методы инициализации --------------------

    /**
     * Загрузка конфигураций арен из конфига.
     */
    private void loadArenaConfigs() {
        // Пробегаем по "arena_1", "arena_2" и т.д.
        arenas = new ArrayList<>();
        for (int i = 1; i <= arenasAmount; i++) {
            final ConfigurationSection section = plugin.getConfig().getConfigurationSection("arena_" + i);
            if (section != null) {
                final ArenaConfig arenaConfig = new ArenaConfig(section);
                arenas.add(arenaConfig);
            }
        }
    }

    // -------------------- Планирование и запуск арены --------------------

    /**
     * Проверяем, нужно ли запускать арену прямо сейчас, или планируем её.
     */
    public void checkAndScheduleArena() {
        if (arenas.isEmpty()) {
            // Перезагружаем конфиги
            loadArenaConfigs();
        }

        nextArenaTime = plugin.getConfig().getLong("arena.nextStartTime", 0);
        final long currentTime = System.currentTimeMillis();

        // Если время не установлено — сразу планируем
        if (nextArenaTime == 0) {
            scheduleNextArena();
        }

        if (nextArenaTime <= currentTime) {
            // Время начала арены уже наступило
            startArena();
        } else {
            // Время ещё не наступило — планируем задачи
            final long delayMillis = nextArenaTime - currentTime;

            // Если до арены 10 минут или меньше — запускаем обратный отсчёт
            if (delayMillis <= 600_000) { // 600_000 мс = 10 минут
                startArenaCountdown();
            } else {
                // Иначе запускаем обратный отсчёт за 10 минут до начала
                Bukkit.getScheduler().runTaskLater(plugin,
                        this::startArenaCountdown, (delayMillis - 600_000) / 50L);
            }

            // Также планируем старт арены
            Bukkit.getScheduler().runTaskLater(plugin,
                    this::startArena, delayMillis / 50L);
        }
    }

    /**
     * Запуск минутного (или заранее настроенного) отсчёта до начала арены.
     */
    private void startArenaCountdown() {
        if (arenaCountdownTask != null && !arenaCountdownTask.isCancelled()) {
            arenaCountdownTask.cancel();
        }

        arenaCountdownTask = new BukkitRunnable() {
            final long currentTime = System.currentTimeMillis();
            final long timeUntilStart = nextArenaTime - currentTime;
            int countdownMinutes = (int) (timeUntilStart / 60_000); // из мс в минуты

            @Override
            public void run() {
                if (countdownMinutes > 0) {
                    Bukkit.getOnlinePlayers().forEach(player ->
                            player.sendMessage(ChatColor.GOLD + "Арена начнется через " + countdownMinutes
                                    + " минут(ы). Напишите /joinarena, чтобы присоединиться!"));
                    countdownMinutes--;
                } else {
                    cancel(); // Как только дошли до 0 — останавливаемся
                }
            }
        };
        // Запускаем задачу каждую минуту (20 тиков = 1 секунда, значит 20*60 = 1 минута)
        arenaCountdownTask.runTaskTimer(plugin, 0L, 20L * 60);
    }

    /**
     * Устанавливаем время для следующей арены (конкретная метка времени).
     */
    public void scheduleNextArena(final long localNextArenaTime) {
        nextArenaTime = localNextArenaTime;
        plugin.getConfig().set("arena.nextStartTime", localNextArenaTime);
        plugin.saveConfig();

        clearTask(chatTask);
        clearTask(arenaCountdownTask);

        // Форматируем дату и время начала
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        final String formattedDate = dateFormat.format(new Date(nextArenaTime));

        // Запускаем немедленный вывод
        chatTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Несколько сообщений подряд, чтобы игроки точно увидели
                for (int i = 0; i < 3; i++) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(ChatColor.GREEN + "Следующая арена начнётся в " + formattedDate + "!");
                    }
                }
            }
        };
        chatTask.run();
    }

    /**
     * Устанавливаем время для следующей арены (интервалом).
     */
    public void scheduleNextArena() {
        final long intervalHours = plugin.getConfig().getLong("arena.intervalHours", 24);
        final long intervalMillis = intervalHours * 60L * 60L * 1000L;
        final long localNextArenaTime = System.currentTimeMillis() + intervalMillis;

        scheduleNextArena(localNextArenaTime);
    }

    /**
     * Запуск (фактический) арены.
     */
    public void startArena() {
        // Если арена уже активна, выходим
        if (isArenaActive) {
            return;
        }
        isArenaActive = true;

        // Если никто не зашёл или осталась одна команда, завершаем сразу
        if (isOneTeamRemaining() || arenaPlayers.isEmpty()) {
            arenaPlayers.clear();
            finishArena();
            return;
        }

        // Получаем нужную арену из списка
        final int index = currentArenaIndex - 1;
        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) {
            --currentArenaIndex;
            finishArena();
            return;
        }

        try {
            // Сохраняем местоположения и телепортируем игроков на арену, а так же проигрываем им звук начала арены
            final Iterator<Player> iterator = arenaPlayers.iterator();
            while (iterator.hasNext()) {
                final Player player = iterator.next();
                if (player.isDead()) {
                    player.sendMessage(ChatColor.RED + "Вы мертвы, поэтому не будете телепортированы на арену.");
                    iterator.remove();
                } else {
                    playerLocations.put(player, player.getLocation());
                    final String team = plugin.getGame().getPlayerTeam(player.getName());
                    if ("RED".equals(team)) {
                        player.teleport(currentArena.getSpawnRed());
                    } else {
                        player.teleport(currentArena.getSpawnBlue());
                    }
                    player.playSound(player.getLocation(), "dota.match_ready", 1.0F, 1.0F);
                }
            }
        }
        catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "Не удаётся загрузить из памяти мир арены. Полетел плагин Multiverse-Core.");
            --currentArenaIndex;
            finishArena();
            return;
        }

        final int arenaDurationSeconds = plugin.getConfig().getInt("arena.durationSeconds", 300);
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

                // Последние 59 секунд — дополнительный урон
                if (countdown > 0 && countdown <= 59) {
                    for (Player p : arenaPlayers) {
                        try {
                            p.sendMessage(ChatColor.RED + "До конца арены осталось секунд: " + countdown
                                    + ". Всем живым наносится урон!");
                            AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            if (maxHealth != null) {
                                double health = Math.max(p.getHealth() - maxHealth.getValue() * 0.1, 1);
                                p.setHealth(health);
                                p.damage(1);
                            }
                        } catch (Exception ignored) { }
                    }
                }

                // Каждую минуту (или 0) информируем, что время скоро истечёт
                if (countdown % 60 == 0) {
                    if (countdown == 0) {
                        for (Player p : arenaPlayers) {
                            p.sendMessage(ChatColor.GREEN + "Арена завершается!");
                        }
                    } else {
                        for (Player p : arenaPlayers) {
                            p.sendMessage(ChatColor.RED + "До конца арены осталось секунд: " + countdown);
                        }
                    }
                }

                countdown--;
            }
        };
        arenaTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Проверяем, осталась ли только одна команда (или никого).
     */
    private boolean isOneTeamRemaining() {
        final Set<String> remainingTeams = new HashSet<>();
        for (Player p : arenaPlayers) {
            final String team = plugin.getGame().getPlayerTeam(p.getName());
            remainingTeams.add(team);
        }
        return remainingTeams.size() <= 1;
    }

    /**
     * Завершение арены. Подсчитываем победителя, даём очки, телепортируем игроков обратно и копируем арену.
     */
    public void finishArena() {
        if (!isArenaActive) {
            return;
        }
        isArenaActive = false;

        String winningTeam = null;
        if (isOneTeamRemaining()) {
            // Если осталась хоть одна команда
            winningTeam = arenaPlayers.stream()
                    .map(player -> plugin.getGame().getPlayerTeam(player.getName()))
                    .findFirst()
                    .orElse(null);
        }

        if (winningTeam != null) {
            for (Player player : arenaPlayers) {
                final String team = plugin.getGame().getPlayerTeam(player.getName());
                if (winningTeam.equals(team)) {
                    player.sendMessage(ChatColor.GREEN + "Ваша команда победила на арене!");
                } else {
                    player.sendMessage(ChatColor.RED + "Ваша команда проиграла на арене.");
                }
            }
            // Начисляем победившей команде очки
            plugin.getGame().addScore(winningTeam, winScore);
        } else {
            for (Player p : arenaPlayers) {
                p.sendMessage(ChatColor.YELLOW + "Арена закончилась ничьей!");
            }
        }

        scheduleNextArena();

        final BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Возвращаем игроков на исходное место
                for (Player player : arenaPlayers) {
                    final Location originalLocation = playerLocations.get(player);
                    if (originalLocation != null) {
                        player.teleport(originalLocation);
                    }
                }
                playerLocations.clear();
                arenaPlayers.clear();

                // Восстанавливаем арену (копируем из copyWorld)
                copyArena();

                // Переходим к следующей арене
                currentArenaIndex++;
                if (currentArenaIndex > arenasAmount) {
                    currentArenaIndex = 1;
                }
                plugin.getConfig().set("arena.currentArenaIndex", currentArenaIndex);

                checkAndScheduleArena();
            }
        };
        // Делаем задержку в 30 секунд (30*20 тиков)
        task.runTaskLater(plugin, 30 * 20L);
    }

    /**
     * Копируем арену из мира-копии в боевой мир.
     */
    public void copyArena() {
        final int index = currentArenaIndex - 1;
        if (index < 0 || index >= arenas.size()) return;

        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) return;

        final World arenaWorld = currentArena.getWorld();
        if (arenaWorld == null) return;

        // Удаляем мобов (не игроков и не двери с люками и кроватями) перед копированием блоков
        for (Entity entity : arenaWorld.getEntities()) {
            if (!(entity instanceof Player) && !(entity instanceof Bed) &&
                    !(entity instanceof Door) && !(entity instanceof TrapDoor)) {
                entity.remove();
            }
        }

        final World copyWorld = currentArena.getCopyWorld();
        if (copyWorld == null) return;

        final Location start = currentArena.getCopyStart();
        final Location end   = currentArena.getCopyEnd();

        final int x1 = start.getBlockX();
        final int y1 = start.getBlockY();
        final int z1 = start.getBlockZ();
        final int x2 = end.getBlockX();
        final int y2 = end.getBlockY();
        final int z2 = end.getBlockZ();

        final int minX = Math.min(x1, x2);
        final int minY = Math.min(y1, y2);
        final int minZ = Math.min(z1, z2);
        final int maxX = Math.max(x1, x2);
        final int maxY = Math.max(y1, y2);
        final int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Location sourceLoc = new Location(copyWorld, x, y, z);
                    final Location targetLoc = new Location(arenaWorld, x, y, z);

                    final Block sourceBlock = sourceLoc.getBlock();
                    final Block targetBlock = targetLoc.getBlock();

                    targetBlock.setType(sourceBlock.getType());
                    targetBlock.setBlockData(sourceBlock.getBlockData());
                }
            }
        }

        // Дополнительное удаление всех неигроковых сущностей после копирования арены
        // Удаляем мобов (не игроков и не двери с люками и кроватями) перед копированием блоков
        for (Entity entity : arenaWorld.getEntities()) {
            if (!(entity instanceof Player) && !(entity instanceof Bed) &&
                    !(entity instanceof Door) && !(entity instanceof TrapDoor)) {
                entity.remove();
            }
        }
    }

    // -------------------- Методы / вспомогательные --------------------

    public boolean isPlayerAlreadyInArena(final Player player) {
        return arenaPlayers.contains(player);
    }

    public void addPlayerToArena(final Player player) {
        // Если арена активна — добавить уже нельзя
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

    /**
     * Отменяет и обнуляет задачу (если она была запущена).
     */
    public void clearTask(BukkitRunnable task) {
        if (task != null) {
            try {
                if (task.getTaskId() != -1 && !task.isCancelled()) {
                    task.cancel();
                }
            } catch (IllegalStateException ignored) {
                // Задача ещё не была запланирована
            }
            task = null;
        }
    }

    // -------------------- Обработчики событий --------------------

    /**
     * Загрузка мира — проверяем, нет ли арены в нём.
     */
    @EventHandler
    public void onWorldLoad(final WorldLoadEvent event) {
        final World loadedWorld = event.getWorld();

        final String worldName = loadedWorld.getName();
        // Пробегаем по "arena_1", "arena_2" и т.д.
        for (int i = 1; i <= arenasAmount; i++) {
            final ConfigurationSection section = plugin.getConfig().getConfigurationSection("arena_" + i);
            if (section == null) continue;

            final String arenaWorldName = section.getString("world");
            if (worldName.equals(arenaWorldName)) {
                final ArenaConfig arenaConfig = new ArenaConfig(section);
                if (arenas.stream()
                        .map(ArenaConfig::getWorld)
                        .noneMatch(world -> world != null && world.getName().equals(arenaConfig.getWorld().getName()))) {
                    arenas.add(arenaConfig);
                }
            }
        }
    }

    /**
     * Игрок вышел из игры.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Если игрок в арене и арена активна
        if (isArenaActive && arenaPlayers.contains(player)) {
            arenaPlayers.remove(player);

            // Если осталась одна команда — завершаем
            if (isOneTeamRemaining()) {
                finishArena();
            }

            // Возвращаем игрока на его исходное положение
            final Location originalLocation = playerLocations.get(player);
            if (originalLocation != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(originalLocation));
            }
        }
    }

    /**
     * Перемещает игрока с арены в обычный мир на его точку возрождения.
     * Если у игрока нет точки возрождения (кровати), телепортирует на спавн основного мира.
     */
    private void teleportPlayerFromArena(final Player player) {
        // Проходим по всем конфигурациям арен и проверяем, находится ли игрок в мире одной из арен
        for (int i = 1; i <= arenasAmount; i++) {
            final int index = i - 1;
            if (index < 0 || index >= arenas.size()) continue;

            final ArenaConfig arenaConfig = arenas.get(index);
            if (arenaConfig == null) continue;

            // Если игрок в мире арены
            if (player.getWorld().equals(arenaConfig.getWorld())) {
                // Проверяем наличие точки возрождения
                if (player.getBedSpawnLocation() == null) {
                    final World normalWorld = Bukkit.getServer().getWorld("world");
                    if (normalWorld == null) {
                        player.kickPlayer("Невозможно переместить вас в обычный мир с арены (ошибка сервера). Пишите админу и не абузьте.");
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(normalWorld.getSpawnLocation()));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(player.getBedSpawnLocation()));
                }
                player.sendMessage(ChatColor.YELLOW + "Вы появились в мире арены и были перемещены на свою точку возрождения.");
                return; // Завершаем после успешного перемещения
            }
        }
    }

    /**
     * Игрок зашёл на сервер.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        teleportPlayerFromArena(player);
    }

    /**
     * Дополнительная проверка, чтобы человек не мог возродиться на арене (ни во время арены, ни вообще)
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        // Если игрок пытается возродиться на арене, перемещаем его в обычный мир
        teleportPlayerFromArena(player);
    }

    /**
     * Игрок умер (возможно, на арене).
     */
    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player victim = event.getEntity();

        // Если есть убийца (PVP) и они из разных команд, даём очки
        final Player attacker = victim.getKiller();
        if (attacker != null && !attacker.equals(victim)) {
            if (!plugin.getGame().areTwoPlayersInTheSameTeam(attacker.getName(), victim.getName())) {
                final String attackerTeam = plugin.getGame().getPlayerTeam(attacker.getName());
                plugin.getGame().addScore(attackerTeam, killScore);
            }
        }

        final int index = currentArenaIndex - 1;
        if (index < 0 || index >= arenas.size()) return;

        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) return;

        // Если игрок умер в мире арены
        if (victim.getWorld().equals(currentArena.getWorld())) {
            arenaPlayers.remove(victim);

            if (isArenaActive) {
                victim.sendMessage(ChatColor.RED + "Вы погибли и были исключены из арены.");
            }

            // Телепортируем назад
            final Location originalLocation = playerLocations.get(victim);
            if (originalLocation != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        victim.sendMessage(ChatColor.GREEN + "Вы были телепортированы обратно.");
                        Bukkit.getScheduler().runTask(plugin, () -> victim.teleport(originalLocation));
                    }
                }.runTaskLater(plugin, 20L);
            }

            // Если осталась только одна команда — завершаем арену
            if (isArenaActive && isOneTeamRemaining()) {
                finishArena();
            }
        }
    }

    /**
     * Игрок ломает блок. Если это блок арены (не воздух), запрещаем выпадение.
     */
    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location loc = block.getLocation();
        if (loc.getWorld() == null) return;

        final int index = currentArenaIndex - 1;
        if (index < 0 || index >= arenas.size()) return;

        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) return;

        // Если это блок из оригинальной арены и он не воздух — запрещаем выпадение
        if (currentArena.getWorld() != null && loc.getWorld().getName().equals(currentArena.getWorld().getName())) {
            final Map<Location, Material> originalBlocks = currentArena.getOriginalArenaBlocks();
            if (originalBlocks.containsKey(loc) && originalBlocks.get(loc) != Material.AIR &&
                    originalBlocks.get(loc) == block.getType()) {
                event.setDropItems(false);
            }
        }
    }

    /**
     * Обработка взрыва, у которого неизвестен источник (BlockExplodeEvent).
     * Нужно убрать ломание для блоков, принадлежащих арене.
     */
    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent event) {
        final int index = currentArenaIndex - 1;
        if (index < 0 || index >= arenas.size()) return;

        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) return;

        final World arenaWorld = currentArena.getWorld();
        if (arenaWorld == null) return;

        // Если взрыв происходит не в мире арены — выходим
        if (!event.getBlock().getWorld().equals(arenaWorld)) {
            return;
        }

        final Map<Location, Material> originalBlocks = currentArena.getOriginalArenaBlocks();

        // Удаляем из blockList() все блоки, которые принадлежат оригинальной арене
        event.blockList().removeIf(block ->
                originalBlocks.containsKey(block.getLocation())
                        && originalBlocks.get(block.getLocation()) != Material.AIR
        );
    }

    /**
     * Обработка взрыва, у которого известен источник (EntityExplodeEvent).
     * Аналогично убираем ломание для блоков, принадлежащих арене.
     */
    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent event) {
        final int index = currentArenaIndex - 1;
        if (index < 0 || index >= arenas.size()) return;

        final ArenaConfig currentArena = arenas.get(index);
        if (currentArena == null) return;

        final World arenaWorld = currentArena.getWorld();
        if (arenaWorld == null) return;

        // Если взрыв происходит не в мире арены — выходим
        if (event.getLocation().getWorld() == null || !event.getLocation().getWorld().equals(arenaWorld)) {
            return;
        }

        final Map<Location, Material> originalBlocks = currentArena.getOriginalArenaBlocks();

        // Удаляем из blockList() все блоки, которые принадлежат оригинальной арене
        event.blockList().removeIf(block ->
                originalBlocks.containsKey(block.getLocation())
                        && originalBlocks.get(block.getLocation()) != Material.AIR
        );
    }

    /**
     * Обработка, чтобы нельзя было использовать кровати и якоря возрождения на арене, и тем самым не появляться
     * после самой арены на ней через время
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block  block  = event.getClickedBlock();
        if (block == null) return;

        // Проверяем, является ли блок кроватью или якорем возрождения
        final Material type = block.getType();
        if (!(type.name().endsWith("BED") || type == Material.RESPAWN_ANCHOR)) {
            return;
        }

        // Проверяем, находится ли игрок в мире одной из арен
        final World playerWorld = player.getWorld();
        boolean inArenaWorld = arenas.stream()
                .map(ArenaConfig::getWorld)
                .anyMatch(world -> world.equals(playerWorld));

        if (!inArenaWorld) return;

        // Отменяем взаимодействие и уведомляем игрока
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Вы не можете использовать кровати и якоря возрождения на арене!");
    }

    /**
     * Если портал создаётся на арене, то отменяем его
     */
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        // Проверяем, находится ли игрок в мире одной из арен
        final World thisWorld = event.getWorld();
        boolean inArenaWorld = arenas.stream()
                .map(ArenaConfig::getWorld)
                .anyMatch(world -> world != null && world.getName().equals(thisWorld.getName()));

        // Если это не мир арены, то не надо ничего делать
        if (!inArenaWorld) return;

        // Отменяем создание портала, если это сделано на арене
        event.setCancelled(true);
    }
}
