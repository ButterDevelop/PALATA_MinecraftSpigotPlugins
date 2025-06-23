package org.butterdevelop.fairWorldGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.structure.Structure;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class FairWorldGenerator extends JavaPlugin implements CommandExecutor {
    private Location teamBSpawn;
    private BukkitTask searchTask;

    // Параметры поиска и генерации
    private double errorThreshold = Double.MAX_VALUE;
    private int maxAttempts = 3;
    private int attemptCount;

    private World currentWorld;
    private World netherWorld;
    private long currentSeed;
    private List<double[]> flatStructs;

    // Глобальный лучший результат
    private double globalBestError = Double.MAX_VALUE;
    private double[][] globalBestCoords = new double[2][2];
    private long globalBestSeed;

    // Настройки
    private static final int MAX_SEARCH_RADIUS = 3000;
    private static final int MIN_TEAM_DISTANCE = 200;
    private static final int SPAWN_CIRCLE_RADIUS = 1000;
    private static final int ANGLE_STEP = 10;
    private static final int BATCH_SIZE = 10;
    private static final int TICK_INTERVAL = 2;

    @Override
    public void onEnable() {
        Objects.requireNonNull(this.getCommand("fairworld")).setExecutor(this);
        getLogger().info("FairWorldGenerator enabled. Use /fairworld generate [maxError] [maxAttempts] or /fairworld stop");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("fairworld")) return false;
        if (args.length == 0) {
            sender.sendMessage("Use: /fairworld generate [maxError] [maxAttempts] | /fairworld stop");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("stop")) {
            if (searchTask != null) {
                searchTask.cancel();
                searchTask = null;
                Bukkit.broadcastMessage("§c[FairWorld] Process stopped by user.");
            } else {
                sender.sendMessage("§c[FairWorld] No process running.");
            }
            return true;
        }
        if (sub.equalsIgnoreCase("generate")) {
            if (searchTask != null) {
                sender.sendMessage("§c[FairWorld] Process already running. Use /fairworld stop.");
                return true;
            }
            // Парсинг параметров
            if (args.length >= 2) {
                try { errorThreshold = Double.parseDouble(args[1]); }
                catch (NumberFormatException e) { errorThreshold = Double.MAX_VALUE; }
            } else errorThreshold = Double.MAX_VALUE;
            if (args.length >= 3) {
                try { maxAttempts = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) { maxAttempts = 3; }
            } else maxAttempts = 3;
            attemptCount = 0;
            globalBestError = Double.MAX_VALUE;
            Bukkit.broadcastMessage(String.format("§e[FairWorld] Starting: threshold=%.2f, maxAttempts=%d", errorThreshold, maxAttempts));
            startGeneration();
            return true;
        }
        sender.sendMessage("Use: /fairworld generate [maxError] [maxAttempts] | /fairworld stop");
        return true;
    }

    private void startGeneration() {
        attemptCount++;
        currentSeed = new Random().nextLong();
        String worldName = "fair_match_" + currentSeed;
        Bukkit.broadcastMessage(String.format("§a[FairWorld] Attempt %d/%d: creating world seed=%d", attemptCount, maxAttempts, currentSeed));
        // Overworld
        currentWorld = Bukkit.createWorld(
                new WorldCreator(worldName)
                        .environment(World.Environment.NORMAL)
                        .type(WorldType.NORMAL)
                        .seed(currentSeed)
        );
        Bukkit.broadcastMessage("§e[FairWorld] World '" + worldName + "' created. Loading chunks...");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.broadcastMessage("§a[FairWorld] Locating structures in Overworld...");
            Map<Structure, Location> structs = locateStructures(currentWorld,
                    List.of(Structure.ANCIENT_CITY, Structure.STRONGHOLD, Structure.TRIAL_CHAMBERS, Structure.VILLAGE_PLAINS)
            );
            if (structs == null) return;
            flatStructs = new ArrayList<>();
            for (Location loc : structs.values()) {
                flatStructs.add(new double[]{loc.getX(), loc.getZ()});
            }
            Bukkit.broadcastMessage("§a[FairWorld] Starting async spawn search in Overworld...");
            startSpawnSearch();
        }, 20L * 5);
    }

    private Map<Structure, Location> locateStructures(World world, List<Structure> types) {
        Location center = new Location(world, 0, world.getHighestBlockYAt(0,0), 0);
        Map<Structure, Location> map = new HashMap<>();
        for (Structure st : types) {
            var start = world.locateNearestStructure(center, st, MAX_SEARCH_RADIUS, false);
            if (start == null) {
                Bukkit.broadcastMessage("§c[FairWorld] Structure not found: " + st.getKey());
                return null;
            }
            Location loc = start.getLocation();
            map.put(st, loc);
            Bukkit.broadcastMessage("§e[FairWorld] Found " + st.getKey() + " at " + locString(loc));
        }
        return map;
    }

    private void startSpawnSearch() {
        final double[] bestError = {Double.MAX_VALUE};
        final double[][] bestCoords = {{0,0},{0,0}};
        final int total = 360 / ANGLE_STEP;
        final int[] idx = {0};

        searchTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (int i = 0; i < BATCH_SIZE && idx[0] < total; i++, idx[0]++) {
                double rad = Math.toRadians(idx[0] * ANGLE_STEP);
                double x1 = Math.cos(rad) * SPAWN_CIRCLE_RADIUS;
                double z1 = Math.sin(rad) * SPAWN_CIRCLE_RADIUS;
                double x2 = -x1, z2 = -z1;
                if (Math.hypot(x1-x2, z1-z2) < MIN_TEAM_DISTANCE) continue;
                double err = 0;
                for (double[] s : flatStructs) {
                    err += Math.abs(Math.hypot(x1-s[0],z1-s[1]) - Math.hypot(x2-s[0],z2-s[1]));
                }
                if (err < bestError[0]) {
                    bestError[0] = err;
                    bestCoords[0] = new double[]{x1,z1};
                    bestCoords[1] = new double[]{x2,z2};
                }
            }
            if (idx[0] % ((5 * 20) / TICK_INTERVAL) == 0) {
                double pct = idx[0] * 100.0 / total;
                Bukkit.broadcastMessage(String.format("§e[FairWorld] Overworld progress: %.1f%% (Err: %.2f)", pct, bestError[0]));
            }
            if (idx[0] >= total) {
                searchTask.cancel();
                if (bestError[0] < globalBestError) {
                    globalBestError = bestError[0];
                    globalBestCoords = new double[][]{bestCoords[0].clone(), bestCoords[1].clone()};
                    globalBestSeed = currentSeed;
                }
                if (bestError[0] <= errorThreshold || attemptCount >= maxAttempts) {
                    double[][] useCoords = bestError[0] <= errorThreshold ? bestCoords : globalBestCoords;
                    long useSeed = bestError[0] <= errorThreshold ? currentSeed : globalBestSeed;
                    finishSearch(useCoords, bestError[0] <= errorThreshold ? bestError[0] : globalBestError, useSeed);
                } else {
                    Bukkit.broadcastMessage(String.format("§6[FairWorld] Err %.2f > %.2f, restarting generation.", bestError[0], errorThreshold));
                    startGeneration();
                }
            }
        }, 0L, TICK_INTERVAL);
    }

    private void finishSearch(double[][] coords, double overworldErr, long seed) {
        // Overworld spawns
        Location a = new Location(currentWorld,
                coords[0][0], currentWorld.getHighestBlockYAt((int)coords[0][0], (int)coords[0][1]), coords[0][1]);
        Location b = new Location(currentWorld,
                coords[1][0], currentWorld.getHighestBlockYAt((int)coords[1][0], (int)coords[1][1]), coords[1][1]);
        currentWorld.setSpawnLocation(a);
        teamBSpawn = b;
        // Calculate nether error in advance, integrate into total error
        double netherErr = 0;
        World nether = Bukkit.getWorld("world_nether");
        if (nether != null) {
            try {
                double axN = a.getX() / 8.0, azN = a.getZ() / 8.0;
                double bxN = b.getX() / 8.0, bzN = b.getZ() / 8.0;
                Location na = new Location(nether, axN,
                        nether.getHighestBlockYAt((int)axN, (int)azN), azN);
                Location nb = new Location(nether, bxN,
                        nether.getHighestBlockYAt((int)bxN, (int)bzN), bzN);
                Map<Structure, Location> netherStructs = locateStructures(nether,
                        List.of(Structure.FORTRESS, Structure.BASTION_REMNANT));
                if (netherStructs != null) {
                    for (Map.Entry<Structure, Location> entry : netherStructs.entrySet()) {
                        Location loc = entry.getValue();
                        netherErr += Math.abs(na.distance(loc) - nb.distance(loc));
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Nether calculation failed: " + e.getMessage());
            }
        }
        double totalErr = overworldErr + netherErr;
        Bukkit.broadcastMessage(String.format("§a[FairWorld] Finished: Overworld Err=%.2f, Nether Err=%.2f, Total Err=%.2f, seed=%d",
                overworldErr, netherErr, totalErr, seed));
        Bukkit.broadcastMessage("§a[FairWorld] Spawn A: " + locString(a));
        Bukkit.broadcastMessage("§a[FairWorld] Spawn B: " + locString(b));
    }

    private String locString(Location l) {
        return String.format("(%.0f,%.0f,%.0f)", l.getX(), l.getY(), l.getZ());
    }

    public Location getTeamBSpawn() {
        return teamBSpawn;
    }
}

// plugin.yml remains unchanged
