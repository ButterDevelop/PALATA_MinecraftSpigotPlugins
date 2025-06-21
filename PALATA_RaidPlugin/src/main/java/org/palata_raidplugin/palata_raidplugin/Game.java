package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;

import java.util.*;

public class Game {

    // ------------------------//
    //       Поля класса       //
    // ------------------------//

    private final Map<String, Location> teamBases = new HashMap<>();
    private final Map<String, ArrayList<String>> teamMembers = new HashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();
    private final Map<String, MyTeam> teams = new HashMap<>();
    public  final List<Player> raidPlayers = new ArrayList<>(); // Игроки, присоединившиеся к рейду

    private final Plugin plugin;
    private final Scoreboard scoreboard;

    private boolean isRaidOpen = false;
    private boolean isRaidActive = false;
    private boolean isRaidStarted = false;
    private boolean isEnabled;
    private boolean isDelayBegunAfterRaid = false;

    public String raidingTeam = null;            // Команда, которая в настоящее время проводит рейд
    public String lastRaidOpenedTheTeam = null;  // Последняя команда, которая начала рейд

    private final Map<String, String> teamCaptains = new HashMap<>(); // Капитаны команд

    private int obsidianDestroyed = 0;
    private final int raidWinScore;
    private long raidStartTime = 0L;

    private double privateRadiusRaid = 10;
    private double privateRadiusHome = 10;
    private double endWorldMainIslandRadius = 10;

    // В каких мирах PvP всегда разрешён
    public List<String> alwaysAllowedPvPWorlds = new ArrayList<>();

    // ------------------------//
    //       Конструктор       //
    // ------------------------//

    public Game(Plugin _plugin) {
        this.plugin = _plugin;

        // Загружаем общие настройки
        this.isEnabled = plugin.getConfig().getBoolean("plugin.raid.isEnabled");
        this.privateRadiusRaid = plugin.getConfig().getDouble("plugin.raid.privateRadiusRaid");
        this.privateRadiusHome = plugin.getConfig().getDouble("plugin.raid.privateRadiusHome");
        this.endWorldMainIslandRadius = plugin.getConfig().getDouble("plugin.raid.endWorldMainIslandRadius");
        this.raidWinScore = plugin.getConfig().getInt("plugin.raid.winScore", 10);

        // Инициализируем Scoreboard
        final ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager != null ? manager.getNewScoreboard() : Bukkit.getScoreboardManager().getNewScoreboard();

        // Инициализируем команды, базы, капитанов и т.д.
        initializeTeams();
    }

    // ------------------------//
    //  Геттеры / Сеттеры и т.д.
    // ------------------------//

    public boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean value) {
        isEnabled = value;
        plugin.getConfig().set("plugin.raid.isEnabled", value);
        plugin.saveConfig();
    }

    public boolean isRaidOpen() {
        return isRaidOpen;
    }

    public boolean isRaidActive() {
        return isRaidActive;
    }

    public boolean isRaidStarted() {
        return isRaidStarted;
    }

    public boolean isDelayBegunAfterRaid() {
        return isDelayBegunAfterRaid;
    }

    public String getLastRaidOpenedTheTeam() {
        return lastRaidOpenedTheTeam;
    }

    public List<Player> getRaidPlayers() {
        return raidPlayers;
    }

    // ------------------------//
    //         Методы          //
    // ------------------------//

    /**
     * Проверяем, есть ли у команды база.
     */
    public boolean hasBase(String team) {
        return teamBases.containsKey(team);
    }

    /**
     * Получаем базу команды.
     */
    public Location getBase(String team) {
        return teamBases.get(team);
    }

    /**
     * Устанавливаем базу команды.
     */
    public void setBase(String team, Location loc) {
        teamBases.put(team, loc);
    }

    /**
     * Открываем рейд для команды.
     */
    public void openRaid(String team) {
        isRaidOpen = true;
        lastRaidOpenedTheTeam = team;
        obsidianDestroyed = 0;
        raidPlayers.clear();

        final int openDurationMinutes = getOpenRaidDurationMinutes();
        for (Player player : getTeamPlayers(team)) {
            player.sendMessage(ChatColor.GREEN + "Рейд был открыт! Присоединяйтесь к нему с помощью /joinraid");
            player.sendMessage(ChatColor.YELLOW + "Осталось времени для присоединения: " + openDurationMinutes + " минут.");
            player.sendMessage(ChatColor.YELLOW + "Если капитан команды за это время не напишет /startraid, то рейд будет автоматически отменён.");
        }
    }

    /**
     * Начало процедуры старта рейда с задержкой.
     */
    public void startRaid(String attackingTeam) {
        final int delayMinutes = getRaidDelayMinutes();
        isRaidStarted = true;

        final BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            // Проверяем, можем ли мы начать рейд
            if (canRaid(attackingTeam)) {
                obsidianDestroyed = 0;
                isRaidActive = true;
                raidStartTime = System.currentTimeMillis();

                // Собираем не полностью готовый нексус у защищающейся команды
                buildNotFullNexus(getDefendingTeam(attackingTeam));

                // Фактический запуск рейда
                startRaidDelayed(attackingTeam);

            } else {
                isRaidActive = false;
                isRaidOpen = false;
                isRaidStarted = false;
                for (Player player : raidPlayers) {
                    player.sendMessage(ChatColor.RED + "Рейд не может быть начат: " +
                            "либо недостаточно игроков у другой команды, " +
                            "либо у другой команды отсутствует Нексус.");
                }
            }
        }, (long) delayMinutes * 60 * 20L);
    }

    /**
     * Запуск рейда после задержки.
     */
    public void startRaidDelayed(String attackingTeam) {
        Bukkit.broadcastMessage(ChatColor.GREEN + "Рейд начался! Команда '" + attackingTeam + "' является нападающей.");

        raidingTeam = attackingTeam;
        final String defendingTeam = getDefendingTeam(attackingTeam);
        final Location nexusLocation = getNexusLocation(defendingTeam);
        final Block nexusBlock = nexusLocation.getBlock();

        new BukkitRunnable() {
            @Override
            public void run() {
                final int requiredDestroyCount = getRequiredDestroyCount();
                final int raidDurationMinutes = getRaidDurationMinutes();
                final long raidEndTime = raidStartTime + ((long) raidDurationMinutes * 60 * 1000);

                // Если блок нексуса сломан (AIR), восстанавливаем его
                if (nexusBlock.getType() == Material.AIR) {
                    nexusBlock.setType(Material.OBSIDIAN);
                    final String message = "Часть нексуса команды '" + defendingTeam
                            + "' уничтожена! Осталось уничтожить "
                            + (requiredDestroyCount - obsidianDestroyed) + " раз!";
                    for (Player p : getTeamPlayers(attackingTeam)) {
                        p.sendMessage(ChatColor.GREEN + message);
                    }
                    for (Player p : getTeamPlayers(defendingTeam)) {
                        p.sendMessage(ChatColor.RED + message);
                    }
                }

                // Если нексус уничтожен нужное кол-во раз или время вышло — завершаем рейд
                if (obsidianDestroyed >= requiredDestroyCount || System.currentTimeMillis() >= raidEndTime) {
                    if (obsidianDestroyed < requiredDestroyCount) {
                        endRaid();
                        Bukkit.broadcastMessage(ChatColor.RED + "Время рейда истекло! Команде '"
                                + attackingTeam + "' не удалось завершить рейд. Нексус команды '"
                                + defendingTeam + "' уничтожен " + obsidianDestroyed + " раз.");
                        addScore(defendingTeam, raidWinScore);
                    } else {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "Команда '" + attackingTeam
                                + "' успешно завершила рейд! Нексус команды '"
                                + defendingTeam + "' уничтожен " + obsidianDestroyed + " раз.");
                        addScore(attackingTeam, raidWinScore);
                    }

                    isDelayBegunAfterRaid = true;
                    final int minutesPrivateDelayAfterRaid = plugin.getConfig().getInt("plugin.raid.minutesPrivateDelayAfterRaid");
                    final BukkitScheduler scheduler = Bukkit.getScheduler();
                    scheduler.runTaskLater(plugin, () -> isDelayBegunAfterRaid = false,
                            (long) minutesPrivateDelayAfterRaid * 60 * 20L);

                    this.cancel();

                    raidingTeam = null;
                    isRaidActive = false;
                    buildFullNexus(defendingTeam);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Завершает рейд (общая логика).
     */
    public void endRaid() {
        isRaidOpen = false;
        isRaidActive = false;
        isRaidStarted = false;

        // Безопасная проверка: если кто-то в рейде есть
        if (!raidPlayers.isEmpty()) {
            final String teamName = getPlayerTeam(raidPlayers.get(0).getName());
            buildFullNexus(teamName);

            final long currentTimestamp = System.currentTimeMillis();
            plugin.getConfig().set(teamName + ".lastRaid", currentTimestamp);
            plugin.saveConfig();
        }

        for (Player player : raidPlayers) {
            player.sendMessage(ChatColor.YELLOW + "Рейд был закончен.");
        }
        raidPlayers.clear();
    }

    /**
     * Добавляет игрока к рейду (если он ещё не в списке).
     */
    public void addPlayerToRaid(Player player) {
        if (!raidPlayers.contains(player)) {
            raidPlayers.add(player);
        }
    }

    /**
     * Отменяет рейд.
     */
    public void cancelRaid(String team, boolean wasCancelledByPlayer) {
        if (!isRaidStarted && !isRaidActive && isRaidOpen) {
            isRaidOpen = false;
            for (Player p : getTeamPlayers(team)) {
                if (wasCancelledByPlayer) {
                    p.sendMessage(ChatColor.RED + "Рейд команды был отменён её капитаном!");
                } else {
                    p.sendMessage(ChatColor.RED + "Рейд команды был отменён! Истекло время!");
                }
            }
        }
    }

    // ------------------------//
    //   Построение / снос
    //       структур
    // ------------------------//

    /**
     * Построить полный (завершённый) нексус.
     */
    public void buildFullNexus(String team) {
        final Location nexusLocation = getNexusLocation(team);
        if (nexusLocation == null) return;

        final World world = nexusLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            nexusLocation.getBlockX() + x,
                            nexusLocation.getBlockY() + y,
                            nexusLocation.getBlockZ() + z
                    );
                    // Барьер по осям (крест)
                    if ((x == 0 && y == 0 && Math.abs(z) == baseRadius) ||
                            (y == 0 && Math.abs(x) == baseRadius && z == 0) ||
                            (z == 0 && x == 0 && Math.abs(y) == baseRadius)) {
                        block.setType(Material.BARRIER);
                    }
                    // Центральный блок — обсидиан
                    else if (x == 0 && y == 0 && z == 0) {
                        block.setType(Material.OBSIDIAN);
                    }
                    // В остальных местах — бедрок, кроме углов (куб 3x3x3 без углов)
                    else if (!(Math.abs(x) == baseRadius && Math.abs(y) == baseRadius && Math.abs(z) == baseRadius)) {
                        block.setType(Material.BEDROCK);
                    }
                }
            }
        }
    }

    /**
     * Построить не полностью готовый нексус (без барьеров).
     */
    public void buildNotFullNexus(String team) {
        final Location nexusLocation = getNexusLocation(team);
        if (nexusLocation == null) return;

        final World world = nexusLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            nexusLocation.getBlockX() + x,
                            nexusLocation.getBlockY() + y,
                            nexusLocation.getBlockZ() + z
                    );
                    // Убираем барьер — ставим воздух
                    if ((x == 0 && y == 0 && Math.abs(z) == baseRadius) ||
                            (y == 0 && Math.abs(x) == baseRadius && z == 0) ||
                            (z == 0 && x == 0 && Math.abs(y) == baseRadius)) {
                        block.setType(Material.AIR);
                    }
                    // Центральный блок — обсидиан
                    else if (x == 0 && y == 0 && z == 0) {
                        block.setType(Material.OBSIDIAN);
                    }
                }
            }
        }
    }

    /**
     * Удалить полностью (убрать) нексус.
     */
    public void removeFullNexus(Location nexusLocation) {
        if (nexusLocation == null) return;
        final World world = nexusLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            nexusLocation.getBlockX() + x,
                            nexusLocation.getBlockY() + y,
                            nexusLocation.getBlockZ() + z
                    );
                    block.setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Построить полный дом (по аналогии с нексусом).
     */
    public void buildFullHome(String team, String worldName) {
        final Location homeLocation = getHomeLocation(team, worldName);
        if (homeLocation == null) return;

        final World world = homeLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            homeLocation.getBlockX() + x,
                            homeLocation.getBlockY() + y,
                            homeLocation.getBlockZ() + z
                    );
                    block.setType(Material.BEDROCK);
                }
            }
        }
    }

    /**
     * Построить полную ферму (по аналогии с домом).
     */
    public void buildFullFarm(String team, String worldName) {
        final Location farmLocation = getFarmLocation(team, worldName);
        if (farmLocation == null) return;

        final World world = farmLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            farmLocation.getBlockX() + x,
                            farmLocation.getBlockY() + y,
                            farmLocation.getBlockZ() + z
                    );
                    block.setType(Material.BEDROCK);
                }
            }
        }
    }

    /**
     * Удалить полностью (убрать) дом.
     */
    public void removeFullHome(Location homeLocation) {
        if (homeLocation == null) return;
        final World world = homeLocation.getWorld();
        final int baseRadius = 1;
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    final Block block = world.getBlockAt(
                            homeLocation.getBlockX() + x,
                            homeLocation.getBlockY() + y,
                            homeLocation.getBlockZ() + z
                    );
                    block.setType(Material.AIR);
                }
            }
        }
    }

    // ------------------------//
    //   Методы Nexus / Home   //
    // ------------------------//

    /**
     * Получаем локацию нексуса по имени команды.
     */
    public Location getNexusLocation(String team) {
        if (team == null) return null;

        final String worldName = plugin.getConfig().getString(team + ".nexus.world");
        if (worldName == null) return null;

        final World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        final double x = plugin.getConfig().getDouble(team + ".nexus.x");
        final double y = plugin.getConfig().getDouble(team + ".nexus.y");
        final double z = plugin.getConfig().getDouble(team + ".nexus.z");

        return new Location(world, x, y, z);
    }

    public Location getHomeLocation(String team, String worldName) {
        if (team == null || worldName == null) return null;

        final World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        if (!plugin.getConfig().isSet(team + "." + worldName + ".home.x")) {
            return null;
        }

        final double x = plugin.getConfig().getDouble(team + "." + worldName + ".home.x");
        final double y = plugin.getConfig().getDouble(team + "." + worldName + ".home.y");
        final double z = plugin.getConfig().getDouble(team + "." + worldName + ".home.z");

        return new Location(world, x, y, z);
    }

    public Location getFarmLocation(String team, String worldName) {
        if (team == null || worldName == null) return null;

        final World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        if (!plugin.getConfig().isSet(team + "." + worldName + ".farm.x")) {
            return null;
        }

        final double x = plugin.getConfig().getDouble(team + "." + worldName + ".farm.x");
        final double y = plugin.getConfig().getDouble(team + "." + worldName + ".farm.y");
        final double z = plugin.getConfig().getDouble(team + "." + worldName + ".farm.z");

        return new Location(world, x, y, z);
    }

    public boolean isBlockInNexus(Location blockLocation, String team) {
        final Location nexusLocation = getNexusLocation(team);
        return nexusLocation != null && nexusLocation.equals(blockLocation);
    }

    public boolean isBlockInHome(Location blockLocation, String team) {
        if (blockLocation == null) return false;
        final Location homeLocation = getHomeLocation(team, blockLocation.getWorld().getName());
        return homeLocation != null && homeLocation.equals(blockLocation);
    }

    // ------------------------//
    //   Логика рейда / проверки
    // ------------------------//

    /**
     * Проверяем, можно ли начать рейд.
     */
    public boolean canRaid(String teamName) {
        if (raidingTeam != null || isRaidActive()) {
            return false;
        }
        final MyTeam team = teams.get(teamName);
        if (team == null) {
            throw new IllegalArgumentException("Team " + teamName + " does not exist.");
        }

        final int minPlayers = plugin.getConfig().getInt("plugin.raid.minPlayers");
        final int differencePlayersCount = plugin.getConfig().getInt("plugin.raid.differencePlayersCount");
        final String defendingTeam = getDefendingTeam(teamName);

        if (defendingTeam == null) {
            return false;
        }

        final int attackersOnline = raidPlayers.size();
        final int defendersOnline = getOnlineTeamPlayers(defendingTeam).size();

        // Разница в игроках не должна превышать differencePlayersCount,
        // у защиты тоже должно быть нужное кол-во игроков
        return (attackersOnline >= minPlayers)
                && (defendersOnline >= minPlayers)
                && (attackersOnline - defendersOnline <= differencePlayersCount)
                && (getNexusLocation(defendingTeam) != null);
    }

    /**
     * Возвращает имя команды-защитника, если есть две команды: RED / BLUE.
     */
    public String getDefendingTeam(String attackingTeam) {
        if ("RED".equals(attackingTeam)) {
            return "BLUE";
        } else if ("BLUE".equals(attackingTeam)) {
            return "RED";
        }
        return null;
    }

    /**
     * Проверяем, жив ли дракон в Энде.
     */
    public boolean isDragonAlive() {
        final World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) return false;

        for (Entity entity : endWorld.getEntities()) {
            if (entity instanceof EnderDragon) {
                return true;
            }
        }
        return false;
    }

    // ------------------------//
    //   Работа с конфигом и Scoreboard
    // ------------------------//

    /**
     * Обновляет счёт команд на Scoreboard.
     */
    public void updateScoreboard() {
        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            objective = scoreboard.registerNewObjective("teamscore", "dummy", "Очки команд");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            String teamName = entry.getKey();
            final int score = entry.getValue();

            if ("red".equalsIgnoreCase(teamName)) {
                teamName = ChatColor.RED + teamName;
            } else if ("blue".equalsIgnoreCase(teamName)) {
                teamName = ChatColor.BLUE + teamName;
            }

            Score teamScore = objective.getScore(teamName);
            teamScore.setScore(score);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Добавляет очки команде.
     */
    public void addScore(String team, int points) {
        teamScores.putIfAbsent(team, 0);
        teamScores.put(team, teamScores.get(team) + points);

        saveTeamScores();
        updateScoreboard();
    }

    /**
     * Получаем очки команды.
     */
    public int getScore(String team) {
        return teamScores.getOrDefault(team, 0);
    }

    /**
     * Получаем сам scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    // ------------------------//
    //      Разные методы
    // ------------------------//

    /**
     * Инкремент количества разрушенного обсидиана.
     */
    public void incrementObsidianDestroyed() {
        obsidianDestroyed++;
    }

    public int getObsidianDestroyed() {
        return obsidianDestroyed;
    }

    public int getRequiredDestroyCount() {
        return plugin.getConfig().getInt("plugin.raid.requiredDestroyCount");
    }

    public int getRaidDelayMinutes() {
        return plugin.getConfig().getInt("plugin.raid.delay");
    }

    public int getRaidDurationMinutes() {
        return plugin.getConfig().getInt("plugin.raid.durationMinutes");
    }

    public int getOpenRaidDurationMinutes() {
        return plugin.getConfig().getInt("plugin.raid.openDurationMinutes");
    }

    public double getEndWorldMainIslandRadius() {
        return endWorldMainIslandRadius;
    }

    /**
     * Получение списка игроков команды.
     */
    public List<Player> getTeamPlayers(String team) {
        final List<String> playerNames = teamMembers.getOrDefault(team, new ArrayList<>());
        final List<Player> players = new ArrayList<>();
        for (String playerName : playerNames) {
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null) {
                players.add(onlinePlayer);
            }
        }
        return players;
    }

    /**
     * Получение списка онлайн-игроков команды.
     */
    public List<Player> getOnlineTeamPlayers(String team) {
        final List<String> playerNames = teamMembers.getOrDefault(team, new ArrayList<>());
        final List<Player> players = new ArrayList<>();
        for (String playerName : playerNames) {
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                players.add(onlinePlayer);
            }
        }
        return players;
    }

    /**
     * Возвращает команду игрока по его имени.
     */
    public String getPlayerTeam(String playerName) {
        for (Map.Entry<String, ArrayList<String>> entry : teamMembers.entrySet()) {
            if (entry.getValue().contains(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Проверка: оба игрока в одной команде?
     */
    public boolean areTwoPlayersInTheSameTeam(String playerName1, String playerName2) {
        final String team1 = getPlayerTeam(playerName1);
        final String team2 = getPlayerTeam(playerName2);

        // Если оба игрока не состоят в команде, считаем их "в одной команде" (обычно это не так, но логика может зависеть от вас)
        if (team1 == null && team2 == null) {
            return true;
        }
        return team1 != null && team1.equals(team2);
    }

    /**
     * Ищем безопасную локацию поблизости.
     */
    public Location getSafeLocation(Location playerLocation) {
        final int searchRadius = 10;
        if (isLocationSafe(playerLocation)) {
            return playerLocation;
        }

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    final Location checkLoc = playerLocation.clone().add(x, y, z);
                    if (isLocationSafe(checkLoc)) {
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Телепортирует игрока в безопасное место.
     */
    public void teleportPlayerToSafePosition(Player player) {
        final Location playerLocation = player.getLocation();
        final Location safeLocation = getSafeLocation(playerLocation);
        if (safeLocation != null) {
            player.teleport(safeLocation);
            player.sendMessage(ChatColor.YELLOW + "Вас переместили в безопасное место для постройки Нексуса.");
        }
    }

    /**
     * Проверяет, является ли локация безопасной для перемещения.
     */
    private boolean isLocationSafe(Location location) {
        final Block block = location.getBlock();

        // Проверяем, что блок и блок над ним — воздух
        if (block.getType() != Material.AIR ||
                block.getRelative(BlockFace.UP).getType() != Material.AIR) {
            return false;
        }

        // Проверяем, что блок под ногами — не воздух и не лава
        final Material below = block.getRelative(BlockFace.DOWN).getType();
        if (below == Material.AIR || below == Material.LAVA) {
            return false;
        }

        return true;
    }

    /**
     * Устанавливает игрока как капитана для указанной команды.
     */
    public void setCaptain(String team, Player player) {
        teamCaptains.put(team, player.getName());
    }

    /**
     * Проверяет, является ли данный игрок капитаном указанной команды.
     */
    public boolean isCaptain(String team, Player player) {
        final String captainName = teamCaptains.get(team);
        return captainName != null && captainName.equals(player.getName());
    }

    /**
     * Получаем имя капитана для команды.
     */
    public String getCaptainName(String team) {
        return teamCaptains.get(team);
    }

    /**
     * Проверяем, находится ли локация в радиусе нексуса.
     */
    public boolean isWithinNexusRadius(Location actionLocation, String team) {
        final Location nexusLocation = getNexusLocation(team);
        if (nexusLocation == null || actionLocation == null) return false;
        if (!actionLocation.getWorld().equals(nexusLocation.getWorld())) return false;

        return isWithin2DRadius(actionLocation, nexusLocation, privateRadiusRaid);
    }

    /**
     * Проверяем, находится ли локация в радиусе дома.
     */
    public boolean isWithinHomeRadius(Location actionLocation, String team) {
        if (actionLocation == null) return false;

        final Location homeLocation = getHomeLocation(team, actionLocation.getWorld().getName());
        if (homeLocation == null) return false;
        if (!actionLocation.getWorld().equals(homeLocation.getWorld())) return false;

        return isWithin2DRadius(actionLocation, homeLocation, privateRadiusHome);
    }

    /**
     * Проверяем, находится ли локация в радиусе фермы.
     */
    public boolean isWithinFarmRadius(Location loc, String team) {
        Location farm = getFarmLocation(team, loc.getWorld().getName());
        if (farm == null) return false;
        double radius = plugin.getConfig().getDouble("plugin.raid.privateRadiusFarm");
        return farm.distance(loc) <= radius;
    }

    /**
     * Проверка: находится ли loc1 в 2D-радиусе от loc2.
     */
    public boolean isWithin2DRadius(Location loc1, Location loc2, double distance) {
        final Location loc1_2D = new Location(loc1.getWorld(), loc1.getX(), 0, loc1.getZ());
        final Location loc2_2D = new Location(loc2.getWorld(), loc2.getX(), 0, loc2.getZ());

        return loc1_2D.distanceSquared(loc2_2D) <= (distance * distance);
    }

    /**
     * Проверка: находится ли loc1 в радиусе distance от loc2 (3D).
     */
    public boolean isWithinRadius(Location loc1, Location loc2, double distance) {
        if (loc1 == null || loc2 == null) return false;
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;

        return loc1.distanceSquared(loc2) <= (distance * distance);
    }

    /**
     * Проверка, всегда ли в этом мире разрешен PvP.
     */
    public boolean isThePvPIsAlwaysOnInThisWorld(String worldName) {
        return alwaysAllowedPvPWorlds.contains(worldName);
    }

    /**
     * Проверка идентичности локаций (мир, x, y, z).
     */
    public boolean areLocationsEqualByXYZAndWorld(Location loc1, Location loc2) {
        return loc1.getWorld().getName().equals(loc2.getWorld().getName())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Загрузка / сохранение состава команд и их капитанов.
     */
    private void loadTeamCaptains() {
        final ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection == null) return;

        for (String team : teamsSection.getKeys(false)) {
            final String captainName = teamsSection.getString(team + ".captain");
            if (captainName != null) {
                teamCaptains.put(team, captainName);
            }
        }
    }

    private void loadTeamMembers() {
        final ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection == null) return;

        for (String team : teamsSection.getKeys(false)) {
            List<String> memberList = teamsSection.getStringList(team + ".members");
            if (!memberList.isEmpty()) {
                teamMembers.put(team, new ArrayList<>(memberList));

                final MyTeam currentTeam = teams.get(team);
                if (currentTeam != null) {
                    for (String member : memberList) {
                        currentTeam.addMember(member);
                    }
                }
            }
        }
    }

    private void loadTeamScores() {
        final ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection == null) return;

        for (String team : teamsSection.getKeys(false)) {
            final int score = teamsSection.getInt(team + ".score");
            teamScores.put(team, score);
        }
    }

    private void saveTeamScores() {
        ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection == null) {
            teamsSection = plugin.getConfig().createSection("teams");
        }

        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            final String team = entry.getKey();
            final int score = entry.getValue();
            teamsSection.set(team + ".score", score);
        }

        plugin.saveConfig();
    }

    /**
     * Загрузка начальных баз команд (Nexus).
     */
    public void loadBases() {
        teamBases.clear();
        teamBases.put("RED", getNexusLocation("RED"));
        teamBases.put("BLUE", getNexusLocation("BLUE"));
    }

    /**
     * Загрузка миров, где PvP всегда разрешён.
     */
    public void loadAlwaysAllowedPvPWorld() {
        final ConfigurationSection pluginSection = plugin.getConfig().getConfigurationSection("plugin");
        if (pluginSection != null) {
            alwaysAllowedPvPWorlds = pluginSection.getStringList("pvpIsAlwaysAllowedInTheseWorlds");
        }
    }

    /**
     * Находим игрока по имени в составе всех команд.
     */
    public Player getPlayerByName(String playerName) {
        for (ArrayList<String> members : teamMembers.values()) {
            for (String member : members) {
                if (member.equalsIgnoreCase(playerName)) {
                    return Bukkit.getPlayerExact(member);
                }
            }
        }
        return null;
    }

    /**
     * Основная инициализация команд, капитанов, членов, баз и т.д.
     */
    private void initializeTeams() {
        loadTeamCaptains();
        loadBases();

        // Создаём/регистрируем команды RED, BLUE
        final MyTeam redTeam = new MyTeam("RED", getPlayerByName(getCaptainName("RED")));
        teams.put("RED", redTeam);

        final MyTeam blueTeam = new MyTeam("BLUE", getPlayerByName(getCaptainName("BLUE")));
        teams.put("BLUE", blueTeam);

        loadTeamMembers();
        loadTeamScores();
        loadAlwaysAllowedPvPWorld();
    }
}
