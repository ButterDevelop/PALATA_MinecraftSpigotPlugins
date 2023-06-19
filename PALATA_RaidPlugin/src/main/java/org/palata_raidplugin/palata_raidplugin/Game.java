package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;

public class Game {

    private HashMap<String, Location> teamBases = new HashMap<>(); // Местоположения баз команд
    private HashMap<String, Location> teamHomes = new HashMap<>(); // Местоположения домой команд
    private HashMap<String, ArrayList<String>> teamMembers = new HashMap<>(); // Члены команд
    private HashMap<String, Integer> teamScores = new HashMap<>(); // Счет команд
    private Scoreboard scoreboard; // Scoreboard для отображения результатов
    public String raidingTeam = null; // Команда, которая в настоящее время проводит рейд
    public String lastRaidOpenedTheTeam = null; // Последняя команда, которая начала рейд
    private Plugin plugin;
    private Map<String, MyTeam> teams = new HashMap<>();

    // Булевые значения, отслеживающие состояние игры
    private boolean isRaidOpen = false;
    private boolean isRaidActive = false;
    private boolean isEnabled = true;
    private boolean isRaidStarted = false;
    private HashMap<String, String> teamCaptains = new HashMap<>(); // Капитаны команд
    private int obsidianDestroyed = 0;
    private double privateRadiusRaid = 10;
    private double privateRadiusHome = 10;

    // Список игроков, присоединившихся к рейду
    public final List<Player> raidPlayers = new ArrayList<>();
    private long raidStartTime;

    public Game(Plugin _plugin) {
        plugin = _plugin;
        initializeTeams();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        isEnabled = plugin.getConfig().getBoolean("plugin.raid.isEnabled");
        privateRadiusRaid = plugin.getConfig().getDouble("plugin.raid.privateRadiusRaid");
        privateRadiusHome = plugin.getConfig().getDouble("plugin.raid.privateRadiusHome");
        scoreboard = manager.getNewScoreboard();
    }

    public boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean value) {
        isEnabled = value;
        plugin.getConfig().set("plugin.raid.isEnabled", isEnabled);
        plugin.saveConfig();
    }

    // Проверка, есть ли у команды база
    public boolean hasBase(String team) {
        return teamBases.containsKey(team);
    }

    // Получение базы команды
    public Location getBase(String team) {
        return teamBases.get(team);
    }

    // Установка базы команды
    public void setBase(String team, Location loc) {
        teamBases.put(team, loc);
    }

    public void startRaid(String attackingTeam) {
        // Получите время задержки из файла конфигурации
        int delayMinutes = getRaidDelayMinutes();

        isRaidStarted = true;

        // Запустите асинхронную задачу через указанное количество минут
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            // Проверьте, есть ли достаточное количество игроков другой команды для начала рейда
            if (canRaid(attackingTeam)) {
                // Начать рейд
                obsidianDestroyed = 0;
                isRaidActive = true;
                raidStartTime = System.currentTimeMillis();

                buildNotFullNexus(getDefendingTeam(attackingTeam));

                startRaidDelayed(attackingTeam);
            } else {
                // Рейд не может быть начат
                isRaidActive = false;
                isRaidOpen = false;
                isRaidStarted = false;
                for (Player player : raidPlayers) {
                    player.sendMessage(ChatColor.RED + "Рейд не может быть начат из-за недостаточного количества игроков другой команды.");
                }
            }
        }, (long) delayMinutes * 60 * 20); // Конвертируйте минуты в тики (20 тиков = 1 секунда)
    }

    // Начало рейда
    public void startRaidDelayed(String attackingTeam) {
        Bukkit.broadcastMessage(ChatColor.GREEN + "Рейд начался! Команда '" + attackingTeam + "' является нападающей.");

        raidingTeam = attackingTeam;
        String defendingTeam = getDefendingTeam(attackingTeam); // Получите команду-защитника
        Location nexusLocation = getNexusLocation(defendingTeam); // Получите местоположение нексуса защитников
        Block nexusBlock = nexusLocation.getBlock();

        // Запускаем задачу на слежение за уничтожением нексуса
        new BukkitRunnable() {
            @Override
            public void run() {
                int requiredDestroyCount = getRequiredDestroyCount();
                // Загрузите продолжительность рейда из файла конфигурации
                int raidDurationMinutes = getRaidDurationMinutes();
                long raidEndTime = raidStartTime + ((long) raidDurationMinutes * 60 * 1000); // конвертируем минуты в миллисекунды
                // Если нексус уничтожен, увеличиваем счетчик уничтожений и восстанавливаем нексус
                if (nexusBlock.getType() == Material.AIR) {
                    nexusBlock.setType(Material.OBSIDIAN);
                    String message = "Часть нексуса команды '" + defendingTeam + "' уничтожен! Осталось уничтожить " + (requiredDestroyCount - obsidianDestroyed) + " раз!";
                    for (Player player : getTeamPlayers(attackingTeam)) {
                        player.sendMessage(ChatColor.GREEN + message);
                    }
                    for (Player player : getTeamPlayers(defendingTeam)) {
                        player.sendMessage(ChatColor.RED + message);
                    }
                }
                // Если нексус уничтожен требуемое количество раз или время рейда вышло, заканчиваем рейд
                if (obsidianDestroyed >= requiredDestroyCount || System.currentTimeMillis() >= raidEndTime) {
                    // Если время рейда истекло, но нексус не был уничтожен необходимое количество раз
                    if (obsidianDestroyed < requiredDestroyCount) {
                        endRaid();
                        Bukkit.broadcastMessage(ChatColor.RED + "Время рейда истекло! Команде '" + attackingTeam + "' не удалось завершить рейд. Нексус команды '" + defendingTeam + "' был уничтожен только " + obsidianDestroyed + " раз.");
                        addScore(defendingTeam, 1);
                    } else {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "Команда '" + attackingTeam + "' успешно завершила рейд! Нексус команды '" + defendingTeam + "' уничтожен " + obsidianDestroyed + " раз.");
                        addScore(attackingTeam, 1);
                    }

                    this.cancel(); // Отменяем задачу
                    raidingTeam = null; // Заканчиваем рейд

                    isRaidActive = false;
                    buildFullNexus(defendingTeam);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Запускаем каждую секунду (20 тиков = 1 секунда)
    }

    public void buildFullNexus(String team) {
        World world = getNexusLocation(team).getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)
        Location nexusLocation = getNexusLocation(team);
        // Размещаем структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(nexusLocation.getBlockX() + x, nexusLocation.getBlockY() + y, nexusLocation.getBlockZ() + z);
                    if ((x == 0 && y == 0 && Math.abs(z) == baseRadius) ||
                            (y == 0 && Math.abs(x) == baseRadius && z == 0) ||
                            (z == 0 && x == 0 && Math.abs(y) == baseRadius)) {
                        // Устанавливаем барьер в остальных блоках нексуса
                        block.setType(Material.BARRIER);
                    } else
                    if (x == 0 && y == 0 && z == 0) {
                        // Устанавливаем обсидиан в середине
                        block.setType(Material.OBSIDIAN);
                    } else
                    if (!(Math.abs(x) == baseRadius && Math.abs(y) == baseRadius && Math.abs(z) == baseRadius)) {
                        // Устанавливаем бедрок не на краях нексуса
                        block.setType(Material.BEDROCK);
                    }
                }
            }
        }
    }

    public void buildNotFullNexus(String team) {
        World world = getNexusLocation(team).getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)
        Location nexusLocation = getNexusLocation(team);
        // Размещаем структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(nexusLocation.getBlockX() + x, nexusLocation.getBlockY() + y, nexusLocation.getBlockZ() + z);
                    if ((x == 0 && y == 0 && Math.abs(z) == baseRadius) ||
                            (y == 0 && Math.abs(x) == baseRadius && z == 0) ||
                            (z == 0 && x == 0 && Math.abs(y) == baseRadius)) {
                        // Устанавливаем барьер в остальных блоках нексуса
                        block.setType(Material.AIR);
                    } else
                    if (x == 0 && y == 0 && z == 0) {
                        // Устанавливаем обсидиан в середине
                        block.setType(Material.OBSIDIAN);
                    }
                }
            }
        }
    }

    public void removeFullNexus(Location nexusLocation) {
        World world = nexusLocation.getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)
        // Размещаем структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(nexusLocation.getBlockX() + x, nexusLocation.getBlockY() + y, nexusLocation.getBlockZ() + z);
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public void buildFullHome(String team) {
        World world = getHomeLocation(team).getWorld();
        int baseRadius = 1; // Радиус базы для дома
        Location homeLocation = getHomeLocation(team);
        // Размещаем структуру центра дома
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(homeLocation.getBlockX() + x, homeLocation.getBlockY() + y, homeLocation.getBlockZ() + z);
                    block.setType(Material.BEDROCK);
                }
            }
        }
    }

    public void removeFullHome(Location homeLocation) {
        World world = homeLocation.getWorld();
        int baseRadius = 1; // Радиус базы для нексуса (без обсидиана)
        // Размещаем структуру нексуса
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int y = -baseRadius; y <= baseRadius; y++) {
                for (int z = -baseRadius; z <= baseRadius; z++) {
                    Block block = world.getBlockAt(homeLocation.getBlockX() + x, homeLocation.getBlockY() + y, homeLocation.getBlockZ() + z);
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public String getDefendingTeam(String attackingTeam) {
        // Возвращает название команды-защитника в зависимости от команды-атакующего.
        // Допустим, у вас есть две команды: "RED" и "BLUE". Если "RED" атакует, то "BLUE" защищает.
        if (attackingTeam.equals("RED")) {
            return "BLUE";
        } else if (attackingTeam.equals("BLUE")) {
            return "RED";
        } else {
            return null;
        }
    }

    public Location getNexusLocation(String team) {
        String worldName = plugin.getConfig().getString(team + ".nexus.world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        // Получаем местоположение нексуса из файла конфигурации
        double x = plugin.getConfig().getDouble(team + ".nexus.x");
        double y = plugin.getConfig().getDouble(team + ".nexus.y");
        double z = plugin.getConfig().getDouble(team + ".nexus.z");

        // Возвращает местоположение нексуса для данной команды
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public boolean isBlockInNexus(Location blockLocation, String team) {
        Location nexusLocation = getNexusLocation(team);
        return nexusLocation != null && nexusLocation.equals(blockLocation);
    }

    public Location getHomeLocation(String team) {
        String worldName = plugin.getConfig().getString(team + ".home.world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        // Получаем местоположение нексуса из файла конфигурации
        double x = plugin.getConfig().getDouble(team + ".home.x");
        double y = plugin.getConfig().getDouble(team + ".home.y");
        double z = plugin.getConfig().getDouble(team + ".home.z");

        // Возвращает местоположение нексуса для данной команды
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public boolean isBlockInHome(Location blockLocation, String team) {
        Location homeLocation = getHomeLocation(team);
        return homeLocation != null && homeLocation.equals(blockLocation);
    }

    // Проверка, можно ли начать рейд
    public boolean canRaid(String teamName) {
        // проверяем, активен ли уже рейд
        if (raidingTeam != null || isRaidActive()) {
            return false;
        }

        MyTeam team = teams.get(teamName);
        if (team == null) {
            throw new IllegalArgumentException("Team " + teamName + " does not exist.");
        }

        int minPlayers = plugin.getConfig().getInt("plugin.raid.minPlayers");
        int differencePlayersCount = plugin.getConfig().getInt("plugin.raid.differencePlayersCount");

        // проверяем, есть ли другие команды и достаточно ли игроков
        if (getTeamPlayers(teamName).size() >= minPlayers && getTeamPlayers(getDefendingTeam(teamName)).size() >= minPlayers &&
                getTeamPlayers(teamName).size() - getTeamPlayers(getDefendingTeam(teamName)).size() <= differencePlayersCount) {
            return true;
        }

        /*for (MyTeam otherTeam : teams.values()) {
            if (!otherTeam.equals(team) && otherTeam.getMembers().size() >= minPlayers) {
                return true;
            }
        }*/

        return false;
    }

    // Обновление счета команды на Scoreboard
    public void updateScoreboard() {
        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            objective = scoreboard.registerNewObjective("teamscore", "dummy", "Очки команд");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            String team = entry.getKey();
            int score = entry.getValue();

            // Задаем цвет команде в зависимости от ее имени
            if (team.equalsIgnoreCase("red")) {
                team = ChatColor.RED + team;
            } else if (team.equalsIgnoreCase("blue")) {
                team = ChatColor.BLUE + team;
            }

            Score teamScore = objective.getScore(team);
            teamScore.setScore(score);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    // Добавление очков к счету команды
    public void addScore(String team, int points) {
        if (!teamScores.containsKey(team)) {
            teamScores.put(team, 0);
        }
        teamScores.put(team, teamScores.get(team) + points);
        saveTeamScores();
        updateScoreboard();
    }

    // Получение очков команды
    public int getScore(String team) {
        return teamScores.getOrDefault(team, 0);
    }

    // Получение объекта Scoreboard
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    // Проверяет, открыт ли рейд
    public boolean isRaidOpen() {
        return isRaidOpen;
    }

    // Проверяет, активен ли рейд
    public boolean isRaidActive() {
        return isRaidActive;
    }

    // Проверяет, пошёл ли отсчёт до начала рейда
    public boolean isRaidStarted() {
        return isRaidStarted;
    }

    // Увеличивает количество уничтоженного обсидиана на 1
    public void incrementObsidianDestroyed() {
        obsidianDestroyed++;
    }

    // Возвращает количество обсидиана, которое ещё необходимо уничтожить
    public int getRequiredDestroyCount() {
        // Здесь нужно прочитать данные из файла конфигурации, например:
        return plugin.getConfig().getInt("plugin.raid.requiredDestroyCount");
    }

    // Возвращает количество уничтоженного на данный момент обсидиана
    public int getObsidianDestroyed() {
        return obsidianDestroyed;
    }

    // Завершает рейд
    public void endRaid() {
        isRaidOpen = false;
        isRaidActive = false;
        isRaidStarted = false;
        buildFullNexus(getPlayerTeam(raidPlayers.get(0).getName()));
        long currentTimestamp = System.currentTimeMillis();
        plugin.getConfig().set(getPlayerTeam(raidPlayers.get(0).getName()) + ".lastRaid", currentTimestamp);
        plugin.saveConfig();
        for (Player player : raidPlayers) {
            player.sendMessage(ChatColor.YELLOW + "Рейд был закончен.");
        }
        raidPlayers.clear();
    }

    // Добавляет игрока к рейду
    public void addPlayerToRaid(Player player) {
        if (!raidPlayers.contains(player)) {
            raidPlayers.add(player);
        }
    }

    public String getLastRaidOpenedTheTeam() {
        return lastRaidOpenedTheTeam;
    }

    // Открывает рейд
    public void openRaid(String team) {
        isRaidOpen = true;
        lastRaidOpenedTheTeam = team;
        obsidianDestroyed = 0;
        raidPlayers.clear();

        for (Player player : getTeamPlayers(team)) {
            player.sendMessage(ChatColor.GREEN + "Рейд был открыт! Присоединяйтесь к нему с помощью /joinraid");
            int openDurationMinutes = getOpenRaidDurationMinutes();
            player.sendMessage(ChatColor.YELLOW + "Осталось времени для присоединения: " + openDurationMinutes + " минут.");
            player.sendMessage(ChatColor.YELLOW + "Если капитан команды за это время не напишет /startraid, то рейд будет автоматически отменён.");
        }
    }

    // Отменяет рейд
    public void cancelRaid(String team, boolean wasCancelledByPlayer) {
        if (!isRaidStarted && !isRaidActive && isRaidOpen) {
            isRaidOpen = false;
            for (Player player : getTeamPlayers(team)) {
                if (wasCancelledByPlayer) {
                    player.sendMessage(ChatColor.RED + "Рейд команды был отменён её капитаном!");
                } else {
                    player.sendMessage(ChatColor.RED + "Рейд команды был отменён! Истекло время!");
                }
            }
        }
    }

    public int getOpenRaidDurationMinutes() {
        return plugin.getConfig().getInt("plugin.raid.openDurationMinutes");
    }

    public List<Player> getTeamPlayers(String team) {
        List<String> playerNames = teamMembers.getOrDefault(team, new ArrayList<>());
        List<Player> players = new ArrayList<>();
        for (String playerName : playerNames) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public String getPlayerTeam(String playerName) {
        for (Map.Entry<String, ArrayList<String>> entry : teamMembers.entrySet()) {
            if (entry.getValue().contains(playerName)) {
                return entry.getKey();
            }
        }
        return null; // Если игрок не найден ни в одной команде, вернуть null
    }

    public Location getSafeLocation(Location playerLocation) {
        int searchRadius = 10; // Радиус поиска свободного места
        World world = playerLocation.getWorld();

        if (isLocationSafe(playerLocation)) return playerLocation;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location location = playerLocation.clone().add(x, y, z);
                    if (isLocationSafe(location)) {
                        return location;
                    }
                }
            }
        }

        return null; // Если свободное место не найдено, вернуть null
    }

    private boolean isLocationSafe(Location location) {
        // Проверяем, является ли данное место безопасным для перемещения
        Block block = location.getBlock();

        // Проверяем, что блок и его соседи - воздух
        if (block.getType() != Material.AIR || !block.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
            return false;
        }

        // Проверяем, что блок под ногами - не воздух и не лава
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.AIR) || block.getRelative(BlockFace.DOWN).getType().equals(Material.LAVA)) {
            return false;
        }

        // Дополнительные проверки, если необходимо

        return true; // Место считается безопасным
    }

    public void teleportPlayerToSafePosition(Player player) {
        Location playerLocation = player.getLocation();
        Location safeLocation = getSafeLocation(playerLocation);
        if (safeLocation != null) {
            player.teleport(safeLocation);
            player.sendMessage(ChatColor.YELLOW + "Вас переместили в безопасное место для постройки Нексуса.");
        }
    }

    // Устанавливает игрока как капитана для указанной команды
    public void setCaptain(String team, Player player) {
        teamCaptains.put(team, player.getName());
    }

    // Проверяет, является ли данный игрок капитаном указанной команды
    public boolean isCaptain(String team, Player player) {
        String captainName = teamCaptains.get(team);
        return captainName != null && captainName.equals(player.getName());
    }

    // Получает имя капитана для указанной команды
    public String getCaptainName(String team) {
        return teamCaptains.get(team);
    }

    public boolean isWithinNexusRadius(Location actionLocation, String team) {
        Location nexusLocation = getNexusLocation(team);
        if (nexusLocation == null || actionLocation == null) {
            return false;
        }

        Location actionLocation2D = new Location(actionLocation.getWorld(), actionLocation.getX(), 0, actionLocation.getZ());
        Location nexusLocation2D = new Location(nexusLocation.getWorld(), nexusLocation.getX(), 0, nexusLocation.getZ());

        return Math.abs(actionLocation2D.distanceSquared(nexusLocation2D)) <= privateRadiusRaid * privateRadiusRaid;
    }

    public boolean isWithinHomeRadius(Location actionLocation, String team) {
        Location homeLocation = getHomeLocation(team);
        if (homeLocation == null || actionLocation == null) {
            return false;
        }

        Location actionLocation2D = new Location(actionLocation.getWorld(), actionLocation.getX(), 0, actionLocation.getZ());
        Location homeLocation2D = new Location(homeLocation.getWorld(), homeLocation.getX(), 0, homeLocation.getZ());

        return Math.abs(actionLocation2D.distanceSquared(homeLocation2D)) <= privateRadiusHome * privateRadiusHome;
    }

    // Загружает состав команд и имена капитанов из файла конфигурации
    private void loadTeamCaptains() {
        // Загрузка составов команд и имен капитанов из файла конфигурации
        ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String team : teamsSection.getKeys(false)) {
                String captainName = teamsSection.getString(team + ".captain");
                if (captainName != null) {
                    teamCaptains.put(team, captainName);
                }
            }
        }
    }

    private void loadTeamMembers() {
        ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String team : teamsSection.getKeys(false)) {
                List<String> memberList = teamsSection.getStringList(team + ".members");
                if (memberList != null && !memberList.isEmpty()) {
                    teamMembers.put(team, new ArrayList<>(memberList));
                    MyTeam currentTeam = teams.get(team);
                    if (currentTeam != null) {
                        for (String member : memberList) {
                            currentTeam.addMember(member);
                        }
                    }
                }
            }
        }
    }

    private void loadTeamScores() {
        ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String team : teamsSection.getKeys(false)) {
                int score = teamsSection.getInt(team + ".score");
                teamScores.put(team, score);
            }
        }
    }

    private void saveTeamScores() {
        ConfigurationSection teamsSection = plugin.getConfig().getConfigurationSection("teams");
        if (teamsSection == null) {
            teamsSection = plugin.getConfig().createSection("teams");
        }

        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            String team = entry.getKey();
            int score = entry.getValue();
            teamsSection.set(team + ".score", score);
        }

        plugin.saveConfig();
    }

    public void loadBases()
    {
        teamBases.clear();
        teamBases.put("RED", getNexusLocation("RED")); // Замените на реальные координаты базы команды "RED"
        teamBases.put("BLUE", getNexusLocation("BLUE")); // Замените на реальные координаты базы команды "BLUE"
    }

    public void loadHomes()
    {
        teamHomes.clear();
        teamHomes.put("RED", getHomeLocation("RED")); // Замените на реальные координаты базы команды "RED"
        teamHomes.put("BLUE", getHomeLocation("BLUE")); // Замените на реальные координаты базы команды "BLUE"
    }

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

    // Вызывается при инициализации плагина, чтобы загрузить состав команд, имена капитанов и т.д.
    private void initializeTeams() {
        loadTeamCaptains();
        // Другие действия по инициализации команд

        // Инициализация баз команд
        loadBases();
        // Инициализация домов команд
        loadHomes();
        // Создание команды "RED" и добавление ее в teams
        MyTeam redTeam = new MyTeam("RED", getPlayerByName(getCaptainName("RED")));
        teams.put("RED", redTeam);
        // Создание команды "BLUE" и добавление ее в teams
        MyTeam blueTeam = new MyTeam("BLUE", getPlayerByName(getCaptainName("RED")));
        teams.put("BLUE", blueTeam);

        // Инициализация членов команд и счета команд
        loadTeamMembers();

        loadTeamScores();
    }

    public int getRaidDelayMinutes() {
        return plugin.getConfig().getInt("plugin.raid.delay");
    }

    public int getRaidDurationMinutes() {
        return plugin.getConfig().getInt("plugin.raid.durationMinutes");
    }

}
